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

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.BillingDataConstants;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountTaskDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.dao.FilterResults;

/**
 * Create {@link AccountTask} entities for accounts that need to have invoices
 * generated.
 *
 * <p>
 * If a {@link InvoiceConsolidationResolver} is configured, the accounts in an
 * invoice consolidation group are invoiced together: the primary account of the
 * group is given a {@link AccountTaskType#GenerateConsolidatedInvoice} task for
 * each month any account in the group needs to be invoiced for, and the other
 * accounts in the group are given no tasks of their own. The accounts in
 * invalid groups are given no tasks at all.
 * </p>
 *
 * @author matt
 * @version 2.4
 */
public class InvoiceGenerationTaskCreator {

	/** The default batch size. */
	public static final int DEFAULT_BATCH_SIZE = 50;

	/**
	 * The messages to log problems with, which are their default messages.
	 */
	private static final MessageSource LOG_MESSAGES = new StaticMessageSource();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserDao userDao;
	private final AccountTaskDao accountTaskDao;
	private final SnfInvoicingSystem invoicingSystem;

	private int batchSize = DEFAULT_BATCH_SIZE;
	private @Nullable InvoiceConsolidationResolver consolidationResolver;

	/**
	 * Constructor.
	 *
	 * @param userDao
	 *        the user DAO to use
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param accountTaskDao
	 *        the account task DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public InvoiceGenerationTaskCreator(UserDao userDao, SnfInvoicingSystem invoicingSystem,
			AccountTaskDao accountTaskDao) {
		super();
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.accountTaskDao = requireNonNullArgument(accountTaskDao, "accountTaskDao");
	}

	/**
	 * Generate account tasks for all accounts, up to the start of the current
	 * month.
	 */
	public void createTasks() {
		// get the invoice end date, which is the start of the current month (exclusive)
		final LocalDate endDate = LocalDateTime.now(UTC).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)
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
		long offset = 0L;
		FilterResults<UserFilterMatch, Long> userResults;
		do {
			userResults = userDao.findFiltered(criteria, null, offset, max);
			for ( UserFilterMatch match : userResults ) {
				try {
					processOneUser(match, endDate);
				} catch ( RuntimeException e ) {
					// log error, but continue to next user
					log.error("Error generating invoice for user {}", match.getEmail(), e);
				}
			}
			offset += max;
		} while ( userResults.getTotalResults() != null && (userResults.getStartingOffset()
				+ userResults.getReturnedResultCount() < userResults.getTotalResults()) );
	}

	private void processOneUser(final UserFilterMatch user, final LocalDate endDate) {
		final Long userId = nonnull(user.getId(), "User ID");
		final InvoiceConsolidationResolver resolver = this.consolidationResolver;
		final InvoiceConsolidationGroup group = (resolver != null ? resolver.groupForUser(userId)
				: null);
		final MessageSourceResolvable problem = (group != null ? group.problem() : null);
		if ( group == null ) {
			processOneAccount(user, endDate);
		} else if ( problem != null ) {
			log.error(
					"Not invoicing user {} until the invoice consolidation group of user {} is fixed: {}",
					user.getEmail(), group.primaryUserId(),
					LOG_MESSAGES.getMessage(problem, Locale.getDefault()));
		} else if ( group.isPrimary(userId) ) {
			processGroup(user, group, endDate);
		} else {
			log.debug("User {} is invoiced with the invoice consolidation group of user {}.",
					user.getEmail(), group.primaryUserId());
		}
	}

	private void processGroup(final UserFilterMatch user, final InvoiceConsolidationGroup group,
			final LocalDate endDate) {
		final Account primary = accountForUser(user);
		if ( primary == null ) {
			log.error(
					"Unable to generate consolidated invoices for user {} because billing account not available.",
					user.getEmail());
			return;
		}
		final ZoneId primaryTimeZone = primary.getTimeZone();
		if ( primaryTimeZone == null ) {
			throw new RuntimeException(String.format("Account %d (%s) has no time zone set.",
					primary.getAccountId(), user.getEmail()));
		}

		// find all months any account in the group still needs to be invoiced for
		final SortedSet<YearMonth> months = new TreeSet<>();
		for ( Long userId : group.userIds() ) {
			final Account account = invoicingSystem.accountForUser(userId);
			if ( account == null ) {
				throw new RuntimeException(String.format(
						"Account for user %d in the invoice consolidation group of %s not available.",
						userId, user.getEmail()));
			}
			final ZoneId accountTimeZone = account.getTimeZone();
			if ( accountTimeZone == null ) {
				throw new RuntimeException(String.format("Account %d (user %d) has no time zone set.",
						account.getAccountId(), userId));
			}
			final ZonedDateTime invoiceEndDate = endDate.atStartOfDay(accountTimeZone);
			ZonedDateTime date = invoicedThroughDate(account.id(), invoiceEndDate.minusMonths(1));
			while ( date.isBefore(invoiceEndDate) ) {
				months.add(YearMonth.from(date));
				date = date.plusMonths(1);
			}
		}
		if ( months.isEmpty() ) {
			log.debug(
					"Invoice consolidation group of user {} invoicing up to date, nothing further to do.",
					user.getEmail());
			return;
		}

		for ( YearMonth month : months ) {
			log.info("Generating consolidated invoice for user {} for month {}", user.getEmail(), month);
			AccountTask task = AccountTask.newTask(
					month.atDay(1).atStartOfDay(primaryTimeZone).toInstant(),
					AccountTaskType.GenerateConsolidatedInvoice, primary.getAccountId(),
					ConsolidatedInvoiceGenerator.taskData(month));
			accountTaskDao.save(task);
		}
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

		final var accountPk = account.getId();

		// grab current account time zone
		final ZoneId accountTimeZone = account.getTimeZone();
		if ( accountTimeZone == null ) {
			throw new RuntimeException(String.format("Account %d (%s) has no time zone set.",
					account.getAccountId(), user.getEmail()));
		}

		final ZonedDateTime invoiceEndDate = endDate.atStartOfDay(accountTimeZone);

		// find up to what point in time invoicing has already been completed
		final ZonedDateTime invoicedThroughDate = invoicedThroughDate(accountPk,
				invoiceEndDate.minusMonths(1));
		if ( !invoicedThroughDate.isBefore(invoiceEndDate) ) {
			log.debug("User {} invoicing up to date, nothing further to do.", user.getEmail());
			return;
		}

		// time to invoice for this account
		ZonedDateTime currInvoiceDate = invoicedThroughDate;
		do {
			log.info("Generating invoice for user {} for month {}", user.getEmail(), currInvoiceDate);
			AccountTask task = AccountTask.newTask(currInvoiceDate.toInstant(),
					AccountTaskType.GenerateInvoice, account.getAccountId());
			accountTaskDao.save(task);
			log.info("InvoiceImpl generation task created for user {} for month {}", user.getEmail(),
					currInvoiceDate);
			currInvoiceDate = currInvoiceDate.plusMonths(1);
		} while ( currInvoiceDate.isBefore(invoiceEndDate) );
	}

	private @Nullable Account accountForUser(final UserFilterMatch user) {
		return invoicingSystem.accountForUser(nonnull(user.getId(), "User ID"));
	}

	private ZonedDateTime invoicedThroughDate(UserLongCompositePK accountPk,
			ZonedDateTime initialInvoiceStartDate) {
		SnfInvoice invoice = invoicingSystem.findLatestInvoiceForAccount(accountPk);
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

	/**
	 * Get the invoice consolidation resolver.
	 *
	 * @return the resolver, or {@code null} if accounts are always invoiced
	 *         individually
	 * @since 2.4
	 */
	public final @Nullable InvoiceConsolidationResolver getConsolidationResolver() {
		return consolidationResolver;
	}

	/**
	 * Set the invoice consolidation resolver.
	 *
	 * @param consolidationResolver
	 *        the resolver to set, or {@code null} to always invoice accounts
	 *        individually
	 * @since 2.4
	 */
	public final void setConsolidationResolver(
			@Nullable InvoiceConsolidationResolver consolidationResolver) {
		this.consolidationResolver = consolidationResolver;
	}

}
