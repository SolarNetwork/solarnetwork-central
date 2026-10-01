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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.DelayQueueSet;
import net.solarnetwork.central.support.DelayedOccasionalProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StatTracker;

/**
 * Asynchronous implementation of {@link ChargePointStatusDao}.
 * 
 * <p>
 * Connection status updates are buffered per charger, and written to the
 * delegate DAO once the configured delay has passed since the first buffered
 * update for that charger. This limits the rate of database updates when
 * chargers connect and disconnect quickly, while keeping the stored status
 * eventually consistent.
 * </p>
 * 
 * <p>
 * Buffered updates are merged per connection session: a newer update for a
 * session replaces an older one, and a disconnection of a session whose
 * connection has not been written yet cancels both. The remaining updates are
 * written in the order they were made. The delegate only clears a status when
 * disconnecting the session it has stored, so a disconnection of an older
 * session can not overwrite a newer connection, whatever order the session
 * events were received in.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class AsyncChargePointStatusDao
		extends DelayedOccasionalProcessor<AsyncChargePointStatusDao.PendingCharger>
		implements ChargePointStatusDao, Runnable, ServiceLifecycleObserver {

	/** The {@code flushDelay} property default value. */
	public static final Duration DEFAULT_FLUSH_DELAY = Duration.ofSeconds(2);

	private static final Logger log = LoggerFactory.getLogger(AsyncChargePointStatusDao.class);

	private final ChargePointStatusDao delegate;
	private final ConcurrentMap<PendingCharger, Map<String, StatusUpdate>> pending;

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
	 * @param clock
	 *        the clock
	 * @param scheduler
	 *        the scheduler
	 * @param delegate
	 *        the delegate
	 * @param chargers
	 *        the queue of chargers with buffered updates; must not accept
	 *        duplicate elements
	 */
	public AsyncChargePointStatusDao(Clock clock, TaskScheduler scheduler, ChargePointStatusDao delegate,
			Queue<PendingCharger> chargers) {
		super(clock, new StatTracker("AsyncChargePointStatus", null, log, 500), scheduler, chargers);
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		this.pending = new ConcurrentHashMap<>(64, 0.9f, 2);
	}

	@Override
	public FilterResults<ChargePointStatus, UserLongCompositePK> findFiltered(
			ChargePointStatusFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public void findFilteredStream(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor,
			@Nullable List<SortDescriptor> sortDescriptors, @Nullable Long offset, @Nullable Integer max)
			throws IOException {
		delegate.findFilteredStream(filter, processor, sortDescriptors, offset, max);
	}

	@Override
	public void updateConnectionStatus(Long userId, String chargePointIdentifier, String connectedTo,
			String sessionId, Instant connectionDate, boolean connected) {
		final PendingCharger charger = new PendingCharger(clock.instant(), userId,
				chargePointIdentifier);
		final StatusUpdate update = new StatusUpdate(userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected);
		// buffer the update before queuing the charger, so a flush that removes the charger from
		// the queue after this point also sees this update
		pending.compute(charger, (_, updates) -> merge(updates, update));
		asyncProcessItem(charger);
	}

	private static @Nullable Map<String, StatusUpdate> merge(@Nullable Map<String, StatusUpdate> updates,
			StatusUpdate update) {
		final Map<String, StatusUpdate> result = (updates != null ? updates : new LinkedHashMap<>(2));
		final String sessionKey = update.sessionKey();
		final StatusUpdate prev = result.remove(sessionKey);
		if ( !update.connected && prev != null && prev.connected ) {
			// connection not written yet, so nothing to disconnect
			return (result.isEmpty() ? null : result);
		}
		result.put(sessionKey, update);
		return result;
	}

	@Override
	protected void processItemInternal(PendingCharger item) {
		final Map<String, StatusUpdate> updates = pending.remove(item);
		if ( updates == null ) {
			return;
		}
		for ( StatusUpdate update : updates.values() ) {
			try {
				delegate.updateConnectionStatus(update.userId, update.chargePointIdentifier,
						update.connectedTo, update.sessionId, update.connectionDate, update.connected);
			} catch ( RuntimeException e ) {
				log.error("Error updating charger {} connection status {}: {}",
						update.chargePointIdentifier, update, e.getMessage(), e);
			}
		}
	}

	/**
	 * Write all buffered updates, including those whose delay has not yet
	 * passed.
	 */
	@Override
	public void serviceDidShutdown() {
		super.serviceDidShutdown();
		for ( PendingCharger charger : pending.keySet() ) {
			processItemInternal(charger);
		}
	}

	/**
	 * A charger with buffered status updates.
	 * 
	 * <p>
	 * Chargers are equal when their user and charge point identifier are
	 * equal, regardless of when their updates are due.
	 * </p>
	 */
	public final class PendingCharger implements Delayed {

		private final Instant ready;
		private final Long userId;
		private final String chargePointIdentifier;

		private PendingCharger(Instant ts, Long userId, String chargePointIdentifier) {
			super();
			this.ready = ts.plus(AsyncChargePointStatusDao.this.getDelay());
			this.userId = userId;
			this.chargePointIdentifier = chargePointIdentifier;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if ( obj instanceof PendingCharger o ) {
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
			PendingCharger other = (PendingCharger) o;
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

		@Override
		public String toString() {
			return "PendingCharger{userId=" + userId + ", chargePointIdentifier="
					+ chargePointIdentifier + ", ready=" + ready + "}";
		}

	}

	/**
	 * A buffered connection status update.
	 */
	private record StatusUpdate(Long userId, String chargePointIdentifier, String connectedTo,
			String sessionId, Instant connectionDate, boolean connected) {

		private String sessionKey() {
			return connectedTo + '\n' + sessionId;
		}

	}

}
