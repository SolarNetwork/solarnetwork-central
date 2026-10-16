/* ==================================================================
 * ConsolidatedInvoiceGenerator.java - 15/10/2026 10:52:36 am
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

package net.solarnetwork.central.user.billing.snf.jobs;

import static net.solarnetwork.central.user.billing.snf.domain.AccountTask.newTask;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.DeliverConsolidatedInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.DeliverInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.SyncInvoiceToAccounting;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.defaultOptions;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;

/**
 * Service to generate consolidated invoices for groups of SNF accounts.
 *
 * <p>
 * Handles {@link AccountTaskType#GenerateConsolidatedInvoice} tasks, whose
 * account is the primary account of the group, and whose {@link #MONTH_PARAM}
 * task data is the month to invoice. Each account in the group is invoiced for
 * the month, and the consolidated invoice that combines those invoices is
 * delivered to the primary account holder.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoiceGenerator implements AccountTaskHandler {

	/**
	 * The task data property name for the month to invoice, as {@code yyyy-MM}.
	 */
	public static final String MONTH_PARAM = "month";

	/**
	 * The messages to log problems with, which are their default messages.
	 */
	private static final MessageSource LOG_MESSAGES = new StaticMessageSource();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountDao accountDao;
	private final AccountTaskDao taskDao;
	private final SnfInvoicingSystem invoicingSystem;
	private final InvoiceConsolidationResolver consolidationResolver;
	private boolean syncInvoicesToAccounting;

	/**
	 * Constructor.
	 *
	 * @param accountDao
	 *        the account DAO
	 * @param taskDao
	 *        the account task DAO
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param consolidationResolver
	 *        the invoice consolidation resolver
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ConsolidatedInvoiceGenerator(AccountDao accountDao, AccountTaskDao taskDao,
			SnfInvoicingSystem invoicingSystem, InvoiceConsolidationResolver consolidationResolver) {
		super();
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.consolidationResolver = requireNonNullArgument(consolidationResolver,
				"consolidationResolver");
	}

	/**
	 * Create the task data for a consolidated invoice generation task.
	 *
	 * @param month
	 *        the month to invoice
	 * @return the task data
	 */
	public static Map<String, Object> taskData(YearMonth month) {
		return Map.of(MONTH_PARAM, requireNonNullArgument(month, "month").toString());
	}

	/**
	 * Generate the invoices of a group of accounts for the given task.
	 *
	 * <p>
	 * If the group is held because its configuration is invalid, the task is
	 * not handled, so it is attempted again later.
	 * </p>
	 *
	 * @param task
	 *        the task
	 */
	@Override
	public boolean handleTask(final AccountTask task) {
		assert task.getTaskType() == AccountTaskType.GenerateConsolidatedInvoice;
		final Long accountId = task.getAccountId();
		final Account primary = accountDao.getForAccountId(accountId);
		if ( primary == null ) {
			log.error(
					"Unable to generate consolidated invoice for task {} because billing account {} not available.",
					task.getId(), accountId);
			return true;
		}

		final YearMonth month = month(task);
		if ( month == null ) {
			log.error(
					"Unable to generate consolidated invoice for task {} because no valid '{}' parameter is available.",
					task, MONTH_PARAM);
			return true;
		}

		final Long userId = primary.getUserId();
		final InvoiceConsolidationGroup group = consolidationResolver.groupForUser(userId);
		if ( group == null || !group.isPrimary(userId) ) {
			log.warn(
					"Not generating consolidated invoice for user {} for month {} because the user is no longer the primary account of an invoice consolidation group.",
					userId, month);
			return true;
		}
		final MessageSourceResolvable problem = group.problem();
		if ( problem != null ) {
			log.warn(
					"Consolidated invoice for user {} for month {} will be generated once the invoice consolidation group is fixed: {}",
					userId, month, LOG_MESSAGES.getMessage(problem, Locale.getDefault()));
			return false;
		}

		log.info("Generating consolidated invoice for user {} for month {}", userId, month);
		final SnfConsolidatedInvoice invoice = invoicingSystem.generateConsolidatedInvoice(group, month,
				defaultOptions());
		if ( invoice == null ) {
			return true;
		}
		log.info("Consolidated invoice for user {} for month {} total = {} {}", userId, month,
				invoice.getTotalAmount(), invoice.getCurrencyCode());

		final Instant now = Instant.now();
		taskDao.save(newTask(now, DeliverConsolidatedInvoice, invoice.getAccountId(),
				invoiceTaskData(invoice.getUserId(), invoice.getInvoiceId())));
		final List<SnfInvoice> invoices = invoice.getInvoices();
		if ( invoices != null ) {
			for ( SnfInvoice accountInvoice : invoices ) {
				final Map<String, Object> taskData = invoiceTaskData(accountInvoice.getUserId(),
						accountInvoice.getInvoiceId());
				if ( group.deliverMemberInvoices() && !group.isPrimary(accountInvoice.getUserId()) ) {
					taskDao.save(newTask(now, DeliverInvoice, accountInvoice.getAccountId(), taskData));
				}
				if ( syncInvoicesToAccounting ) {
					taskDao.save(newTask(now, SyncInvoiceToAccounting, accountInvoice.getAccountId(),
							taskData));
				}
			}
		}

		return true;
	}

	private static Map<String, Object> invoiceTaskData(Long userId, Long invoiceId) {
		Map<String, Object> taskData = new LinkedHashMap<>(2);
		taskData.put(AccountTask.USER_ID_PARAM, userId);
		taskData.put(AccountTask.ID_PARAM, invoiceId);
		return taskData;
	}

	private static @Nullable YearMonth month(AccountTask task) {
		final Map<String, Object> taskData = task.getTaskData();
		final Object month = (taskData != null ? taskData.get(MONTH_PARAM) : null);
		if ( month == null ) {
			return null;
		}
		try {
			return YearMonth.parse(month.toString());
		} catch ( DateTimeParseException e ) {
			return null;
		}
	}

	/**
	 * Get the setting to record generated invoices in the accounting system.
	 *
	 * @return {@code true} to add a
	 *         {@link AccountTaskType#SyncInvoiceToAccounting} task for each
	 *         generated account invoice; defaults to {@code false}
	 */
	public final boolean isSyncInvoicesToAccounting() {
		return syncInvoicesToAccounting;
	}

	/**
	 * Set the setting to record generated invoices in the accounting system.
	 *
	 * @param syncInvoicesToAccounting
	 *        {@code true} to add a
	 *        {@link AccountTaskType#SyncInvoiceToAccounting} task for each
	 *        generated account invoice
	 */
	public final void setSyncInvoicesToAccounting(boolean syncInvoicesToAccounting) {
		this.syncInvoicesToAccounting = syncInvoicesToAccounting;
	}

}
