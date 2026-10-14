/* ==================================================================
 * AccountTaskJob.java - 21/07/2020 10:57:46 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.jobs;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;

/**
 * Job to process {@link AccountTaskType} tasks.
 *
 * <p>
 * A task whose handler returns {@code false}, or throws an exception, is
 * deferred by {@code retryDelay}, so that it does not hold up the tasks behind
 * it and is attempted again on a later run.
 * </p>
 *
 * @author matt
 * @version 2.2
 */
public class AccountTaskJob extends JobSupport {

	/**
	 * The {@code retryDelay} property default value.
	 *
	 * @since 2.2
	 */
	public static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(5);

	private final @Nullable TransactionTemplate transactionTemplate;
	private final AccountTaskDao taskDao;
	private final AccountTaskHandler generateInvoiceTaskHandler;
	private final AccountTaskHandler deliverInvoiceTaskHandler;
	private @Nullable AccountTaskHandler syncInvoiceTaskHandler;
	private @Nullable AccountTaskHandler syncPaymentTaskHandler;
	private Duration retryDelay = DEFAULT_RETRY_DELAY;

	/**
	 * Constructor.
	 *
	 * @param transactionTemplate
	 *        the transaction template to use, or {@code null}
	 * @param taskDao
	 *        the task DAO
	 * @param generateInvoiceTaskHandler
	 *        the handler for {@link AccountTaskType#GenerateInvoice}
	 * @param deliverInvoiceTaskHandler
	 *        the handler for {@link AccountTaskType#DeliverInvoice}
	 * @throws IllegalArgumentException
	 *         if {@code taskDao} or {@code generateInvoiceTaskHandler} is
	 *         {@code null}
	 */
	public AccountTaskJob(@Nullable TransactionTemplate transactionTemplate, AccountTaskDao taskDao,
			AccountTaskHandler generateInvoiceTaskHandler,
			AccountTaskHandler deliverInvoiceTaskHandler) {
		super("Billing", "AccountTaskProcessor");
		this.transactionTemplate = transactionTemplate;
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.generateInvoiceTaskHandler = requireNonNullArgument(generateInvoiceTaskHandler,
				"generateInvoiceTaskHandler");
		this.deliverInvoiceTaskHandler = requireNonNullArgument(deliverInvoiceTaskHandler,
				"deliverInvoiceTaskHandler");
	}

	@Override
	public void run() {
		executeParallelJob("account task");
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterations) throws Exception {
		int processedCount = 0;
		boolean processed;
		do {
			final AtomicReference<AccountTask> claimed = new AtomicReference<>();
			try {
				if ( transactionTemplate != null ) {
					processed = transactionTemplate.execute(_ -> execute(claimed));
				} else {
					processed = execute(claimed);
				}
			} catch ( RuntimeException e ) {
				final AccountTask task = claimed.get();
				if ( task == null ) {
					// failed to claim a task, so give up for now
					throw e;
				}
				final Instant execAt = Instant.now().plus(retryDelay);
				if ( e instanceof RepeatableTaskException ) {
					log.debug(e.getMessage(), e);
					log.info("Error processing account task {}; will re-try after {}: {}",
							task.getId(), execAt, e.getMessage());
				} else {
					log.error("Error processing account task {}; will re-try after {}: {}", task,
							execAt, e.toString(), e);
				}
				// the task's transaction has rolled back, so defer in a new one
				if ( transactionTemplate != null ) {
					transactionTemplate.executeWithoutResult(_ -> defer(task, execAt));
				} else {
					defer(task, execAt);
				}
				processed = true;
			}
			if ( processed ) {
				remainingIterations.decrementAndGet();
				processedCount++;
			}
		} while ( processed && remainingIterations.get() > 0 );
		return processedCount;
	}

	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	private boolean execute(AtomicReference<AccountTask> claimed) {
		final AccountTask task = taskDao.claimAccountTask();
		if ( task == null ) {
			return false;
		}
		claimed.set(task);
		boolean retry = false;
		final AccountTaskType taskType = task.getTaskType();
		switch (taskType) {
			case GenerateInvoice:
				retry = !generateInvoiceTaskHandler.handleTask(task);
				break;

			case DeliverInvoice:
				retry = !deliverInvoiceTaskHandler.handleTask(task);
				break;

			case SyncInvoiceToAccounting:
				retry = !handleAccountingTask(task, syncInvoiceTaskHandler);
				break;

			case SyncPaymentToAccounting:
				retry = !handleAccountingTask(task, syncPaymentTaskHandler);
				break;

			default:
				// do nothing, but delete
				break;
		}
		if ( retry ) {
			final Instant execAt = Instant.now().plus(retryDelay);
			log.info("Account task {} will be re-tried after {}", task.getId(), execAt);
			defer(task, execAt);
		} else {
			taskDao.delete(task);
		}
		return true;
	}

