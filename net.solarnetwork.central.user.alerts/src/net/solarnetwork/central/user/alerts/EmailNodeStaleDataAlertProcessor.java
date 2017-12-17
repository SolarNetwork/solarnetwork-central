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

package net.solarnetwork.central.user.alerts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
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

/**
 * Process stale data alerts for nodes.
 * 
 * @author matt
 * @version 1.2
 */
public class EmailNodeStaleDataAlertProcessor implements UserAlertBatchProcessor {

	/** The default value for {@link #getBatchSize()}. */
	public static final Integer DEFAULT_BATCH_SIZE = 50;

	/** The default value for {@link #getMailTemplateResource()}. */
	public static final String DEFAULT_MAIL_TEMPLATE_RESOURCE = "/net/solarnetwork/central/user/alerts/user-alert-NodeStaleData.txt";

	/** The default value for {@link #getMailTemplateResolvedResource()}. */
	public static final String DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE = "/net/solarnetwork/central/user/alerts/user-alert-NodeStaleData-Resolved.txt";

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
	private final GeneralNodeDatumDao generalNodeDatumDao;
	private final MailService mailService;
	private Integer batchSize = DEFAULT_BATCH_SIZE;
	private final MessageSource messageSource;
	private String mailTemplateResource = DEFAULT_MAIL_TEMPLATE_RESOURCE;
	private String mailTemplateResolvedResource = DEFAULT_MAIL_TEMPLATE_RESOLVED_RESOURCE;
	private DateTimeFormatter timestampFormat = DateTimeFormat.forPattern("d MMM yyyy HH:mm z");
	private int initialAlertReminderDelayMinutes = 60;
	private int alertReminderFrequencyMultiplier = 4;

	// maintain a cache of node data during the execution of the job (cleared after each invocation)
	private final Map<Long, SolarNode> nodeCache = new HashMap<Long, SolarNode>(64);
	private final Map<Long, List<GeneralNodeDatumFilterMatch>> nodeDataCache = new HashMap<Long, List<GeneralNodeDatumFilterMatch>>(
			64);
	private final Map<Long, List<GeneralNodeDatumFilterMatch>> userDataCache = new HashMap<Long, List<GeneralNodeDatumFilterMatch>>(
			16);

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
	 * @param generalNodeDatumDao
	 *        The {@link GeneralNodeDatumDao} to use.
	 * @param mailService
	 *        The {@link MailService} to use.
	 * @param messageSource
	 *        The {@link MessageSource} to use.
	 */
	public EmailNodeStaleDataAlertProcessor(SolarNodeDao solarNodeDao, UserDao userDao,
			UserNodeDao userNodeDao, UserAlertDao userAlertDao,
			UserAlertSituationDao userAlertSituationDao, GeneralNodeDatumDao generalNodeDatumDao,
			MailService mailService, MessageSource messageSource) {
		super();
		this.solarNodeDao = solarNodeDao;
		this.userDao = userDao;
		this.userNodeDao = userNodeDao;
		this.userAlertDao = userAlertDao;
		this.userAlertSituationDao = userAlertSituationDao;
		this.generalNodeDatumDao = generalNodeDatumDao;
		this.mailService = mailService;
		this.messageSource = messageSource;
	}

	/**
	 * Get the current system time. Exposed to support testing.
	 * 
	 * @return The current system time.
	 * @since 1.2
	 */
	protected long getCurrentTime() {
		return System.currentTimeMillis();
	}

