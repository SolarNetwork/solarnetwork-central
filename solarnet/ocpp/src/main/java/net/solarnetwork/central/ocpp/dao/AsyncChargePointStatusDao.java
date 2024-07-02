/* ==================================================================
 * AsyncChargePointStatusDao.java - 2/07/2024 1:11:43 pm
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

package net.solarnetwork.central.ocpp.dao;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.ObjectUtils;

/**
 * Asynchronous implementation of {@link ChargePointStatusDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class AsyncChargePointStatusDao
		implements ChargePointStatusDao, Runnable, ServiceLifecycleObserver {

	/**
	 * A status update record.
	 */
	public static final record StatusUpdate(Instant ts, Long userId, String chargePointIdentifier,
			String connectedTo, String sessionId, Instant connectionDate, boolean connected)
			implements Comparable<StatusUpdate> {

		@Override
		public boolean equals(Object obj) {
			if ( obj instanceof StatusUpdate o ) {
				return Objects.equals(userId, o.userId)
						&& Objects.equals(chargePointIdentifier, o.chargePointIdentifier);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, chargePointIdentifier);
		}

		@Override
		public int compareTo(StatusUpdate o) {
			int result = userId.compareTo(o.userId);
			if ( result == 0 ) {
				result = chargePointIdentifier.compareTo(o.chargePointIdentifier);
			}
			return result;
		}

	}

	/** The {@code flushDelay} property default value. */
	public static final Duration DEFAULT_FLUSH_DELAY = Duration.ofSeconds(2);

	private static final Logger log = LoggerFactory.getLogger(AsyncChargePointStatusDao.class);

	private final Clock clock;
	private final TaskScheduler scheduler;
	private final ChargePointStatusDao delegate;
	private final NavigableSet<StatusUpdate> statuses;
	private Duration flushDelay = DEFAULT_FLUSH_DELAY;

	private final Lock flushLock;
	private ScheduledFuture<?> flushTask;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param delegate
	 *        the delegate
	 */
	public AsyncChargePointStatusDao(TaskScheduler scheduler, ChargePointStatusDao delegate) {
		this(Clock.systemUTC(), scheduler, delegate, new ConcurrentSkipListSet<>());
	}

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param delegate
	 *        the delegate
	 * @param statuses
	 *        the status buffer
	 */
	public AsyncChargePointStatusDao(Clock clock, TaskScheduler scheduler, ChargePointStatusDao delegate,
			NavigableSet<StatusUpdate> statuses) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		this.scheduler = ObjectUtils.requireNonNullArgument(scheduler, "scheduler");
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		this.statuses = ObjectUtils.requireNonNullArgument(statuses, "statuses");
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
			for ( Iterator<StatusUpdate> itr = statuses.iterator(); itr.hasNext(); ) {
				StatusUpdate update = itr.next();
				delegate.updateConnectionStatus(update.userId, update.chargePointIdentifier,
						update.connectedTo, update.sessionId, update.connectionDate, update.connected);
				itr.remove();
			}
		} catch ( Exception e ) {
			log.error("Error flushing OCPP charge point statuses: {}", e.getMessage(), e);
		} finally {
			flushLock.unlock();
		}
	}

	@Override
	public FilterResults<ChargePointStatus, UserLongCompositePK> findFiltered(
			ChargePointStatusFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public void findFilteredStream(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) throws IOException {
		delegate.findFilteredStream(filter, processor, sortDescriptors, offset, max);
	}

	@Override
	public void updateConnectionStatus(Long userId, String chargePointIdentifier, String connectedTo,
			String sessionId, Instant connectionDate, boolean connected) {
		statuses.add(new StatusUpdate(clock.instant(), userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected));
		flushLock.lock();
		try {
			if ( flushTask == null || flushTask.isDone() ) {
				scheduleFlushTask();
			}
		} finally {
			flushLock.unlock();
		}
	}

	private void scheduleFlushTask() {
		final Duration delay = getFlushDelay();
		flushTask = scheduler.schedule(this, clock.instant().plus(delay));
	}

	@Override
	public void run() {
		final Duration delay = getFlushDelay();
		try {
			for ( Iterator<StatusUpdate> itr = statuses.iterator(); itr.hasNext(); ) {
				StatusUpdate update = itr.next();
				if ( update.ts.compareTo(clock.instant().minus(delay)) < 1 ) {
					delegate.updateConnectionStatus(update.userId, update.chargePointIdentifier,
							update.connectedTo, update.sessionId, update.connectionDate,
							update.connected);
					itr.remove();
				}
			}
		} catch ( Exception e ) {
			log.error("Error flushing OCPP charge point status: {}", e.getMessage(), e);
		} finally {
			flushLock.lock();
			try {
				if ( !statuses.isEmpty() ) {
					scheduleFlushTask();
				}
			} finally {
				flushLock.unlock();
			}
		}
	}

	/**
	 * Get the flush delay.
	 * 
	 * @return the flush delay
	 */
	public Duration getFlushDelay() {
		return flushDelay;
	}

	/**
	 * Set the flush delay.
	 * 
	 * @param flushDelay
	 *        the flush delay to set
	 */
	public void setFlushDelay(Duration flushDelay) {
		this.flushDelay = flushDelay;
	}

}
