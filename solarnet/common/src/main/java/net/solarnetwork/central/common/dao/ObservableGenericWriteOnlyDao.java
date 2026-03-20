/* ==================================================================
 * ObservableGenericWriteOnlyDao.java - 20/03/2026 11:01:17 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.service.RemoteServiceException;

/**
 * A {@link GenericWriteOnlyDao} that allows observing the persisted entities.
 * 
 * @author matt
 * @version 1.0
 */
public class ObservableGenericWriteOnlyDao<T, K> implements GenericWriteOnlyDao<T, K> {

	/** The {@code observerTimeout} property default value. */
	public static final Duration DEFAULT_OBSERVER_TIMEOUT = Duration.ofMillis(200);

	private static final Logger log = LoggerFactory.getLogger(ObservableGenericWriteOnlyDao.class);

	private final GenericWriteOnlyDao<T, K> delegate;
	private final @Nullable Function<T, Future<?>> observer;
	private Duration observerTimeout = DEFAULT_OBSERVER_TIMEOUT;

	// cache this because toMillis() is relatively slow
	private long observerTimeoutMs = DEFAULT_OBSERVER_TIMEOUT.toMillis();

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the DAO to persist events to
	 * @param observer
	 *        the optional observer to use
	 * @throws IllegalArgumentException
	 *         if any argument except {@code observer} is {@code null}
	 */
	public ObservableGenericWriteOnlyDao(GenericWriteOnlyDao<T, K> delegate,
			@Nullable Function<T, Future<?>> observer) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.observer = observer;
	}

	@Override
	public @Nullable K persist(T entity) {
		try {
			return delegate.persist(entity);
		} finally {
			if ( observer != null ) {
				try {
					Future<?> f = observer.apply(entity);
					if ( observerTimeoutMs > 0 ) {
						f.get(observerTimeoutMs, TimeUnit.MINUTES);
					}
				} catch ( TimeoutException | InterruptedException e ) {
					// move on
				} catch ( Exception e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IllegalArgumentException
							|| root instanceof RemoteServiceException ) {
						log.warn("Unable to publish {} to observer: {}", entity, root.getMessage());
					} else {
						log.warn("Error publishing {} to observer: {}", entity, root, root);
					}
				}
			}
		}
	}

	/**
	 * Get the observer timeout.
	 * 
	 * @return the timeout; defaults to {@link #DEFAULT_OBSERVER_TIMEOUT}
	 */
	public final Duration getObserverTimeout() {
		return observerTimeout;
	}

	/**
	 * Set the observer timeout.
	 * 
	 * @param observerTimeout
	 *        the timeout to set; if {@code null} or negative then
	 *        {@link #DEFAULT_OBSERVER_TIMEOUT} will be used
	 */
	public final void setObserverTimeout(Duration observerTimeout) {
		var dur = (observerTimeout != null && !observerTimeout.isNegative() ? observerTimeout
				: DEFAULT_OBSERVER_TIMEOUT);
		this.observerTimeout = dur;
		this.observerTimeoutMs = dur.toMillis();
	}

}
