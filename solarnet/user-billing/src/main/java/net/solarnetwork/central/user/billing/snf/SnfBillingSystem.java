/* ==================================================================
 * SnfBillingSystem.java - 20/07/2020 9:01:05 AM
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

package net.solarnetwork.central.user.billing.snf;

import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.user.billing.snf.SnfBillingUtils.invoiceForSnfInvoice;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoicingOptions;
import net.solarnetwork.central.user.billing.support.BasicBillingSystemInfo;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.domain.SortDescriptor;

/**
 * {@link BillingSystem} implementation for SolarNetwork Foundation.
 * 
 * @author matt
 * @version 2.0
 */
public class SnfBillingSystem implements BillingSystem {

	/** The {@literal accounting} billing data value for SNF. */
	public static final String ACCOUNTING_SYSTEM_KEY = "snf";

	private final AccountDao accountDao;
	private final SnfInvoiceDao invoiceDao;
	private final SnfInvoicingSystem invoicingSystem;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param invoicingSystem
	 *        the invoicing system
	 * @param accountDao
	 *        the account DAO
	 * @param invoiceDao
	 *        the invoice DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SnfBillingSystem(SnfInvoicingSystem invoicingSystem, AccountDao accountDao,
			SnfInvoiceDao invoiceDao) {
		super();
		this.invoicingSystem = requireNonNullArgument(invoicingSystem, "invoicingSystem");
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.invoiceDao = requireNonNullArgument(invoiceDao, "invoiceDao");
	}

	@Override
	public String getAccountingSystemKey() {
		return ACCOUNTING_SYSTEM_KEY;
	}

	@Override
	public boolean supportsAccountingSystemKey(String key) {
		return ACCOUNTING_SYSTEM_KEY.equals(key);
	}

	@Override
	public BillingSystemInfo getInfo(Locale locale) {
		return new BasicBillingSystemInfo(getAccountingSystemKey());
	}

	@Override
	public FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		// get account
		Account account = accountDao.getForUser(filter.getUserId());
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, filter.getUserId());
		}
		SnfInvoiceFilter invoiceFilter = SnfInvoiceFilter.forAccount(account);
		if ( filter.getUnpaid() != null ) {
			invoiceFilter.setUnpaidOnly(filter.getUnpaid());
		}
		net.solarnetwork.dao.FilterResults<SnfInvoice, UserLongPK> results = invoiceDao
				.findFiltered(invoiceFilter, SnfInvoiceDao.SORT_BY_INVOICE_DATE_DESCENDING, offset, max);
		List<InvoiceMatch> matches = StreamSupport.stream(results.spliterator(), false)
				.map(InvoiceImpl::new).collect(toList());
		return new BasicFilterResults<>(matches, results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

	private SnfInvoice getSnfInvoice(Long userId, String invoiceId) {
		final UserLongPK id = new UserLongPK(userId, Long.valueOf(invoiceId));
		final SnfInvoice invoice = invoiceDao.get(id);
		if ( invoice == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, invoiceId);
		}
		return invoice;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Invoice getInvoice(Long userId, String invoiceId, Locale locale) {
		final SnfInvoice invoice = getSnfInvoice(userId, invoiceId);
		final MessageSource messageSource = invoicingSystem.messageSourceForInvoice(invoice);
		return invoiceForSnfInvoice(invoice, messageSource, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Resource renderInvoice(Long userId, String invoiceId, MimeType outputType, Locale locale) {
		SnfInvoice invoice = getSnfInvoice(userId, invoiceId);
		return invoicingSystem.renderInvoice(invoice, outputType, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public Invoice getPreviewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			Locale locale) {
		SnfInvoice invoice = createPreviewInvoice(userId, options, locale);
		if ( invoice == null ) {
			return null;
		}
		final MessageSource messageSource = invoicingSystem.messageSourceForInvoice(invoice);
		return invoiceForSnfInvoice(invoice, messageSource, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public Resource previewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			MimeType outputType, Locale locale) {
		SnfInvoice invoice = createPreviewInvoice(userId, options, locale);
		if ( invoice == null ) {
			return null;
		}
		return invoicingSystem.renderInvoice(invoice, outputType, locale);
	}

	private SnfInvoice createPreviewInvoice(Long userId,
			net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions options,
			Locale locale) {
		Account account = accountDao.getForUser(userId);
		if ( account == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
		}

		LocalDate start;
		LocalDate end;
		if ( options != null && options.getMonth() != null ) {
			start = options.getMonth().atDay(1);
			end = start.plusMonths(1);
		} else {
			// find current month in account's time zone
			ZoneId zone = account.getTimeZone();
			if ( zone == null ) {
				zone = ZoneId.systemDefault();
			}
			start = LocalDate.now(zone).withDayOfMonth(1);
			end = start.plusMonths(1);
		}

		log.debug("Generating preview invoice for account {} (user {}) month {}", account.getId(),
				account.getUserId(), start);

		SnfInvoicingOptions opts = new SnfInvoicingOptions(true,
				options != null ? options.isUseAccountCredit() : false);
		SnfInvoice invoice = invoicingSystem.generateInvoice(userId, start, end, opts);
		return invoice;
	}

}
