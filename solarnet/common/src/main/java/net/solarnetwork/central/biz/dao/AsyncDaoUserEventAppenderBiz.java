/* ==================================================================
 * AsyncDaoUserEventAppenderBiz.java - 1/08/2022 3:26:54 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.uuid.UUIDComparator;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;

/**
 * Asynchronous {@link UserEventAppenderBiz}.
 *
 * @author matt
 * @version 1.5
 */
public class AsyncDaoUserEventAppenderBiz
		implements UserEventAppenderBiz, PingTest, ServiceLifecycleObserver, Runnable {

	/** The {@code queueLagAlertThreshold} property default value. */
	public final int DEFAULT_QUEUE_LAG_ALERT_THRESHOLD = 500;

	private static final Logger log = LoggerFactory.getLogger(AsyncDaoUserEventAppenderBiz.class);

	/**
	 * A comparator for {@link UserEvent} that sorts by event ID first, then
	 * user ID.
	 */
	public static Comparator<UserEvent> EVENT_SORT = (o1, o2) -> {
		int comparison = UUIDComparator.staticCompare(o1.getEventId(), o2.getEventId());
		if ( comparison != 0 ) {
			return comparison;
		}
		return o1.getUserId().compareTo(o2.getUserId());
	};

	/**
	 * A function to generate a SolarFlux MQTT topic from a user event.
	 *
	 * @since 1.1
	 */
	public static Function<UserEvent, String> SOLARFLUX_TOPIC_FN = (event) -> "user/" + event.getUserId()
			+ "/event";

	/**
	 * A function to generate a SolarFlux MQTT topic from a user event.
	 *
	 * @since 1.3
	 */
	public static Function<UserEvent, String> SOLARFLUX_TAGGED_TOPIC_FN = (event) -> {
		final StringBuilder buf = new StringBuilder("user/");
		buf.append(event.getUserId()).append("/event");

		final String[] tags = event.getTags();
		for ( String tag : tags ) {
			buf.append('/');
			buf.append(tag);
		}
		return buf.toString();
	};

	/**
	 * Enumeration of user event statistic count types.
	 */
	public static enum UserEventStats {

		/** The count of user events added. */
		EventsAdded,

		/** The count of user events persisted. */
		EventsStored,

		;

	}

	private final ExecutorService executorService;
	private final UserEventAppenderDao dao;
	private final StatTracker stats;
	private final BlockingQueue<UserEvent> queue;
	private final UuidGenerator uuidGenerator;
	private MqttJsonPublisher<UserEvent> solarFluxPublisher;
	private int queueLagAlertThreshold = DEFAULT_QUEUE_LAG_ALERT_THRESHOLD;

	/**
	 * Constructor.
	 *
	 * @param executorService
	 *        the executor service
	 * @param dao
	 *        the DAO
	 */
	public AsyncDaoUserEventAppenderBiz(ExecutorService executorService, UserEventAppenderDao dao) {
		this(executorService, dao, new PriorityBlockingQueue<>(64, EVENT_SORT),
				new StatTracker("AsyncDaoUserEventAppender", null, log, 500),
				TimeBasedV7UuidGenerator.INSTANCE_MICROS);
	}

	/**
	 * Constructor.
	 *
	 * @param executorService
	 *        the executor service
	 * @param dao
	 *        the DAO
	 * @param queue
	 *        the queue
	 * @param stats
	 *        the stats
	 * @param uuidGenerator
	 *        the UUID generator to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AsyncDaoUserEventAppenderBiz(ExecutorService executorService, UserEventAppenderDao dao,
			BlockingQueue<UserEvent> queue, StatTracker stats, UuidGenerator uuidGenerator) {
		super();
		this.executorService = requireNonNullArgument(executorService, "executorService");
		this.dao = requireNonNullArgument(dao, "datumDao");
		this.queue = requireNonNullArgument(queue, "queue");
		this.stats = requireNonNullArgument(stats, "stats");
		this.uuidGenerator = requireNonNullArgument(uuidGenerator, "uuidGenerator");

	}

	@Override
	public UserEvent addEvent(Long userId, LogEventInfo info) {
		UserEvent event = new UserEvent(userId, uuidGenerator.generate(),
				requireNonNullArgument(info, "info").getTags(), info.getMessage(), info.getData());
		queue.offer(event);
		stats.increment(UserEventStats.EventsAdded);
		try {
			executorService.execute(this);
		} catch ( RejectedExecutionException e ) {
			// assume shutting down; discard event
			log.warn("Discarding UserEvent {} because of RejectedExecutionException: {}", event,
					e.getMessage());
		}
		return event;
	}

	@Override
	public void run() {
		final UserEvent event = queue.poll();
		if ( event != null ) {
			try {
				dao.add(event);
				stats.increment(UserEventStats.EventsStored);
			} catch ( RuntimeException e ) {
				log.error("Unable to add event {} to DAO: {}", event, e.getMessage(), e);
			}
			final MqttJsonPublisher<UserEvent> flux = getSolarFluxPublisher();
			if ( flux != null ) {
				Future<?> f = flux.apply(event);
				try {
					f.get(1, TimeUnit.SECONDS);
				} catch ( TimeoutException | InterruptedException e ) {
					// move on
				} catch ( ExecutionException e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IllegalArgumentException iae ) {
						log.warn("Unable to publish UserEvent {} to SolarFlux: {}", event,
								iae.getMessage());
					} else {
						log.warn("Error publishing UserEvent {} to SolarFlux: {}", event,
								root.toString(), root);
					}
				}
			}
		}
	}

	@Override
	public void serviceDidStartup() {
		// nothing to do
	}

	@Override
	public void serviceDidShutdown() {
		if ( !executorService.isShutdown() ) {
			executorService.shutdown();
			try {
				executorService.awaitTermination(1, TimeUnit.MINUTES);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "Async DAO User Event Appender";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		// verify buffer queue not lagging behind additions
		final Map<String, Long> allStats = stats.allCounts();
		final Long addCount = allStats.get(UserEventStats.EventsAdded.name());
		final Long removeCount = allStats.get(UserEventStats.EventsStored.name());
		final long lagDiff = (addCount != null ? addCount : 0L)
				- (removeCount != null ? removeCount : 0L);
		if ( lagDiff > queueLagAlertThreshold ) {
			return new PingTestResult(false,
					String.format("Queue removal lag %d > %d", lagDiff, queueLagAlertThreshold),
					allStats);
		}
		return new PingTestResult(true,
				String.format("Processed %d events; lag %d.", addCount != null ? addCount : 0L, lagDiff),
				allStats);
	}

	/**
	 * Get the minimum queue lag before the ping test will fail.
	 *
	 * @return the threshold; defaults to
	 *         {@link #DEFAULT_QUEUE_LAG_ALERT_THRESHOLD}
	 */
	public int getQueueLagAlertThreshold() {
		return queueLagAlertThreshold;
	}

	/**
	 * Set the minimum queue lag before the ping test will fail.
	 *
	 * @param queueLagAlertThreshold
	 *        the threshold to set
	 */
	public void setQueueLagAlertThreshold(int queueLagAlertThreshold) {
		this.queueLagAlertThreshold = queueLagAlertThreshold;
	}

	/**
	 * Get the SolarFlux publisher.
	 *
	 * @return the publisher
	 */
	public MqttJsonPublisher<UserEvent> getSolarFluxPublisher() {
		return solarFluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 *
	 * @param solarFluxPublisher
	 *        the publisher to set
	 */
	public void setSolarFluxPublisher(MqttJsonPublisher<UserEvent> solarFluxPublisher) {
		this.solarFluxPublisher = solarFluxPublisher;
	}

}