	@Override
	public Long processAlerts(Long lastProcessedAlertId, DateTime validDate) {
		if ( validDate == null ) {
			validDate = new DateTime();
		}
		List<UserAlert> alerts = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				lastProcessedAlertId, validDate, batchSize);
		Long lastAlertId = null;
		final long now = getCurrentTime();
		final DateTime nowDateTime = new DateTime(now);
		final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("H:mm");
		try {
			loadMostRecentNodeData(alerts);
			for ( UserAlert alert : alerts ) {
				Map<String, Object> alertOptions = alert.getOptions();
				if ( alertOptions == null ) {
					continue;
				}

				// extract options
				Number age;
				String[] sourceIds = null;
				try {
					age = (Number) alertOptions.get(UserAlertOptions.AGE_THRESHOLD);
					@SuppressWarnings("unchecked")
					List<String> sources = (List<String>) alertOptions.get(UserAlertOptions.SOURCE_IDS);
					if ( sources != null ) {
						sourceIds = sources.toArray(new String[sources.size()]);
					}
				} catch ( ClassCastException e ) {
					log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
					continue;
				}

				if ( age == null ) {
					log.debug("Skipping alert {} that does not include {} option", alert,
							UserAlertOptions.AGE_THRESHOLD);
					continue;
				}

				if ( sourceIds != null ) {
					// sort so we can to binarySearch later
					Arrays.sort(sourceIds);
				}

				// look for first stale data matching age + source criteria
				final List<Interval> timePeriods = new ArrayList<Interval>(2);
				GeneralNodeDatumFilterMatch stale = getFirstStaleDatum(alert, nowDateTime, age,
						sourceIds, timeFormatter, timePeriods);

				Map<String, Object> staleInfo = new HashMap<String, Object>(4);
				if ( stale != null ) {
					staleInfo.put(SITUATION_INFO_DATUM_CREATED,
							Long.valueOf(stale.getId().getCreated().getMillis()));
					staleInfo.put(SITUATION_INFO_NODE_ID, stale.getId().getNodeId());
					staleInfo.put(SITUATION_INFO_SOURCE_ID, stale.getId().getSourceId());
				}

				// get UserAlertSitutation for this alert
				UserAlertSituation sit = userAlertSituationDao
						.getActiveAlertSituationForAlert(alert.getId());
				if ( stale != null ) {
					long notifyOffset = 0;
					if ( sit == null ) {
						sit = new UserAlertSituation();
						sit.setCreated(new DateTime(now));
						sit.setAlert(alert);
						sit.setStatus(UserAlertSituationStatus.Active);
						sit.setNotified(new DateTime(now));
						sit.setInfo(staleInfo);

					} else if ( sit.getNotified().equals(sit.getCreated()) ) {
						notifyOffset = (initialAlertReminderDelayMinutes * 60L * 1000L);
					} else {
						notifyOffset = ((sit.getNotified().getMillis() - sit.getCreated().getMillis())
								* alertReminderFrequencyMultiplier);
					}

					// taper off the alerts so the become less frequent over time
					if ( (sit.getNotified().getMillis() + notifyOffset) <= now ) {
						sendAlertMail(alert, "user.alert.NodeStaleData.mail.subject",
								mailTemplateResource, stale);
						sit.setNotified(new DateTime(now));
					}
					if ( sit.getNotified().getMillis() == now || sit.getInfo() == null
							|| !staleInfo.equals(sit.getInfo()) ) {
						userAlertSituationDao.store(sit);
					}
				} else {
					// not stale, so mark valid for age span
					final boolean withinTimePeriods = withinIntervals(now, timePeriods);
					DateTime newValidTo;
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
						sit.setNotified(new DateTime(now));
						userAlertSituationDao.store(sit);

						GeneralNodeDatumFilterMatch nonStale = getFirstNonStaleDatum(alert, now, age,
								sourceIds);

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
				&& lastAlertId.equals(alerts.get(alerts.size() - 1).getId()) ) {
			// we've finished our batch
			lastAlertId = null;
		}

		return lastAlertId;
	}

	private List<Interval> parseAlertTimeWindows(final DateTime nowDateTime,
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
		List<Interval> timePeriods = new ArrayList<Interval>(windows.size());
		for ( Map<String, Object> window : windows ) {
			Object s = window.get("timeStart");
			Object e = window.get("timeEnd");
			if ( s != null && e != null ) {
				try {
					LocalTime start = timeFormatter.parseLocalTime(s.toString());
					LocalTime end = timeFormatter.parseLocalTime(e.toString());
					SolarNode node = nodeCache.get(intervalNodeId);
					DateTimeZone tz = DateTimeZone.UTC;
					if ( node != null ) {
						TimeZone nodeTz = node.getTimeZone();
						if ( nodeTz != null ) {
							tz = DateTimeZone.forTimeZone(nodeTz);
						}
					} else {
						log.warn("Node {} not available, defaulting to UTC time zone", intervalNodeId);
					}
					DateTime startTimeToday = start.toDateTime(nowDateTime.toDateTime(tz));
					DateTime endTimeToday = end.toDateTime(nowDateTime.toDateTime(tz));
					timePeriods.add(new Interval(startTimeToday, endTimeToday));
				} catch ( IllegalArgumentException t ) {
					log.warn("Error parsing time window time: {}", t.getMessage());
				}
			}
		}
		if ( timePeriods.size() > 0 ) {
			// sort by start dates if there is more than one interval
			Collections.sort(timePeriods, new Comparator<Interval>() {

				@Override
				public int compare(Interval o1, Interval o2) {
					return o1.getStart().compareTo(o2.getStart());
				}
			});
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
		Map<Long, Long> nodeUserMapping = new HashMap<Long, Long>();

		// get set of unique user IDs and/or node IDs
		Set<Long> nodeIds = new HashSet<Long>(alerts.size());
		Set<Long> userIds = new HashSet<Long>(alerts.size());
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
		if ( userIds.isEmpty() == false ) {
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setUserIds(userIds.toArray(new Long[userIds.size()]));
			filter.setMostRecent(true);
			FilterResults<GeneralNodeDatumFilterMatch> latestNodeData = generalNodeDatumDao
					.findFiltered(filter, null, null, null);
			for ( GeneralNodeDatumFilterMatch match : latestNodeData.getResults() ) {
				// first add to node list
				List<GeneralNodeDatumFilterMatch> datumMatches = nodeDataCache
						.get(match.getId().getNodeId());
				if ( datumMatches == null ) {
					datumMatches = new ArrayList<GeneralNodeDatumFilterMatch>();
					nodeDataCache.put(match.getId().getNodeId(), datumMatches);
				}
				datumMatches.add(match);

				// now add match to User list
				Long userId = nodeUserMapping.get(match.getId().getNodeId());
				if ( userId == null ) {
					log.warn("No user ID found for node ID: {}", match.getId().getNodeId());
					continue;
				}
				datumMatches = userDataCache.get(userId);
				if ( datumMatches == null ) {
					datumMatches = new ArrayList<GeneralNodeDatumFilterMatch>();
					userDataCache.put(userId, datumMatches);
				}
				datumMatches.add(match);
			}
			log.debug("Loaded most recent datum for users {}: {}", userIds, userDataCache);
		}

		// we can remove any nodes already fetched via user query
		nodeIds.removeAll(nodeUserMapping.keySet());

		// for any node IDs still around, query for them now
		if ( nodeIds.isEmpty() == false ) {
			DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeIds(nodeIds.toArray(new Long[nodeIds.size()]));
			filter.setMostRecent(true);
			FilterResults<GeneralNodeDatumFilterMatch> latestNodeData = generalNodeDatumDao
					.findFiltered(filter, null, null, null);
			for ( GeneralNodeDatumFilterMatch match : latestNodeData.getResults() ) {
				List<GeneralNodeDatumFilterMatch> datumMatches = nodeDataCache
						.get(match.getId().getNodeId());
				if ( datumMatches == null ) {
					datumMatches = new ArrayList<GeneralNodeDatumFilterMatch>();
					nodeDataCache.put(match.getId().getNodeId(), datumMatches);
				}
				if ( !nodeCache.containsKey(match.getId().getNodeId()) ) {
					nodeCache.put(match.getId().getNodeId(),
							solarNodeDao.get(match.getId().getNodeId()));
				}
				datumMatches.add(match);
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
	 * @return The associated data, never <em>null</em>.
	 */
	private List<GeneralNodeDatumFilterMatch> getLatestNodeData(final UserAlert alert) {
		List<GeneralNodeDatumFilterMatch> results;
		if ( alert.getNodeId() != null ) {
			results = nodeDataCache.get(alert.getNodeId());
		} else {
			results = userDataCache.get(alert.getUserId());
		}
		return (results == null ? Collections.<GeneralNodeDatumFilterMatch> emptyList() : results);
	}

	private boolean withinIntervals(final long now, List<Interval> intervals) {
		if ( intervals == null ) {
			return true;
		}
		for ( Interval i : intervals ) {
			if ( !i.contains(now) ) {
				return false;
			}
		}
		return true;
	}

	private DateTime startOfNextTimePeriod(final long now, List<Interval> intervals) {
		if ( intervals == null || intervals.size() < 1 ) {
			return new DateTime();
		}
		Interval found = null;
		Interval earliest = null;
		for ( Interval i : intervals ) {
			if ( i.isAfter(now) && (found == null || found.isAfter(i.getStartMillis())) ) {
				// this time period starts later than now, so that is the next period to work with
				found = i;
			}
			if ( earliest == null || earliest.isAfter(i.getStartMillis()) ) {
				earliest = i;
			}
		}

		if ( found != null ) {
			return found.getStart();
		}

		// no time period later than now, so make the next period the start of the earliest interval, tomorrow
		return earliest.getStart().plusDays(1);
	}

	private GeneralNodeDatumFilterMatch getFirstStaleDatum(final UserAlert alert, final DateTime now,
			final Number age, final String[] sourceIds, final DateTimeFormatter timeFormatter,
			final List<Interval> outputIntervals) {
		GeneralNodeDatumFilterMatch stale = null;
		List<GeneralNodeDatumFilterMatch> latestNodeData = getLatestNodeData(alert);
		List<Interval> intervals = new ArrayList<Interval>(2);
		if ( alert.getNodeId() != null ) {
			try {
				intervals = parseAlertTimeWindows(now, timeFormatter, alert, alert.getNodeId());
			} catch ( ClassCastException e ) {
				log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
			}
		}

		for ( GeneralNodeDatumFilterMatch datum : latestNodeData ) {
			List<Interval> nodeIntervals = intervals;
			if ( alert.getNodeId() == null ) {
				try {
					nodeIntervals = parseAlertTimeWindows(now, timeFormatter, alert,
							datum.getId().getNodeId());
					if ( nodeIntervals != null ) {
						for ( Interval interval : nodeIntervals ) {
							if ( !intervals.contains(interval) ) {
								intervals.add(interval);
							}
						}
					}
				} catch ( ClassCastException e ) {
					log.warn("Unexpected option data type in alert {}: {}", alert, e.getMessage());
					continue;
				}
			}
			if ( datum.getId().getCreated().getMillis() + (long) (age.doubleValue() * 1000) < now
					.getMillis()
					&& (sourceIds == null
							|| Arrays.binarySearch(sourceIds, datum.getId().getSourceId()) >= 0)
					&& withinIntervals(now.getMillis(), nodeIntervals) ) {
				stale = datum;
				break;
			}
		}
		if ( intervals != null && outputIntervals != null ) {
			outputIntervals.addAll(intervals);
		}
		return stale;
	}

	private GeneralNodeDatumFilterMatch getFirstNonStaleDatum(final UserAlert alert, final long now,
			final Number age, final String[] sourceIds) {
		GeneralNodeDatumFilterMatch nonStale = null;
		List<GeneralNodeDatumFilterMatch> latestNodeData = getLatestNodeData(alert);
		for ( GeneralNodeDatumFilterMatch datum : latestNodeData ) {
			if ( datum.getId().getCreated().getMillis() + (long) (age.doubleValue() * 1000) >= now
					&& (sourceIds == null
							|| Arrays.binarySearch(sourceIds, datum.getId().getSourceId()) >= 0) ) {
				nonStale = datum;
				break;
			}
		}
		return nonStale;
	}

	private void sendAlertMail(UserAlert alert, String subjectKey, String resourcePath,
			GeneralNodeDatumFilterMatch datum) {
		if ( alert.getStatus() == UserAlertStatus.Suppressed ) {
			// no emails for this alert
			log.debug("Alert email suppressed: {}; datum {}; subject {}", alert, datum, subjectKey);
			return;
		}
		User user = userDao.get(alert.getUserId());
		SolarNode node = (datum != null ? nodeCache.get(datum.getId().getNodeId()) : null);
		if ( user != null && node != null ) {
			BasicMailAddress addr = new BasicMailAddress(user.getName(), user.getEmail());
			Locale locale = Locale.US; // TODO: get Locale from User entity
			Map<String, Object> model = new HashMap<String, Object>(4);
			model.put("alert", alert);
			model.put("user", user);
			model.put("datum", datum);

			// add a formatted datum date to model
			DateTimeFormatter dateFormat = timestampFormat.withLocale(locale);
			if ( node != null && node.getTimeZone() != null ) {
				dateFormat = dateFormat.withZone(DateTimeZone.forTimeZone(node.getTimeZone()));
			}
			model.put("datumDate", dateFormat.print(datum.getId().getCreated()));

			String subject = messageSource.getMessage(subjectKey,
					new Object[] { datum.getId().getNodeId() }, locale);

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