	private boolean handleAccountingTask(AccountTask task, @Nullable AccountTaskHandler handler) {
		if ( handler == null ) {
			log.warn("No accounting system is available for account task {}", task.getId());
			return false;
		}
		return handler.handleTask(task);
	}

	private void defer(AccountTask task, Instant execAt) {
		taskDao.deferAccountTask(nonnull(task.getId(), "Task ID"), execAt);
	}

	/**
	 * Get the handler for {@link AccountTaskType#SyncInvoiceToAccounting}
	 * tasks.
	 *
	 * @return the handler, or {@code null} if there is no accounting system
	 * @since 2.2
	 */
	public final @Nullable AccountTaskHandler getSyncInvoiceTaskHandler() {
		return syncInvoiceTaskHandler;
	}

	/**
	 * Set the handler for {@link AccountTaskType#SyncInvoiceToAccounting}
	 * tasks.
	 *
	 * <p>
	 * Without a handler these tasks are deferred, so they are handled once a
	 * handler is configured.
	 * </p>
	 *
	 * @param syncInvoiceTaskHandler
	 *        the handler to set, or {@code null} if there is no accounting
	 *        system
	 * @since 2.2
	 */
	public final void setSyncInvoiceTaskHandler(@Nullable AccountTaskHandler syncInvoiceTaskHandler) {
		this.syncInvoiceTaskHandler = syncInvoiceTaskHandler;
	}

	/**
	 * Get the handler for {@link AccountTaskType#SyncPaymentToAccounting}
	 * tasks.
	 *
	 * @return the handler, or {@code null} if there is no accounting system
	 * @since 2.2
	 */
	public final @Nullable AccountTaskHandler getSyncPaymentTaskHandler() {
		return syncPaymentTaskHandler;
	}

	/**
	 * Set the handler for {@link AccountTaskType#SyncPaymentToAccounting}
	 * tasks.
	 *
	 * <p>
	 * Without a handler these tasks are deferred, so they are handled once a
	 * handler is configured.
	 * </p>
	 *
	 * @param syncPaymentTaskHandler
	 *        the handler to set, or {@code null} if there is no accounting
	 *        system
	 * @since 2.2
	 */
	public final void setSyncPaymentTaskHandler(@Nullable AccountTaskHandler syncPaymentTaskHandler) {
		this.syncPaymentTaskHandler = syncPaymentTaskHandler;
	}

	/**
	 * Get the amount of time to defer a task that asks to be re-tried, or that
	 * fails.
	 *
	 * @return the delay, never {@code null}; defaults to
	 *         {@link #DEFAULT_RETRY_DELAY}
	 * @since 2.2
	 */
	public final Duration getRetryDelay() {
		return retryDelay;
	}

	/**
	 * Set the amount of time to defer a task that asks to be re-tried, or that
	 * fails.
	 *
	 * @param retryDelay
	 *        the delay to set; must be positive
	 * @throws IllegalArgumentException
	 *         if {@code retryDelay} is {@code null} or not positive
	 * @since 2.2
	 */
	public final void setRetryDelay(Duration retryDelay) {
		if ( !requireNonNullArgument(retryDelay, "retryDelay").isPositive() ) {
			throw new IllegalArgumentException("The retryDelay argument must be positive.");
		}
		this.retryDelay = retryDelay;
	}
}
