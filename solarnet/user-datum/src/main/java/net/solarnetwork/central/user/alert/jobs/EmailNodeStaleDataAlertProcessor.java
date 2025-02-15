/* ==================================================================
 * EmailNodeStaleDataAlertProcessor.java - 15/05/2015 7:23:12 pm
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.user.alert.jobs;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.domain.DateInterval;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamPK.NodeDatumStreamPK;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.ClasspathResourceMessageTemplateDataSource;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.DateUtils;

/**
 * Process stale data alerts for nodes.
 *
 * @author matt
 * @version 2.2
 */
public class EmailNodeStaleDataAlertProcessor implements UserAlertBatchProcessor {

	/** The default value for {@link #getBatchSize()}. */
	public static final Integer DEFAULT_BATCH_SIZE = 50;

	/** The default value for {@link #getMailTemplateResource()}. */
	public static final String DEFAULT_MAIL_TEMPLATE_RESOURCE = "net/solarnetwork/central/user/alert/jobs/user-alert-NodeStaleData.txt";

	/** The default value for {@link #getMailTemplateResolvedResource()}. */
	public static final String DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE = "net/solarnetwork/central/user/alert/jobs/user-alert-NodeStaleData-Resolved.txt";

	/**
	 * A {@code UserAlertSituation} {@code info} key for an associated node ID.
	 *
	 * @since 1.1
	 */
	public static final String SITUATION_INFO_NODE_ID = "nodeId";

	/**
	 * A {@code UserAlertSituation} {@code info} key for an associated source
	 * ID.
	 *
	 * @since 1.1
	 */
	public static final String SITUATION_INFO_SOURCE_ID = "sourceId";

	/**
	 * A {@code UserAlertSituation} {@code info} key for an associated datum
	 * creation date.
	 *
	 * @since 1.1
	 */
	public static final String SITUATION_INFO_DATUM_CREATED = "datumCreated";

	private final SolarNodeDao solarNodeDao;
	private final UserDao userDao;
	private final UserNodeDao userNodeDao;
	private final UserAlertDao userAlertDao;
	private final UserAlertSituationDao userAlertSituationDao;
	private final DatumEntityDao datumDao;
	private final MailService mailService;
	private Integer batchSize = DEFAULT_BATCH_SIZE;
	private final MessageSource messageSource;
	private String mailTemplateResource = DEFAULT_MAIL_TEMPLATE_RESOURCE;
	private String mailTemplateResolvedResource = DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE;
	private DateTimeFormatter timestampFormat = DateUtils.DISPLAY_DATE_LONG_TIME_SHORT;
	private int initialAlertReminderDelayMinutes = 60;
	private int alertReminderFrequencyMultiplier = 4;

	// maintain a cache of node data during the execution of the job (cleared after each invocation)
	private final Map<Long, SolarNode> nodeCache = new HashMap<>(64);
	private final Map<Long, List<NodeDatumStreamPK>> nodeDataCache = new HashMap<>(64);
	private final Map<Long, List<NodeDatumStreamPK>> userDataCache = new HashMap<>(16);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Construct with properties.
	 *
	 * @param solarNodeDao
	 *        The {@link SolarNodeDao} to use.
	 * @param userDao
	 *        The {@link UserDao} to use.
	 * @param userNodeDao
	 *        The {@link UserNodeDao} to use.
	 * @param userAlertDao
	 *        The {@link UserAlertDao} to use.
	 * @param userAlertSituationDao
	 *        The {@link UserAlertSituationDao} to use.
	 * @param datumDao
	 *        The {@link DatumEntityDao} to use.
	 * @param mailService
	 *        The {@link MailService} to use.
	 * @param messageSource
	 *        The {@link MessageSource} to use.
	 */
	public EmailNodeStaleDataAlertProcessor(SolarNodeDao solarNodeDao, UserDao userDao,
			UserNodeDao userNodeDao, UserAlertDao userAlertDao,
			UserAlertSituationDao userAlertSituationDao, DatumEntityDao datumDao,
			MailService mailService, MessageSource messageSource) {
		super();
		this.solarNodeDao = solarNodeDao;
		this.userDao = userDao;
		this.userNodeDao = userNodeDao;
		this.userAlertDao = userAlertDao;
		this.userAlertSituationDao = userAlertSituationDao;
		this.datumDao = datumDao;
		this.mailService = mailService;
		this.messageSource = messageSource;
	}

