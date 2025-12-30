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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;

/**
 * Job to process {@link AccountTaskType} tasks.
 *
 * @author matt
 * @version 2.1
 */
public class AccountTaskJob extends JobSupport {

	private final TransactionTemplate transactionTemplate;
	private final AccountTaskDao taskDao;
	private final AccountTaskHandler generateInvoiceTaskHandler;
	private final AccountTaskHandler deliverInvoiceTaskHandler;

	/**
	 * Constructor.
	 *
	 * @param transactionTemplate
	 *        the transaction template to use, or {@literal null}
	 * @param taskDao
	 *        the task DAO
	 * @param generateInvoiceTaskHandler
	 *        the handler for {@link AccountTaskType#GenerateInvoice}
	 * @param deliverInvoiceTaskHandler
	 *        the handler for {@link AccountTaskType#DeliverInvoice}
	 * @throws IllegalArgumentException
	 *         if {@code taskDao} or {@code generateInvoiceTaskHandler} is
	 *         {@literal null}
	 */
	public AccountTaskJob(TransactionTemplate transactionTemplate, AccountTaskDao taskDao,
			AccountTaskHandler generateInvoiceTaskHandler,
			AccountTaskHandler deliverInvoiceTaskHandler) {
		super();
		this.transactionTemplate = transactionTemplate;
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.generateInvoiceTaskHandler = requireNonNullArgument(generateInvoiceTaskHandler,
				"generateInvoiceTaskHandler");
		this.deliverInvoiceTaskHandler = requireNonNullArgument(deliverInvoiceTaskHandler,
				"deliverInvoiceTaskHandler");
		setGroupId("Billing");
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
			try {
				if ( transactionTemplate != null ) {
					processed = transactionTemplate.execute(_ -> execute());
				} else {
					processed = execute();
				}
			} catch ( RepeatableTaskException e ) {
				log.debug(e.getMessage(), e);
				log.info("Error processing account task; will re-try later: {}", e.getMessage());
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
	private boolean execute() {
		final AccountTask task = taskDao.claimAccountTask();
		if ( task == null ) {
			return false;
		}
		boolean retry = false;
		final AccountTaskType taskType = task.getTaskType();
		switch (taskType) {
			case GenerateInvoice:
				retry = !generateInvoiceTaskHandler.handleTask(task);
				break;

			case DeliverInvoice:
				retry = !deliverInvoiceTaskHandler.handleTask(task);
				break;

			default:
				// do nothing, but delete
				break;
		}
		if ( !retry ) {
			taskDao.delete(task);
		}
		return true;
	}
}
