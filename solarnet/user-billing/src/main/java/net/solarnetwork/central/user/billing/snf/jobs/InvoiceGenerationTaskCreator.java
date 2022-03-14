/* ==================================================================
 * InvoiceGenerationTaskCreator.java - 21/07/2020 10:06:05 AM
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.UserFilterMatch;

/**
 * Create {@link AccountTask} entities for accounts that need to have invoices
 * generated.
 * 
 * @author matt
 * @version 2.0
 */
public class InvoiceGenerationTaskCreator {

	/** The default batch size. */
	public static final int DEFAULT_BATCH_SIZE = 50;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserDao userDao;
	private final AccountTaskDao accountTaskDao;
	private final SnfInvoicingSystem invoicingSystem;

	private int batchSize = DEFAULT_BATCH_SIZE;

	/**
	 * Constructor.
	 * 
	 * @param userDao
	 *        the user DAO to use
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param accountTaskDao
	 *        the account task DAO to use
	 */
	public InvoiceGenerationTaskCreator(UserDao userDao, SnfInvoicingSystem invoicingSystem,
			AccountTaskDao accountTaskDao) {
		super();
		this.userDao = userDao;
		this.invoicingSystem = invoicingSystem;
		this.accountTaskDao = accountTaskDao;
	}

	/**
	 * Generate account tasks for all accounts, up to the start of the current
	 * month.
	 */
	public void createTasks() {
		// get the invoice end date, which is the start of the current month (exclusive)
		final LocalDate endDate = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)
				.toLocalDate();
		createTasks(endDate);
	}

	/**
	 * Generate account tasks for all accounts, up to the given end date.
	 * 
	 * @param endDate
	 *        the end date
	 */
	public void createTasks(final LocalDate endDate) {

		// iterate over users configured to use SNF Billing
		Map<String, Object> billingDataFilter = new HashMap<>();
		billingDataFilter.put(BillingDataConstants.ACCOUNTING_DATA_PROP,
				SnfBillingSystem.ACCOUNTING_SYSTEM_KEY);
		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(billingDataFilter);
		final int max = this.batchSize;
		int offset = 0;
		FilterResults<UserFilterMatch> userResults;
		do {
			userResults = userDao.findFiltered(criteria, null, offset, max);
			for ( UserFilterMatch match : userResults ) {
				try {
					processOneAccount(match, endDate);
				} catch ( RuntimeException e ) {
					// log error, but continue to next user
					log.error("Error generating invoice for user {}", match.getEmail(), e);
				}
			}
			offset += max;
		} while ( userResults.getStartingOffset() != null && userResults.getReturnedResultCount() != null
				&& userResults.getTotalResults() != null && (userResults.getStartingOffset()
						+ userResults.getReturnedResultCount() < userResults.getTotalResults()) );
	}

	private void processOneAccount(final UserFilterMatch user, final LocalDate endDate) {
		// get billing account
		final Account account = accountForUser(user);
		if ( account == null ) {
			log.error("Unable to generate invoices for user {} because billing account not available.",
					user.getEmail());
			return;
		} else if ( account.getId() == null ) {
			log.error("Unable to generate invoices for user {} because billing account has no ID.",
					user.getEmail());
			return;
		}

		// grab current account time zone
		final ZoneId accountTimeZone = account.getTimeZone();
		if ( accountTimeZone == null ) {
			throw new RuntimeException(String.format("Account %s has no time zone set.",
					account.getId().getId(), user.getEmail()));
		}

		final ZonedDateTime invoiceEndDate = endDate.atStartOfDay(accountTimeZone);

		// find up to what point in time invoicing has already been completed
		final ZonedDateTime invoicedThroughDate = invoicedThroughDate(account,
				invoiceEndDate.minusMonths(1));
		if ( !invoicedThroughDate.isBefore(invoiceEndDate) ) {
			log.debug("User {} invoicing up to date, nothing further to do.", user.getEmail());
			return;
		}

		// time to invoice for this account
		ZonedDateTime currInvoiceDate = invoicedThroughDate;
		do {
			log.info("Generating invoice for user {} for month {}", user.getEmail(), currInvoiceDate,
					endDate);
			AccountTask task = AccountTask.newTask(currInvoiceDate.toInstant(),
					AccountTaskType.GenerateInvoice, account.getId().getId());
			accountTaskDao.save(task);
			log.info("InvoiceImpl generation task created for user {} for month {} total = {} {}",
					user.getEmail(), currInvoiceDate);
			currInvoiceDate = currInvoiceDate.plusMonths(1);
		} while ( currInvoiceDate.isBefore(invoiceEndDate) );
	}

	private Account accountForUser(final UserFilterMatch user) {
		return invoicingSystem.accountForUser(user.getId());
	}

	private ZonedDateTime invoicedThroughDate(Account account, ZonedDateTime initialInvoiceStartDate) {
		SnfInvoice invoice = invoicingSystem.findLatestInvoiceForAccount(account.getId());
		if ( invoice == null ) {
			return initialInvoiceStartDate;
		}
		ZoneId zone = invoice.getTimeZone();
		if ( zone == null ) {
			throw new RuntimeException(
					String.format("InvoiceImpl %s has no time zone available.", invoice.getId()));
		}
		return invoice.getEndDate().atStartOfDay(zone);
	}

	/**
	 * Set the batch size to process users with.
	 * 
	 * <p>
	 * This is the maximum number of user records to fetch from the database and
	 * process at a time, e.g. a result page size. The service will iterate over
	 * all result pages to process all users.
	 * </p>
	 * 
	 * @param batchSize
	 *        the user record batch size to set
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
