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

package net.solarnetwork.central.user.datum.alert.jobs;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
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
import net.solarnetwork.central.mail.support.SimpleMessageDataSource;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;
import net.solarnetwork.central.user.alert.jobs.UserAlertBatchProcessor;
import net.solarnetwork.central.user.biz.UserAlertRendererResolver;
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
import net.solarnetwork.service.TemplateRenderer;
import net.solarnetwork.util.DateUtils;

/**
 * Process stale data alerts for nodes.
 *
 * @author matt
 * @version 3.0
 */
public class EmailNodeStaleDataAlertProcessor implements UserAlertBatchProcessor {

	/** The default value for {@link #getBatchSize()}. */
	public static final Integer DEFAULT_BATCH_SIZE = 50;

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

	/**
	 * A {@code UserAlertSituation} {@code info} key for an associated list of
	 * stale datum ID objects.
	 *
	 * @since 3.0
	 */
	public static final String SITUATION_INFO_STALE_DATUM_IDS = "stale";

	/**
	 * A {@code UserAlertSituation} {@code info} key for an associated datum
	 * timestamp.
	 *
	 * @since 3.0
	 */
	public static final String SITUATION_INFO_TIMESTAMP = "timestamp";

	/** The message bundle name to use for versioned messages. */
	public static final String MESSAGE_BUNDLE_NAME = "snf.stale-datum-alert";

	/** The message bundle name to use for global versioned messages. */
	public static final String GLOBAL_MESSAGE_BUNDLE_NAME = "snf.global";

	private static final String[] MESSAGE_BUNDLE_NAMES = new String[] { GLOBAL_MESSAGE_BUNDLE_NAME,
			MESSAGE_BUNDLE_NAME };

	private final InstantSource clock;
	private final SolarNodeDao solarNodeDao;
	private final UserDao userDao;
	private final UserNodeDao userNodeDao;
	private final UserAlertDao userAlertDao;
	private final UserAlertSituationDao userAlertSituationDao;
	private final DatumEntityDao datumDao;
	private final MailService mailService;
	private final VersionedMessageDao messageDao;
	private Integer batchSize = DEFAULT_BATCH_SIZE;
	private int initialAlertReminderDelayMinutes = 60;
	private int alertReminderFrequencyMultiplier = 4;

	private @Nullable List<UserAlertRendererResolver> rendererResolvers;
	private @Nullable Cache<String, VersionedMessages> messageCache;