	/**
	 * Get the current system time. Exposed to support testing.
	 *
	 * @return The current system time.
	 * @since 1.2
	 */
	protected Instant getCurrentTime() {
		return Instant.now();
	}

	@Override
	public Long processAlerts(Long lastProcessedAlertId, Instant validDate) {
		if ( validDate == null ) {
			validDate = Instant.now();
		}
		List<UserAlert> alerts = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				lastProcessedAlertId, validDate, batchSize);
		Long lastAlertId = null;
		final Instant now = getCurrentTime();
		final DateTimeFormatter timeFormatter = DateUtils.LOCAL_TIME;
		try {
			loadMostRecentNodeData(alerts);
			for ( UserAlert alert : alerts ) {
				Map<String, Object> alertOptions = alert.getOptions();
				if ( alertOptions == null ) {
					continue;
				}

				final PathMatcher sourceIdMatcher = new AntPathMatcher();

				// extract options
				Number age;
				List<String> sourceIdPatterns;
				try {
					age = (Number) alertOptions.get(UserAlertOptions.AGE_THRESHOLD);
					sourceIdPatterns = alert.optionSourceIds();
				} catch ( ClassCastException e ) {
					log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
					continue;
				}

				if ( age == null ) {
					log.debug("Skipping alert {} that does not include {} option", alert,
							UserAlertOptions.AGE_THRESHOLD);
					continue;
				}

				// look for first stale data matching age + source criteria
				final List<DateInterval> timePeriods = new ArrayList<>(2);
				NodeDatumStreamPK stale = getFirstStaleDatum(alert, now, age, sourceIdMatcher,
						sourceIdPatterns, timeFormatter, timePeriods);

				Map<String, Object> staleInfo = new HashMap<>(4);
				if ( stale != null ) {
					staleInfo.put(SITUATION_INFO_DATUM_CREATED, stale.getTimestamp().toEpochMilli());
					staleInfo.put(SITUATION_INFO_NODE_ID, stale.getNodeId());
					staleInfo.put(SITUATION_INFO_SOURCE_ID, stale.getSourceId());
				}

				// get UserAlertSituation for this alert
				UserAlertSituation sit = userAlertSituationDao
						.getActiveAlertSituationForAlert(alert.getId());
				if ( stale != null ) {
					long notifyOffset = 0;
					if ( sit == null ) {
						sit = new UserAlertSituation();
						sit.setCreated(now);
						sit.setAlert(alert);
						sit.setStatus(UserAlertSituationStatus.Active);
						sit.setNotified(now);
						sit.setInfo(staleInfo);
					} else if ( sit.getNotified().equals(sit.getCreated()) ) {
						notifyOffset = (initialAlertReminderDelayMinutes * 60L * 1000L);
					} else {
						notifyOffset = ((sit.getNotified().toEpochMilli()
								- sit.getCreated().toEpochMilli()) * alertReminderFrequencyMultiplier);
					}

					// taper off the alerts so the become less frequent over time
					if ( !sit.getNotified().plusMillis(notifyOffset).isAfter(now) ) {
						sendAlertMail(alert, "user.alert.NodeStaleData.mail.subject",
								mailTemplateResource, stale);
						sit.setNotified(now);
					}
					if ( sit.getNotified().equals(now) || sit.getInfo() == null
							|| !staleInfo.equals(sit.getInfo()) ) {
						sit.setInfo(staleInfo);
						userAlertSituationDao.save(sit);
					}
				} else {
					// not stale, so mark valid for age span
					final boolean withinTimePeriods = withinIntervals(now, timePeriods);
					Instant newValidTo;
					if ( !timePeriods.isEmpty() && !withinTimePeriods ) {
						// we're not in valid to the start of the next time period
						newValidTo = startOfNextTimePeriod(now, timePeriods);
					} else {
						newValidTo = validDate.plusSeconds(age.intValue());
					}
					log.debug("Marking alert {} valid to {}", alert.getId(), newValidTo);
					userAlertDao.updateValidTo(alert.getId(), newValidTo);
					alert.setValidTo(newValidTo);
					if ( sit != null && withinTimePeriods ) {
						// make Resolved
						sit.setStatus(UserAlertSituationStatus.Resolved);
						sit.setNotified(now);
						userAlertSituationDao.save(sit);

						NodeDatumStreamPK nonStale = getFirstNonStaleDatum(alert, now, age,
								sourceIdMatcher, sourceIdPatterns);

						sendAlertMail(alert, "user.alert.NodeStaleData.Resolved.mail.subject",
								mailTemplateResolvedResource, nonStale);
					}
				}
				lastAlertId = alert.getId();
			}
		} catch ( RuntimeException e ) {
			throw new RepeatableTaskException("Error processing user alerts", e, lastAlertId);
		} finally {
			nodeCache.clear();
			nodeDataCache.clear();
			userDataCache.clear();
		}

