/* ==================================================================
 * InvoiceGenerator.java - 20/07/2020 1:45:41 PM
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

import static java.lang.String.format;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTask.newTask;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.DeliverInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions.defaultOptions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Service to generate invoices for SNF accounts.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceGenerator implements AccountTaskHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AccountDao accountDao;
	private final AccountTaskDao taskDao;
	private final SnfInvoicingSystem invoicingSystem;

	/**
	 * Constructor.
	 * 
	 * @param accountDao
	 *        the account DAO
	 * @param taskDao
	 *        the account task DAO
	 * @param invoicingSystem
	 *        the invoicing system
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public InvoiceGenerator(AccountDao accountDao, AccountTaskDao taskDao,
			SnfInvoicingSystem invoicingSystem) {
		super();
		if ( accountDao == null ) {
			throw new IllegalArgumentException("The accountDao argument must not be null.");
		}
		this.accountDao = accountDao;
		if ( taskDao == null ) {
			throw new IllegalArgumentException("The taskDao argument must not be null.");
		}
		this.taskDao = taskDao;
		if ( invoicingSystem == null ) {
			throw new IllegalArgumentException("The invoicingSystem argument must not be null.");
		}
		this.invoicingSystem = invoicingSystem;
	}

	/**
	 * Generate a single invoice for the given task.
	 * 
	 * @param task
	 *        the task
	 */
	@Override
	public boolean handleTask(final AccountTask task) {
		assert task.getTaskType() == AccountTaskType.GenerateInvoice;

		final Account account = accountDao.get(new UserLongPK(null, task.getAccountId()));
		if ( account == null ) {
			log.error(
					"Unable to generate invoices for task {} because billing account {} not available.",
					task.getId(), task.getAccountId());
			return true;
		}

		// grab current account time zone
		final ZoneId accountTimeZone = account.getTimeZone();
		if ( accountTimeZone == null ) {
			throw new RuntimeException(
					format("Account %s has no time zone set.", account.getId().getId()));
		}

		final ZonedDateTime invoiceStartDate = task.getCreated().atZone(accountTimeZone);

		log.info("Generating invoice for user {} for month {}", account.getUserId(), invoiceStartDate);
		SnfInvoice invoice = invoicingSystem.generateInvoice(account.getUserId(),
				invoiceStartDate.toLocalDate(), invoiceStartDate.plusMonths(1).toLocalDate(),
				defaultOptions());
		if ( invoice != null ) {
			log.info("InvoiceImpl for user {} for month {} total = {} {}", account.getUserId(),
					invoiceStartDate, invoice.getTotalAmount(), invoice.getCurrencyCode());
			Map<String, Object> taskData = new LinkedHashMap<>(2);
			taskData.put("userId", invoice.getUserId());
			taskData.put("id", invoice.getId().getId());
			taskDao.save(newTask(Instant.now(), DeliverInvoice, task.getAccountId(), taskData));
		}

		return true;
	}

}