	// maintain a cache of node data during the execution of the job (cleared after each invocation)
	private final Map<Long, SolarNode> nodeCache = new HashMap<>(64);
	private final Map<Long, List<NodeDatumStreamPK>> nodeDataCache = new HashMap<>(64);
	private final Map<Long, List<NodeDatumStreamPK>> userDataCache = new HashMap<>(16);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Construct with properties.
	 *
	 * @param clock
	 *        the clock to use
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
	 * @param messageDao
	 *        the message DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public EmailNodeStaleDataAlertProcessor(InstantSource clock, SolarNodeDao solarNodeDao,
			UserDao userDao, UserNodeDao userNodeDao, UserAlertDao userAlertDao,
			UserAlertSituationDao userAlertSituationDao, DatumEntityDao datumDao,
			MailService mailService, VersionedMessageDao messageDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.solarNodeDao = requireNonNullArgument(solarNodeDao, "solarNodeDao");
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.userAlertDao = requireNonNullArgument(userAlertDao, "userAlertDao");
		this.userAlertSituationDao = requireNonNullArgument(userAlertSituationDao,
				"userAlertSituationDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.mailService = requireNonNullArgument(mailService, "mailService");
		this.messageDao = requireNonNullArgument(messageDao, "messageDao");
	}

	@Override
	public @Nullable Long processAlerts(@Nullable Long lastProcessedAlertId, Instant validDate) {
		if ( validDate == null ) {
			validDate = clock.instant();
		}
		List<UserAlert> alerts = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				lastProcessedAlertId, validDate, batchSize);
		Long lastAlertId = null;
		final Instant now = clock.instant();
		final DateTimeFormatter timeFormatter = DateUtils.LOCAL_TIME;
		try {
			loadMostRecentNodeData(alerts);
			for ( UserAlert alert : alerts ) {
				// extract options
				final Integer age = alert.optionAgeThreshold();
				final List<String> sourceIdPatterns = alert.optionSourceIds();

				if ( age == null ) {
					log.debug("Skipping alert {} that does not include {} option", alert,
							UserAlertOptions.AGE_THRESHOLD);
					continue;
				}

				final PathMatcher sourceIdMatcher = new AntPathMatcher();

				// look for first stale data matching age + source criteria
				final List<DateInterval> timePeriods = new ArrayList<>(2);
				List<NodeDatumStreamPK> stale = getStaleDatum(alert, now, age, sourceIdMatcher,
						sourceIdPatterns, timeFormatter, timePeriods);

				Map<String, Object> staleInfo = new HashMap<>(4);
				if ( stale != null ) {
					staleInfo.put(SITUATION_INFO_STALE_DATUM_IDS, stale.stream().map(id -> {
						var data = new LinkedHashMap<>(3);
						data.put(SITUATION_INFO_NODE_ID, id.getNodeId());
						data.put(SITUATION_INFO_SOURCE_ID, id.getSourceId());
						data.put(SITUATION_INFO_TIMESTAMP, id.getTimestamp().toString());
						return data;
					}).toList());
				}

				// get UserAlertSituation for this alert
				UserAlertSituation sit = userAlertSituationDao
						.getActiveAlertSituationForAlert(alert.id());
				if ( stale != null ) {
					long notifyOffset = 0;
					if ( sit == null || sit.getNotified() == null ) {
						sit = new UserAlertSituation();
						sit.setCreated(now);
						sit.setAlert(alert);
						sit.setStatus(UserAlertSituationStatus.Active);
						sit.setNotified(now);
						sit.setInfo(staleInfo);
					} else if ( sit.getNotified().equals(sit.getCreated()) ) {
						notifyOffset = (initialAlertReminderDelayMinutes * 60L * 1000L);
					} else {
						notifyOffset = ((sit.getNotified().toEpochMilli() - sit.created().toEpochMilli())
								* alertReminderFrequencyMultiplier);
					}

					// taper off the alerts so the become less frequent over time
					if ( sit.notified().plusMillis(notifyOffset).compareTo(now) >= 0 ) {
						sendAlertMail(now, sit, "mail.subject.stale", stale);
						sit.setNotified(now);
					}
					if ( sit.notified().equals(now) || sit.getInfo() == null
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
					log.debug("Marking alert {} valid to {}", alert.id(), newValidTo);
					userAlertDao.updateValidTo(alert.id(), newValidTo);
					alert.setValidTo(newValidTo);
					if ( sit != null && withinTimePeriods ) {
						// make Resolved
						sit.setStatus(UserAlertSituationStatus.Resolved);
						sit.setNotified(now);
						userAlertSituationDao.save(sit);

						List<NodeDatumStreamPK> nonStale = getNonStaleDatum(alert, now, age,
								sourceIdMatcher, sourceIdPatterns);

						sendAlertMail(now, sit, "mail.subject.resolved", nonStale);
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

	private @Nullable List<DateInterval> parseAlertTimeWindows(final Instant nowDateTime,
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
				List<UserNode> nodes = userNodeDao.findUserNodesForUser(new User(alert.getUserId(), ""));
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
						_ -> new ArrayList<>());
				datumMatches.add(pk);

				// now add match to User list
				Long userId = nodeUserMapping.get(pk.getNodeId());
				if ( userId == null ) {
					// this must be an archived node; just ignore
					log.debug("No user ID found for node ID; assuming from archived node: {}",
							pk.getNodeId());
					continue;
				}
				datumMatches = userDataCache.computeIfAbsent(userId, _ -> new ArrayList<>());
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
						_ -> new ArrayList<>());
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
	 * @return The associated data, never {@code null}.
	 */
	private List<NodeDatumStreamPK> getLatestNodeData(final UserAlert alert) {
		List<NodeDatumStreamPK> results;
		if ( alert.getNodeId() != null ) {
			results = nodeDataCache.get(alert.getNodeId());
		} else {
			results = userDataCache.get(alert.getUserId());
		}
		return (results == null ? List.of() : results);
	}

	private boolean withinIntervals(final Instant now, @Nullable List<DateInterval> intervals) {
		if ( intervals == null || intervals.isEmpty() ) {
			return true;
		}
		for ( DateInterval i : intervals ) {
			if ( i.getStart().isAfter(now) || i.getEnd().isBefore(now) ) {
				return false;
			}
		}
		return true;
	}

	private Instant startOfNextTimePeriod(final Instant now, @Nullable List<DateInterval> intervals) {
		if ( intervals == null || intervals.isEmpty() ) {
			return clock.instant();
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
		return nonnull(earliest, "earliest").getStart().plus(1, ChronoUnit.DAYS);
	}

	private @Nullable List<NodeDatumStreamPK> getStaleDatum(final UserAlert alert, final Instant now,
			final Number age, PathMatcher sourceIdMatcher, final @Nullable List<String> sourceIdPatterns,
			final DateTimeFormatter timeFormatter, final List<DateInterval> outputIntervals) {
		final List<NodeDatumStreamPK> latestNodeData = getLatestNodeData(alert);
		final long ageMs = (long) (age.doubleValue() * 1000L);

		List<NodeDatumStreamPK> stale = null;
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
			if ( (datum.getTimestamp().toEpochMilli() + ageMs) < now.toEpochMilli()
					&& sourceIdMatches(sourceIdMatcher, sourceIdPatterns, datum.getSourceId())
					&& withinIntervals(now, nodeIntervals) ) {
				if ( stale == null ) {
					stale = new ArrayList<>(8);
				}
				stale.add(datum);
			}
		}
		if ( intervals != null && outputIntervals != null ) {
			outputIntervals.addAll(intervals);
		}
		return stale;
	}

	private static boolean sourceIdMatches(PathMatcher matcher, @Nullable List<String> sourceIdPatterns,
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

	private @Nullable List<NodeDatumStreamPK> getNonStaleDatum(final UserAlert alert, final Instant now,
			final Number age, final PathMatcher sourceIdMatcher,
			final @Nullable List<String> sourceIdPatterns) {
		List<NodeDatumStreamPK> nonStale = null;
		final List<NodeDatumStreamPK> latestNodeData = getLatestNodeData(alert);
		final long ageMs = (long) (age.doubleValue() * 1000L);
		for ( NodeDatumStreamPK datum : latestNodeData ) {
			if ( !datum.getTimestamp().plusMillis(ageMs).isBefore(now)
					&& sourceIdMatches(sourceIdMatcher, sourceIdPatterns, datum.getSourceId()) ) {
				if ( nonStale == null ) {
					nonStale = new ArrayList<>(8);
				}
				nonStale.add(datum);
			}
		}
		return nonStale;
	}

	private void sendAlertMail(final Instant now, final UserAlertSituation sit, final String subjectKey,
			List<NodeDatumStreamPK> datum) {
		final UserAlert alert = sit.getAlert();
		if ( alert.getStatus() == UserAlertStatus.Suppressed ) {
			// no emails for this alert
			log.debug("Alert email suppressed: {}; datum {}; subject {}", alert, datum, subjectKey);
			return;
		}
		User user = userDao.get(alert.getUserId());
		if ( user == null ) {
			return;
		}

		BasicMailAddress addr = null;
		String[] emails = alert.optionEmailTos();
		if ( (emails == null || emails.length == 0) ) {
			addr = new BasicMailAddress(user.getName(), user.getEmail());
		} else {
			addr = new BasicMailAddress(emails);
		}

		final Locale locale = Locale.US; // TODO: get Locale from User entity

		final ResolvedRenderer renderer = renderer(sit, MimeTypeUtils.TEXT_HTML, locale);
		final var messageSource = new VersionedMessageDaoMessageSource(messageDao, MESSAGE_BUNDLE_NAMES,
				now, messageCache);
		final Map<String, Object> model = renderer.resolver.templateParametersForAlert(user, sit, datum,
				locale);
		model.put("messages", messageSource.propertiesForLocale(locale));

		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try {
			renderer.renderer.render(locale, MimeTypeUtils.TEXT_HTML, model, byos);
		} catch ( IOException e ) {
			throw new IllegalStateException("Error generating alert email: " + e.getMessage(), e);
		}

		String subject = messageSource.getMessage(subjectKey,
				new Object[] { situationNodeIds(sit, datum) }, locale);

		log.debug("Sending NodeStaleData alert {} to {} with model {}", subject, user.getEmail(), model);
		mailService.sendMail(addr,
				new SimpleMessageDataSource(subject, byos.toString(StandardCharsets.UTF_8)));
	}

	/**
	 * Get a sorted node ID(s) list for the situation, based on a set of
	 * optional node related entities.
	 * 
	 * @param nodes
	 *        the optional node related entities to extract node IDs from
	 * @return a display listing of node IDs, or an empty string if none
	 */
	private @Nullable String situationNodeIds(final UserAlertSituation sit,
			@Nullable List<NodeDatumStreamPK> nodes) {
		StringBuilder buf = new StringBuilder(32);
		if ( nodes != null && !nodes.isEmpty() ) {
			final SortedSet<Long> nodeIds = new TreeSet<>();
			for ( NodeDatumStreamPK node : nodes ) {
				nodeIds.add(node.getNodeId());
			}
			for ( Long nodeId : nodeIds ) {
				if ( !buf.isEmpty() ) {
					buf.append(", ");
				}
				buf.append(nodeId);
			}
		} else if ( sit.getAlert() != null && sit.getAlert().getNodeId() != null ) {
			buf.append(sit.getAlert().getNodeId());
		}
		return (buf.isEmpty() ? null : buf.toString());
	}

	private record ResolvedRenderer(UserAlertRendererResolver resolver, TemplateRenderer renderer) {

	}

	private ResolvedRenderer renderer(UserAlertSituation situation, MimeType mimeType, Locale locale) {
		if ( rendererResolvers != null ) {
			for ( UserAlertRendererResolver resolver : rendererResolvers ) {
				TemplateRenderer r = resolver.rendererForAlert(situation, mimeType, locale);
				if ( r != null ) {
					return new ResolvedRenderer(resolver, r);
				}
			}
		}
		String msg = String.format("MIME %s not supported for alert rendering.", mimeType);
		throw new IllegalArgumentException(msg);
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
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

	/**
	 * Get the available alert renderer resolvers.
	 * 
	 * @return the renderer resolvers
	 * @since 3.0
	 */
	public final @Nullable List<UserAlertRendererResolver> getRendererResolvers() {
		return rendererResolvers;
	}

	/**
	 * Set the available alert renderer resolvers.
	 * 
	 * @param rendererResolvers
	 *        the renderer resolvers to set
	 * @since 3.0
	 */
	public final void setRendererResolvers(@Nullable List<UserAlertRendererResolver> rendererResolvers) {
		this.rendererResolvers = rendererResolvers;
	}

	/**
	 * Get the optional message cache.
	 *
	 * @return the cache
	 * @since 3.0
	 */
	public final @Nullable Cache<String, VersionedMessages> getMessageCache() {
		return messageCache;
	}

	/**
	 * Set the optional message cache.
	 *
	 * @param messageCache
	 *        the cache to set
	 * @since 3.0
	 */
	public final void setMessageCache(@Nullable Cache<String, VersionedMessages> messageCache) {
		this.messageCache = messageCache;
	}

}
