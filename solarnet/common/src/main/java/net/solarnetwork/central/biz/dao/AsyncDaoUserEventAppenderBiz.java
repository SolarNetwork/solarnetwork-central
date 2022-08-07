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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.uuid.UUIDComparator;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UuidGenerator;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.SolarFluxPublisher;
import net.solarnetwork.central.support.TimeBasedV7UuidGenerator;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatCounter;
import net.solarnetwork.util.StatCounter.Stat;

/**
 * Asynchronous {@link UserEventAppenderBiz}.
 * 
 * @author matt
 * @version 1.1
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
	public static Comparator<UserEvent> EVENT_SORT = new Comparator<UserEvent>() {

		@Override
		public int compare(UserEvent o1, UserEvent o2) {
			int comparison = UUIDComparator.staticCompare(o1.getEventId(), o2.getEventId());
			if ( comparison != 0 ) {
				return comparison;
			}
			return o1.getUserId().compareTo(o2.getUserId());
		}
	};

	/**
	 * A function to generate a SolarFlux MQTT topic from a user event.
	 * 
	 * @since 1.1
	 */
	public static Function<UserEvent, String> SOLARFLUX_TOPIC_FN = (event) -> {
		return "user/" + event.getUserId() + "/event";
	};

	/**
	 * Enumeration of user event statistic count types.
	 */
	public static enum UserEventStats implements Stat {

		/** The count of user events added. */
		EventsAdded(0, "events added"),

		/** The count of user events persisted. */
		EventsStored(1, "events stored"),

		;

		private final int index;
		private final String description;

		private UserEventStats(int index, String description) {
			this.index = index;
			this.description = description;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}

	private final ExecutorService executorService;
	private final UserEventAppenderDao dao;
	private final StatCounter stats;
	private final BlockingQueue<UserEvent> queue;
	private final UuidGenerator uuidGenerator;
	private SolarFluxPublisher<UserEvent> solarFluxPublisher;
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
				new StatCounter("AsyncDaoUserEventAppender",
						"net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz", log, 500,
						UserEventStats.values()),
				TimeBasedV7UuidGenerator.INSTANCE);
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
			BlockingQueue<UserEvent> queue, StatCounter stats, UuidGenerator uuidGenerator) {
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
		stats.incrementAndGet(UserEventStats.EventsAdded);
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
				stats.incrementAndGet(UserEventStats.EventsStored);
			} catch ( RuntimeException e ) {
				log.error("Unable to add event {} to DAO: {}", event, e.getMessage(), e);
			}
			final SolarFluxPublisher<UserEvent> flux = getSolarFluxPublisher();
			if ( flux != null ) {
				flux.apply(event);
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
		long addCount = stats.get(UserEventStats.EventsAdded);
		long removeCount = stats.get(UserEventStats.EventsStored);
		long lagDiff = addCount - removeCount;
		Map<String, Number> statMap = new LinkedHashMap<>(UserEventStats.values().length);
		for ( UserEventStats s : UserEventStats.values() ) {
			statMap.put(s.toString(), stats.get(s));
		}
		if ( lagDiff > queueLagAlertThreshold ) {
			return new PingTestResult(false,
					String.format("Queue removal lag %d > %d", lagDiff, queueLagAlertThreshold),
					statMap);
		}
		return new PingTestResult(true, String.format("Processed %d events; lag %d.", addCount, lagDiff),
				statMap);
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
	public SolarFluxPublisher<UserEvent> getSolarFluxPublisher() {
		return solarFluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 * 
	 * @param solarFluxPublisher
	 *        the publisher to set
	 */
	public void setSolarFluxPublisher(SolarFluxPublisher<UserEvent> solarFluxPublisher) {
		this.solarFluxPublisher = solarFluxPublisher;
	}

}