		// short-circuit performing batch for no results if obvious
		if ( alerts.size() < batchSize && lastAlertId != null
				&& lastAlertId.equals(alerts.getLast().getId()) ) {
			// we've finished our batch
			lastAlertId = null;
		}

		return lastAlertId;
	}

	private List<DateInterval> parseAlertTimeWindows(final Instant nowDateTime,
			final DateTimeFormatter timeFormatter, final UserAlert alert, final Long nodeId) {
		Map<String, Object> alertOptions = alert.getOptions();
		if ( alertOptions == null ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> windows = (List<Map<String, Object>>) alertOptions
				.get(UserAlertOptions.TIME_WINDOWS);
		if ( windows == null ) {
			return null;
		}
		final Long intervalNodeId = (nodeId != null ? nodeId : alert.getNodeId());
		List<DateInterval> timePeriods = new ArrayList<>(windows.size());
		for ( Map<String, Object> window : windows ) {
			Object s = window.get("timeStart");
			Object e = window.get("timeEnd");
			if ( s != null && e != null ) {
				try {
					LocalTime start = timeFormatter.parse(s.toString(), LocalTime::from);
					LocalTime end = timeFormatter.parse(e.toString(), LocalTime::from);
					SolarNode node = nodeCache.get(intervalNodeId);
					ZoneId tz = ZoneOffset.UTC;
					if ( node != null ) {
						TimeZone nodeTz = node.getTimeZone();
						if ( nodeTz != null ) {
							tz = nodeTz.toZoneId();
						}
					} else {
						log.warn("Node {} not available, defaulting to UTC time zone", intervalNodeId);
					}
					Instant startTimeToday = nowDateTime.atZone(tz).with(start).toInstant();
					Instant endTimeToday = nowDateTime.atZone(tz).with(end).toInstant();
					timePeriods.add(new DateInterval(startTimeToday, endTimeToday, tz));
				} catch ( IllegalArgumentException t ) {
					log.warn("Error parsing time window time: {}", t.getMessage());
				}
			}
		}
		if ( !timePeriods.isEmpty() ) {
			// sort by start dates if there is more than one interval
			timePeriods.sort(Comparator.comparing(DateInterval::getStart));
		} else {
			timePeriods = null;
		}
		return timePeriods;
	}

	private void loadMostRecentNodeData(List<UserAlert> alerts) {
		// reset cache
		nodeCache.clear();
		nodeDataCache.clear();
		userDataCache.clear();

		// keep a reverse node ID -> user ID mapping
		Map<Long, Long> nodeUserMapping = new HashMap<>();

		// get set of unique user IDs and/or node IDs
		Set<Long> nodeIds = new HashSet<>(alerts.size());
		Set<Long> userIds = new HashSet<>(alerts.size());
		for ( UserAlert alert : alerts ) {
			if ( alert.getNodeId() != null ) {
				nodeIds.add(alert.getNodeId());
			} else {
				userIds.add(alert.getUserId());

				// need to associate all possible node IDs to this user ID
				List<UserNode> nodes = userNodeDao
						.findUserNodesForUser(new User(alert.getUserId(), null));
				for ( UserNode userNode : nodes ) {
					nodeCache.put(userNode.getNode().getId(), userNode.getNode());
					nodeUserMapping.put(userNode.getNode().getId(), alert.getUserId());
				}
			}
		}

		// load up data for users first, as that might pull in all node data already
		if ( !userIds.isEmpty() ) {
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setUserIds(userIds.toArray(Long[]::new));
			filter.setMostRecent(true);
			ObjectDatumStreamFilterResults<Datum, DatumPK> latestNodeData = datumDao
					.findFiltered(filter);
			for ( Datum match : latestNodeData.getResults() ) {
				// first add to node list
				final ObjectDatumStreamMetadata meta = latestNodeData
						.metadataForStreamId(match.getStreamId());
				if ( meta == null || meta.getKind() != ObjectDatumKind.Node ) {
					log.warn("Node stream metadata not available for datum match {}", match);
					continue;
				}
				final NodeDatumStreamPK pk = ObjectDatumStreamPK.nodeId(meta.getObjectId(),
						meta.getSourceId(), match.getTimestamp());
				List<NodeDatumStreamPK> datumMatches = nodeDataCache.computeIfAbsent(pk.getNodeId(),
						k -> new ArrayList<>());
				datumMatches.add(pk);

				// now add match to User list
				Long userId = nodeUserMapping.get(pk.getNodeId());
				if ( userId == null ) {
					// this must be an archived node; just ignore
					log.debug("No user ID found for node ID; assuming from archived node: {}",
							pk.getNodeId());
					continue;
				}
				datumMatches = userDataCache.computeIfAbsent(userId, k -> new ArrayList<>());
				datumMatches.add(pk);
			}
			log.debug("Loaded most recent datum for users {}: {}", userIds, userDataCache);
		}

		// we can remove any nodes already fetched via user query
		nodeIds.removeAll(nodeUserMapping.keySet());

		// for any node IDs still around, query for them now
		if ( !nodeIds.isEmpty() ) {
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setNodeIds(nodeIds.toArray(Long[]::new));
			filter.setMostRecent(true);
			ObjectDatumStreamFilterResults<Datum, DatumPK> latestNodeData = datumDao
					.findFiltered(filter);
			for ( Datum match : latestNodeData.getResults() ) {
				final ObjectDatumStreamMetadata meta = latestNodeData
						.metadataForStreamId(match.getStreamId());
				if ( meta == null || meta.getKind() != ObjectDatumKind.Node ) {
					log.warn("Node stream metadata not available for datum match {}", match);
					continue;
				}
				final NodeDatumStreamPK pk = ObjectDatumStreamPK.nodeId(meta.getObjectId(),
						meta.getSourceId(), match.getTimestamp());
				List<NodeDatumStreamPK> datumMatches = nodeDataCache.computeIfAbsent(pk.getNodeId(),
						k -> new ArrayList<>());
				if ( !nodeCache.containsKey(pk.getNodeId()) ) {
					nodeCache.put(pk.getNodeId(), solarNodeDao.get(pk.getNodeId()));
				}
				datumMatches.add(pk);
			}
			log.debug("Loaded most recent datum for nodes {}: {}", nodeIds, nodeDataCache);
		}
	}

	/**
	 * Get list of most recent datum associated with an alert. Depends on
	 * {@link #loadMostRecentNodeData(List)} having been already called.
	 *
	 * @param alert
	 *        The alert to get the most recent data for.
	 * @return The associated data, never {@literal null}.
	 */
	private List<NodeDatumStreamPK> getLatestNodeData(final UserAlert alert) {
		List<NodeDatumStreamPK> results;
		if ( alert.getNodeId() != null ) {
			results = nodeDataCache.get(alert.getNodeId());
		} else {
			results = userDataCache.get(alert.getUserId());
		}
		return (results == null ? Collections.emptyList() : results);
	}

	private boolean withinIntervals(final Instant now, List<DateInterval> intervals) {
		if ( intervals == null ) {
			return true;
		}
		for ( DateInterval i : intervals ) {
			if ( i.getStart().isAfter(now) || i.getEnd().isBefore(now) ) {
				return false;
			}
		}
		return true;
	}

	private Instant startOfNextTimePeriod(final Instant now, List<DateInterval> intervals) {
		if ( intervals == null || intervals.isEmpty() ) {
			return Instant.now();
		}
		DateInterval found = null;
		DateInterval earliest = null;
		for ( DateInterval i : intervals ) {
			if ( i.getStart().isAfter(now)
					&& (found == null || found.getStart().isAfter(i.getStart())) ) {
				// this time period starts later than now, so that is the next period to work with
				found = i;
			}
			if ( earliest == null || earliest.getStart().isAfter(i.getStart()) ) {
				earliest = i;
			}
		}

		if ( found != null ) {
			return found.getStart();
		}

		// no time period later than now, so make the next period the start of the earliest interval, tomorrow
		return earliest.getStart().plus(1, ChronoUnit.DAYS);
	}

	private NodeDatumStreamPK getFirstStaleDatum(final UserAlert alert, final Instant now,
			final Number age, PathMatcher sourceIdMatcher, final List<String> sourceIdPatterns,
			final DateTimeFormatter timeFormatter, final List<DateInterval> outputIntervals) {
		NodeDatumStreamPK stale = null;
		List<NodeDatumStreamPK> latestNodeData = getLatestNodeData(alert);
		List<DateInterval> intervals = new ArrayList<>(2);
		if ( alert.getNodeId() != null ) {
			try {
				intervals = parseAlertTimeWindows(now, timeFormatter, alert, alert.getNodeId());
			} catch ( ClassCastException e ) {
				log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
			}
		}

		for ( NodeDatumStreamPK datum : latestNodeData ) {
			List<DateInterval> nodeIntervals = intervals;
			if ( alert.getNodeId() == null ) {
				try {
					nodeIntervals = parseAlertTimeWindows(now, timeFormatter, alert, datum.getNodeId());
					if ( nodeIntervals != null ) {
						if ( intervals == null ) {
							intervals = nodeIntervals;
						} else {
							for ( DateInterval interval : nodeIntervals ) {
								if ( !intervals.contains(interval) ) {
									intervals.add(interval);
								}
							}
						}
					}
				} catch ( ClassCastException e ) {
					log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
					continue;
				}
			}
			if ( datum.getTimestamp().toEpochMilli() + (long) (age.doubleValue() * 1000) < now
					.toEpochMilli()
					&& sourceIdMatches(sourceIdMatcher, sourceIdPatterns, datum.getSourceId())
					&& withinIntervals(now, nodeIntervals) ) {
				stale = datum;
				break;
			}
		}
		if ( intervals != null && outputIntervals != null ) {
			outputIntervals.addAll(intervals);
		}
		return stale;
	}

	private static boolean sourceIdMatches(PathMatcher matcher, List<String> sourceIdPatterns,
			String sourceId) {
		if ( sourceIdPatterns == null ) {
			return true;
		}
		for ( String sourceIdPattern : sourceIdPatterns ) {
			if ( matcher.isPattern(sourceIdPattern) ) {
				if ( matcher.match(sourceIdPattern, sourceId) ) {
					return true;
				}
			} else if ( sourceIdPattern.equals(sourceId) ) {
				return true;
			}
		}
		return false;
	}

	private NodeDatumStreamPK getFirstNonStaleDatum(final UserAlert alert, final Instant now,
			final Number age, final PathMatcher sourceIdMatcher, final List<String> sourceIdPatterns) {
		NodeDatumStreamPK nonStale = null;
		List<NodeDatumStreamPK> latestNodeData = getLatestNodeData(alert);
		for ( NodeDatumStreamPK datum : latestNodeData ) {
			if ( !datum.getTimestamp().plusMillis((long) (age.doubleValue() * 1000)).isBefore(now)
					&& sourceIdMatches(sourceIdMatcher, sourceIdPatterns, datum.getSourceId()) ) {
				nonStale = datum;
				break;
			}
		}
		return nonStale;
	}

	private void sendAlertMail(UserAlert alert, String subjectKey, String resourcePath,
			NodeDatumStreamPK datum) {
		if ( alert.getStatus() == UserAlertStatus.Suppressed ) {
			// no emails for this alert
			log.debug("Alert email suppressed: {}; datum {}; subject {}", alert, datum, subjectKey);
			return;
		}
		User user = userDao.get(alert.getUserId());
		SolarNode node = (datum != null ? nodeCache.get(datum.getNodeId()) : null);
		BasicMailAddress addr = null;
		String[] emails = alert.optionEmailTos();
		if ( (emails == null || emails.length == 0) ) {
			if ( user != null ) {
				addr = new BasicMailAddress(user.getName(), user.getEmail());
			}
		} else {
			addr = new BasicMailAddress(emails);
		}
		if ( user != null && node != null && addr != null ) {
			Locale locale = Locale.US; // TODO: get Locale from User entity
			Map<String, Object> model = new HashMap<>(4);
			model.put("alert", alert);
			model.put("user", user);
			model.put("datum", datum);

			// add a formatted datum date to model
			DateTimeFormatter dateFormat = timestampFormat.withLocale(locale);
			if ( node != null && node.getTimeZone() != null ) {
				dateFormat = dateFormat.withZone(node.getTimeZone().toZoneId());
			}
			model.put("datumDate", dateFormat.format(datum.getTimestamp()));

			String subject = messageSource.getMessage(subjectKey, new Object[] { datum.getNodeId() },
					locale);

			log.debug("Sending NodeStaleData alert {} to {} with model {}", subject, user.getEmail(),
					model);
			ClasspathResourceMessageTemplateDataSource msg = new ClasspathResourceMessageTemplateDataSource(
					locale, subject, resourcePath, model);
			msg.setClassLoader(getClass().getClassLoader());
			mailService.sendMail(addr, msg);
		}
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	public String getMailTemplateResource() {
		return mailTemplateResource;
	}

	public void setMailTemplateResource(String mailTemplateResource) {
		this.mailTemplateResource = mailTemplateResource;
	}

	public DateTimeFormatter getTimestampFormat() {
		return timestampFormat;
	}

	public void setTimestampFormat(DateTimeFormatter timestampFormat) {
		this.timestampFormat = timestampFormat;
	}

	public String getMailTemplateResolvedResource() {
		return mailTemplateResolvedResource;
	}

	public void setMailTemplateResolvedResource(String mailTemplateResolvedResource) {
		this.mailTemplateResolvedResource = mailTemplateResolvedResource;
	}

	public int getInitialAlertReminderDelayMinutes() {
		return initialAlertReminderDelayMinutes;
	}

	public void setInitialAlertReminderDelayMinutes(int initialAlertReminderDelayMinutes) {
		this.initialAlertReminderDelayMinutes = initialAlertReminderDelayMinutes;
	}

	public int getAlertReminderFrequencyMultiplier() {
		return alertReminderFrequencyMultiplier;
	}

	public void setAlertReminderFrequencyMultiplier(int alertReminderFrequencyMultiplier) {
		this.alertReminderFrequencyMultiplier = alertReminderFrequencyMultiplier;
	}

}
