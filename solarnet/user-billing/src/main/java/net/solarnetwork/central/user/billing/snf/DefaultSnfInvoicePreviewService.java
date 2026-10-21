/* ==================================================================
 * DefaultSnfInvoicePreviewService.java - 22/10/2026 2:45:27 pm
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

package net.solarnetwork.central.user.billing.snf;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewState;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreview;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicePreviewKey;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions;

/**
 * Default implementation of {@link SnfInvoicePreviewService}.
 *
 * <p>
 * Previews are generated as dry-run invoices of
 * {@link SnfInvoicingSystem#generateInvoice(Long, LocalDate, LocalDate, SnfInvoicingSystem.InvoiceGenerationOptions)},
 * by tasks run on an executor. At most {@code maxConcurrent} previews are
 * generated at once: when no permit is available, nothing is started and a
 * {@code Waiting} preview is returned, so the caller's next request tries
 * again. Finished previews are kept in a cache. A failed preview is kept for
 * {@code failureRetryDelay}, after which a request starts it again.
 * </p>
 *
 * <p>
 * The accounts of a group are generated one at a time. When one finishes, the
 * next one is started right away, as long as the group was asked for within
 * {@code groupPollTimeout}; otherwise the next request for the group starts
 * it. So a group keeps being generated while someone is waiting for it, and
 * stops soon after they leave.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DefaultSnfInvoicePreviewService implements SnfInvoicePreviewService {

	/** The {@code maxConcurrent} default value. */
	public static final int DEFAULT_MAX_CONCURRENT = 2;

	/** The {@code failureRetryDelay} property default value. */
	public static final Duration DEFAULT_FAILURE_RETRY_DELAY = Duration.ofSeconds(60);

	/** The {@code groupPollTimeout} property default value. */
	public static final Duration DEFAULT_GROUP_POLL_TIMEOUT = Duration.ofSeconds(30);

	private static final Logger log = LoggerFactory.getLogger(DefaultSnfInvoicePreviewService.class);

	private final InstantSource clock;
	private final SnfInvoicingSystem invoicingSystem;
	private final Executor executor;
	private final Cache<SnfInvoicePreviewKey, SnfInvoicePreview> cache;
	private final Semaphore permits;
	private final ConcurrentMap<SnfInvoicePreviewKey, Instant> running = new ConcurrentHashMap<>(8,
			0.9f, 2);
	private final ConcurrentMap<SnfInvoicePreviewKey, Instant> groupPolls = new ConcurrentHashMap<>(8,
			0.9f, 2);
	private @Nullable TransactionOperations transactionTemplate;
	private Duration failureRetryDelay = DEFAULT_FAILURE_RETRY_DELAY;
	private Duration groupPollTimeout = DEFAULT_GROUP_POLL_TIMEOUT;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @param invoicingSystem
	 *        the invoicing system to generate previews with
	 * @param executor
	 *        the executor to generate previews on
	 * @param cache
	 *        the cache of finished previews
	 * @param maxConcurrent
	 *        the maximum number of previews to generate at once
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}, or {@code maxConcurrent} is
	 *         less than {@code 1}
	 */
	public DefaultSnfInvoicePreviewService(InstantSource clock, SnfInvoicingSystem invoicingSystem,
			Executor executor, Cache<SnfInvoicePreviewKey, SnfInvoicePreview> cache,
			int maxConcurrent) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.executor = requireNonNullArgument(executor, "executor");
		this.cache = requireNonNullArgument(cache, "cache");
		if ( maxConcurrent < 1 ) {
			throw new IllegalArgumentException("The maxConcurrent argument must be at least 1.");
		}
		this.permits = new Semaphore(maxConcurrent);
		if ( executor instanceof ThreadPoolExecutor pool && maxConcurrent >= pool.getCorePoolSize() ) {
			log.warn(
					"Preview invoice maximum concurrency {} is not less than the executor core pool size {}: "
							+ "previews may delay other tasks on the executor.",
					maxConcurrent, pool.getCorePoolSize());
		}
	}

	@Override
	public SnfInvoicePreview accountPreview(SnfInvoicePreviewKey key) {
		final SnfInvoicePreview preview = currentPreview(requireNonNullArgument(key, "key"));
		return (preview != null ? preview : start(key, null));
	}

	@Override
	public List<SnfInvoicePreview> groupPreview(InvoiceConsolidationGroup group,
			SnfInvoicePreviewKey key) {
		requireNonNullArgument(group, "group");
		requireNonNullArgument(key, "key");
		final Instant now = clock.instant();
		groupPolls.put(key, now);
		groupPolls.values().removeIf(polled -> isGroupPollExpired(polled, now));
		return groupPreviews(group, key);
	}

	private boolean isGroupPollExpired(Instant polled, Instant now) {
		return now.isAfter(polled.plus(groupPollTimeout));
	}

	/**
	 * Start the next account of a group after one finishes, if the group is
	 * still being asked for.
	 */
	private void continueGroup(InvoiceConsolidationGroup group, SnfInvoicePreviewKey key) {
		final Instant polled = groupPolls.get(key);
		if ( polled == null || isGroupPollExpired(polled, clock.instant()) ) {
			log.debug("Not continuing preview invoice group {}: no longer polled.", key);
			return;
		}
		groupPreviews(group, key);
	}

	private List<SnfInvoicePreview> groupPreviews(InvoiceConsolidationGroup group,
			SnfInvoicePreviewKey key) {
		final List<Long> userIds = group.userIds();
		final List<SnfInvoicePreviewKey> keys = new ArrayList<>(userIds.size());
		final List<@Nullable SnfInvoicePreview> previews = new ArrayList<>(userIds.size());
		boolean generating = false;
		int next = -1;
		for ( Long userId : userIds ) {
			final SnfInvoicePreviewKey memberKey = new SnfInvoicePreviewKey(userId, key.month(),
					key.useAccountCredit());
			final SnfInvoicePreview preview = currentPreview(memberKey);
			if ( preview == null ) {
				if ( next < 0 ) {
					next = keys.size();
				}
			} else if ( preview.state() == InvoicePreviewState.Running ) {
				generating = true;
			}
			keys.add(memberKey);
			previews.add(preview);
		}
		if ( !generating && next >= 0 ) {
			previews.set(next, start(keys.get(next), () -> continueGroup(group, key)));
		}
		final Instant now = clock.instant();
		final List<SnfInvoicePreview> result = new ArrayList<>(previews.size());
		for ( SnfInvoicePreview preview : previews ) {
			result.add(preview != null ? preview : SnfInvoicePreview.waiting(now));
		}
		return result;
	}

	@Override
	public @Nullable SnfInvoicePreview completedAccountPreview(SnfInvoicePreviewKey key) {
		final SnfInvoicePreview cached = cache.get(requireNonNullArgument(key, "key"));
		return (cached != null && cached.isDone() ? cached : null);
	}

	/**
	 * Get the cached or running preview of an account.
	 *
	 * @param key
	 *        the key
	 * @return the preview, or {@code null} if the preview must be started
	 */
	private @Nullable SnfInvoicePreview currentPreview(SnfInvoicePreviewKey key) {
		final SnfInvoicePreview cached = cachedPreview(key);
		if ( cached != null ) {
			return cached;
		}
		final Instant started = running.get(key);
		return (started != null ? SnfInvoicePreview.running(started) : null);
	}

	private @Nullable SnfInvoicePreview cachedPreview(SnfInvoicePreviewKey key) {
		final SnfInvoicePreview cached = cache.get(key);
		if ( cached != null && cached.state() == InvoicePreviewState.Failed
				&& !clock.instant().isBefore(cached.date().plus(failureRetryDelay)) ) {
			// time to try again
			cache.remove(key, cached);
			return null;
		}
		return cached;
	}

	/**
	 * Start generating a preview, if a permit is available.
	 *
	 * @param key
	 *        the key
	 * @param then
	 *        an optional task to run after the preview is generated and its
	 *        permit released
	 * @return the preview
	 */
	private SnfInvoicePreview start(SnfInvoicePreviewKey key, @Nullable Runnable then) {
		if ( !permits.tryAcquire() ) {
			log.debug("Preview invoice {} waiting: no permit available.", key);
			return SnfInvoicePreview.waiting(clock.instant());
		}
		boolean submitted = false;
		try {
			final Instant now = clock.instant();
			final Instant started = running.putIfAbsent(key, now);
			if ( started != null ) {
				return SnfInvoicePreview.running(started);
			}

			// a task may have finished after the cache was checked
			final SnfInvoicePreview cached = cachedPreview(key);
			if ( cached != null ) {
				running.remove(key, now);
				return cached;
			}

			try {
				executor.execute(() -> generate(key, now, then));
			} catch ( RejectedExecutionException e ) {
				running.remove(key, now);
				log.warn("Preview invoice {} rejected by executor: {}", key, e.getMessage());
				return SnfInvoicePreview.waiting(now);
			}
			submitted = true;
			log.debug("Started preview invoice {}", key);
			return SnfInvoicePreview.running(now);
		} finally {
			if ( !submitted ) {
				permits.release();
			}
		}
	}

	private void generate(SnfInvoicePreviewKey key, Instant started, @Nullable Runnable then) {
		boolean finished = false;
		try {
			SnfInvoicePreview result;
			try {
				result = SnfInvoicePreview.done(clock.instant(), generateInvoice(key));
				log.debug("Finished preview invoice {} in {}ms", key,
						Duration.between(started, result.date()).toMillis());
			} catch ( RuntimeException e ) {
				if ( Thread.currentThread().isInterrupted() ) {
					log.info("Preview invoice {} interrupted.", key);
					return;
				}
				log.warn("Error generating preview invoice {}: {}", key, e.toString(), e);
				result = SnfInvoicePreview.failed(clock.instant());
			}
			cache.put(key, result);
			finished = true;
		} finally {
			running.remove(key, started);
			permits.release();
		}
		if ( finished && then != null ) {
			try {
				then.run();
			} catch ( RuntimeException e ) {
				log.warn("Error continuing after preview invoice {}: {}", key, e.toString(), e);
			}
		}
	}

	private @Nullable SnfInvoice generateInvoice(SnfInvoicePreviewKey key) {
		final LocalDate startDate = key.month().atDay(1);
		final LocalDate endDate = key.month().plusMonths(1).atDay(1);
		final var options = new SnfInvoicingOptions(true, key.useAccountCredit());
		final TransactionOperations tx = this.transactionTemplate;
		if ( tx != null ) {
			return tx.execute(_ -> invoicingSystem.generateInvoice(key.userId(), startDate, endDate,
					options));
		}
		return invoicingSystem.generateInvoice(key.userId(), startDate, endDate, options);
	}

	/**
	 * Get the transaction template.
	 *
	 * @return the template, or {@code null} to generate previews without one
	 */
	public final @Nullable TransactionOperations getTransactionTemplate() {
		return transactionTemplate;
	}

	/**
	 * Set the transaction template.
	 *
	 * @param transactionTemplate
	 *        the template to generate each preview in, such as a read-only
	 *        transaction, or {@code null} to generate previews without one
	 */
	public final void setTransactionTemplate(@Nullable TransactionOperations transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Get the failure retry delay.
	 *
	 * @return the delay after which a failed preview is started again;
	 *         defaults to {@link #DEFAULT_FAILURE_RETRY_DELAY}
	 */
	public final Duration getFailureRetryDelay() {
		return failureRetryDelay;
	}

	/**
	 * Set the failure retry delay.
	 *
	 * @param failureRetryDelay
	 *        the delay after which a failed preview is started again; if
	 *        {@code null} then {@link #DEFAULT_FAILURE_RETRY_DELAY} is used
	 */
	public final void setFailureRetryDelay(@Nullable Duration failureRetryDelay) {
		this.failureRetryDelay = (failureRetryDelay != null ? failureRetryDelay
				: DEFAULT_FAILURE_RETRY_DELAY);
	}

	/**
	 * Get the group poll timeout.
	 *
	 * @return the time after a group is last asked for during which its next
	 *         account is started as soon as one finishes; defaults to
	 *         {@link #DEFAULT_GROUP_POLL_TIMEOUT}
	 */
	public final Duration getGroupPollTimeout() {
		return groupPollTimeout;
	}

	/**
	 * Set the group poll timeout.
	 *
	 * @param groupPollTimeout
	 *        the time after a group is last asked for during which its next
	 *        account is started as soon as one finishes; if {@code null} then
	 *        {@link #DEFAULT_GROUP_POLL_TIMEOUT} is used
	 */
	public final void setGroupPollTimeout(@Nullable Duration groupPollTimeout) {
		this.groupPollTimeout = (groupPollTimeout != null ? groupPollTimeout
				: DEFAULT_GROUP_POLL_TIMEOUT);
	}

}
