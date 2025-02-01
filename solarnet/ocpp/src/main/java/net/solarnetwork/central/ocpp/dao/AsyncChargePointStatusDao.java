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
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.DelayQueueSet;
import net.solarnetwork.central.support.DelayedOcassionalProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StatTracker;

/**
 * Asynchronous implementation of {@link ChargePointStatusDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class AsyncChargePointStatusDao
		extends DelayedOcassionalProcessor<AsyncChargePointStatusDao.StatusUpdate>
		implements ChargePointStatusDao, Runnable, ServiceLifecycleObserver {

	/** The {@code flushDelay} property default value. */
	public static final Duration DEFAULT_FLUSH_DELAY = Duration.ofSeconds(2);

	private static final Logger log = LoggerFactory.getLogger(AsyncChargePointStatusDao.class);

	private final ChargePointStatusDao delegate;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @param delegate
	 *        the delegate
	 */
	public AsyncChargePointStatusDao(TaskScheduler scheduler, ChargePointStatusDao delegate) {
		this(Clock.systemUTC(), scheduler, delegate, new DelayQueueSet<>());
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
			Queue<StatusUpdate> statuses) {
		super(clock, new StatTracker("AsyncChargePointStatus", null, log, 500), scheduler, statuses);
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public FilterResults<ChargePointStatus, UserLongCompositePK> findFiltered(
			ChargePointStatusFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public void findFilteredStream(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor, List<SortDescriptor> sortDescriptors,
			Long offset, Integer max) throws IOException {
		delegate.findFilteredStream(filter, processor, sortDescriptors, offset, max);
	}

	@Override
	public void updateConnectionStatus(Long userId, String chargePointIdentifier, String connectedTo,
			String sessionId, Instant connectionDate, boolean connected) {
		asyncProcessItem(new StatusUpdate(clock.instant(), userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected));
	}

	@Override
	protected void processItemInternal(StatusUpdate item) {
		delegate.updateConnectionStatus(item.userId, item.chargePointIdentifier, item.connectedTo,
				item.sessionId, item.connectionDate, item.connected);
	}

	/**
	 * Create a new status update instance.
	 * 
	 * <p>
	 * This method is targeted for unit tests.
	 * </p>
	 * 
	 * @param ts
	 *        the timestamp
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param connectedTo
	 *        the connected to
	 * @param sessionId
	 *        the session ID
	 * @param connectionDate
	 *        the connection date
	 * @param connected
	 *        the connected flag
	 * @return the new instance
	 */
	public StatusUpdate updateFor(Instant ts, Long userId, String chargePointIdentifier,
			String connectedTo, String sessionId, Instant connectionDate, boolean connected) {
		return new StatusUpdate(ts, userId, chargePointIdentifier, connectedTo, sessionId,
				connectionDate, connected);
	}

	/**
	 * A delayed status update.
	 */
	public final class StatusUpdate implements Delayed {

		private final Instant ready;
		private final Long userId;
		private final String chargePointIdentifier;
		private final String connectedTo;
		private final String sessionId;
		private final Instant connectionDate;
		private final boolean connected;

		private StatusUpdate(Instant ts, Long userId, String chargePointIdentifier, String connectedTo,
				String sessionId, Instant connectionDate, boolean connected) {
			super();
			this.ready = ts.plus(AsyncChargePointStatusDao.this.getDelay());
			this.userId = userId;
			this.chargePointIdentifier = chargePointIdentifier;
			this.connectedTo = connectedTo;
			this.sessionId = sessionId;
			this.connectionDate = connectionDate;
			this.connected = connected;
		}

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
		public int compareTo(Delayed o) {
			// not bothering to check instanceof for performance
			StatusUpdate other = (StatusUpdate) o;
			int result = ready.compareTo(other.ready);
			if ( result == 0 ) {
				result = userId.compareTo(other.userId);
				if ( result == 0 ) {
					result = chargePointIdentifier.compareTo(other.chargePointIdentifier);
				}
			}
			return result;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return clock.instant().until(ready, unit.toChronoUnit());
		}

		/**
		 * Get the user ID.
		 * 
		 * @return the user ID
		 */
		public Long getUserId() {
			return userId;
		}

		/**
		 * Get the charge point identifier.
		 * 
		 * @return the charge point identifier
		 */
		public String getChargePointIdentifier() {
			return chargePointIdentifier;
		}

		/**
		 * Get the connected to.
		 * 
		 * @return the connected to
		 */
		public String getConnectedTo() {
			return connectedTo;
		}

		/**
		 * Get the session ID.
		 * 
		 * @return the session ID
		 */
		public String getSessionId() {
			return sessionId;
		}

		/**
		 * Get the connection date.
		 * 
		 * @return the connection date
		 */
		public Instant getConnectionDate() {
			return connectionDate;
		}

		/**
		 * Get the connected flag.
		 * 
		 * @return the connected flag
		 */
		public boolean isConnected() {
			return connected;
		}

	}

}
