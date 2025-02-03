/* ==================================================================
 * DelayedOccasionalProcessor.java - 3/07/2024 11:09:30 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.biz.AsyncProcessor;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * Asynchronously process occasionally-appearing items.
 *
 * <p>
 * The goal of this class is to help asynchronously process "bursty" items that
 * come into being inconsistently, and sometimes in a duplicate manner, after a
 * brief delay. The delay allows de-duplication to occur within the delay
 * period.
 * </p>
 *
 * <p>
 * Different processing styles can be achieved via different {@link Queue}
 * implementations, such as {@link LinkedHashSetBlockingQueue} for
 * de-duplication or {@link DelayQueueSet} for consistently delayed
 * de-duplication.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public abstract class DelayedOccasionalProcessor<T>
		implements AsyncProcessor<T>, Runnable, ServiceLifecycleObserver, PingTest {

	/** The {@code delay} property default value. */
	public static final Duration DEFAULT_DELAY = Duration.ofSeconds(2);

	/** The {@code queueSizeAlertThreshold} default value. */
	public static final int DEFAULT_QUEUE_SIZE_ALERT_THRESHOLD = 500;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** The clock to use. */
	protected final Clock clock;

	/** The statistics tracker. */
	protected final StatTracker stats;

	private final TaskScheduler scheduler;
	private final Queue<T> items;
	private Duration delay = DEFAULT_DELAY;
	private int queueSizeAlertThreshold = DEFAULT_QUEUE_SIZE_ALERT_THRESHOLD;

	private final Lock flushLock;
	private ScheduledFuture<?> flushTask;

	/**
	 * Processor statistics.
	 */
	public static enum Stats {

		/** Count of items added. */
		ItemsAdded,

		/** Count of items removed. */
		ItemsRemoved,

		/** Count of items processed. */
		ItemsProcessed,

		/** Count of items that failed processing. */
		ItemsFailed,

		/** Current queue size. */
		QueueSize,

		/** The number of batches processed. */
		Batches,
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @param stats
	 *        the statistic tracker
	 * @param scheduler
	 *        the scheduler
	 * @param items
	 *        the item buffer; this must support concurrent access
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DelayedOccasionalProcessor(Clock clock, StatTracker stats, TaskScheduler scheduler,
			Queue<T> items) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.stats = requireNonNullArgument(stats, "stats");
		this.scheduler = requireNonNullArgument(scheduler, "scheduler");
		this.items = requireNonNullArgument(items, "statuses");
		this.flushLock = new ReentrantLock();
	}

	@Override
	public void serviceDidStartup() {
		// nothing
	}

	@Override
	public void serviceDidShutdown() {
		flushLock.lock();
		try {
			if ( flushTask != null && flushTask.isDone() ) {
				flushTask.cancel(true);
			}
			T item;
			while ( (item = items.poll()) != null ) {
				stats.increment(Stats.ItemsRemoved);
				try {
					processItemInternal(item);
					stats.increment(Stats.ItemsProcessed);
				} catch ( Exception e ) {
					stats.increment(Stats.ItemsFailed);
					log.error("Error processing delayed item [{}]: {}", item, e.getMessage(), e);
				}
			}
		} finally {
			flushLock.unlock();
		}
	}

	@Override
	public void asyncProcessItem(T item) {
		items.add(item);
		stats.increment(Stats.ItemsAdded);
		flushLock.lock();
		try {
			if ( flushTask == null || flushTask.isDone() ) {
				scheduleFlushTask();
			}
		} finally {
			flushLock.unlock();
		}
	}

	@Override
	public boolean cancelAsyncProcessItem(T item) {
		boolean result;
		flushLock.lock();
		try {
			result = items.remove(item);
		} finally {
			flushLock.unlock();
		}
		if ( result ) {
			stats.increment(Stats.ItemsRemoved);
		}
		return result;
	}

	/**
	 * Process a delayed item.
	 *
	 * @param item
	 *        the item to process
	 */
	protected abstract void processItemInternal(T item);

	@Override
	public final void run() {
		stats.increment(Stats.Batches);
		try {
			T item;
			while ( (item = items.poll()) != null ) {
				stats.increment(Stats.ItemsRemoved);
				try {
					processItemInternal(item);
					stats.increment(Stats.ItemsProcessed);
				} catch ( Exception e ) {
					log.error("Error processing delayed item [{}]: {}", item, e.getMessage(), e);
				}
			}
		} finally {
			flushLock.lock();
			try {
				if ( items.isEmpty() ) {
					flushTask = null;
				} else {
					scheduleFlushTask();
				}
			} finally {
				flushLock.unlock();
			}
		}
	}

	private void scheduleFlushTask() {
		final Duration delay = getDelay();
		flushTask = scheduler.schedule(this, clock.instant().plus(delay));
	}

	@Override
	public String getPingTestId() {
		return stats.getUid() != null ? stats.getUid() : getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return stats.getDisplayName();
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000L;
	}

	@Override
	public Result performPingTest() throws Exception {
		final int size = items.size();
		final int threshold = queueSizeAlertThreshold;
		Map<String, Long> statMap = stats.allCounts();
		statMap.put(Stats.QueueSize.name(), (long) size);
		return new PingTestResult(size <= threshold,
				size > threshold ? "Queue size %d is over %d.".formatted(size, threshold) : null,
				statMap);
	}

	/**
	 * Get the delay.
	 *
	 * @return the delay; defaults to {@link #DEFAULT_DELAY}
	 */
	public final Duration getDelay() {
		return delay;
	}

	/**
	 * Set the delay.
	 *
	 * @param delay
	 *        the delay to set
	 */
	public final void setDelay(Duration delay) {
		this.delay = delay;
	}

	/**
	 * Set the queue size alert threshold.
	 *
	 * @return the queue size threshold after which the ping test should fail;
	 *         defaults to {@link #DEFAULT_QUEUE_SIZE_ALERT_THRESHOLD}
	 */
	public final int getQueueSizeAlertThreshold() {
		return queueSizeAlertThreshold;
	}

	/**
	 * Set the queue size alert threshold.
	 *
	 * @param queueSizeAlertThreshold
	 *        the queue size threshold after which the ping test should fail
	 */
	public final void setQueueSizeAlertThreshold(int queueSizeAlertThreshold) {
		this.queueSizeAlertThreshold = queueSizeAlertThreshold;
	}

}
