/* ==================================================================
 * InvoiceImpl.java - 24/07/2020 3:14:15 PM
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.META_AVAILABLE_CREDIT;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.solarnetwork.central.dao.BaseStringEntity;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;

/**
 * Wrap a {@link SnfInvoiceItem} as an
 * {@link net.solarnetwork.central.user.billing.domain.Invoice}.
 * 
 * @author matt
 * @version 2.0
 */
public class InvoiceImpl extends BaseStringEntity implements Invoice, InvoiceMatch {

	private static final long serialVersionUID = 6864680090286557577L;

	private final SnfInvoice invoice;
	private final List<InvoiceItem> items;

	/**
	 * Constructor.
	 * 
	 * @param invoice
	 *        the invoice to wrap
	 * @throws IllegalArgumentException
	 *         if {@code invoice} is {@literal null}
	 */
	public InvoiceImpl(SnfInvoice invoice) {
		this(invoice, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param invoice
	 *        the invoice to wrap
	 * @param items
	 *        the items to use directly
	 * @throws IllegalArgumentException
	 *         if {@code invoice} is {@literal null}
	 */
	public InvoiceImpl(SnfInvoice invoice, List<InvoiceItem> items) {
		super();
		if ( invoice == null ) {
			throw new IllegalArgumentException("The invoice argument must not be null.");
		}
		this.invoice = invoice;
		this.items = items;
		setId(invoice.getId().getId().toString());
		setCreated(invoice.getCreated());
	}

	@Override
	public YearMonth getInvoiceMonth() {
		// as long as the start/end date range is two consecutive start-of-month values (day 1) then return the month
		LocalDate startDate = invoice.getStartDate();
		LocalDate endDate = invoice.getEndDate();
		if ( startDate != null && startDate.getDayOfMonth() == 1
				&& startDate.plusMonths(1).equals(endDate) ) {
			return YearMonth.of(startDate.getYear(), startDate.getMonth());
		}
		return null;
	}

	@Override
	public String getTimeZoneId() {
		Address addr = invoice.getAddress();
		return (addr != null ? addr.getTimeZoneId() : null);
	}

	@Override
	public String getInvoiceNumber() {
		return SnfBillingUtils.invoiceNumForId(invoice.getId().getId());
	}

	@Override
	public BigDecimal getAmount() {
		return invoice.getTotalAmount();
	}

	@Override
	public BigDecimal getBalance() {
		return invoice.getTotalAmount();
	}

	@Override
	public BigDecimal getTaxAmount() {
		Set<SnfInvoiceItem> items = invoice.getItems();
		if ( items == null ) {
			items = Collections.emptySet();
		}
		return items.stream().filter(e -> InvoiceItemType.Tax.equals(e.getItemType()))
				.map(e -> e.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Override
	public BigDecimal getCreditAmount() {
		Set<SnfInvoiceItem> items = invoice.getItems();
		if ( items == null ) {
			items = Collections.emptySet();
		}
		BigDecimal sum = items.stream().filter(e -> InvoiceItemType.Credit.equals(e.getItemType()))
				.map(e -> e.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
		return (!sum.equals(BigDecimal.ZERO) ? sum : null);
	}

	@Override
	public BigDecimal getRemainingCreditAmount() {
		Set<SnfInvoiceItem> items = invoice.getItems();
		if ( items == null ) {
			items = Collections.emptySet();
		}
		BigDecimal amount = items
				.stream().filter(e -> InvoiceItemType.Credit.equals(e.getItemType())
						&& e.getMetadata() != null && e.getMetadata().containsKey(META_AVAILABLE_CREDIT))
				.map(e -> {
					Object availCreditVal = e.getMetadata().get(META_AVAILABLE_CREDIT);
					return (availCreditVal instanceof BigDecimal ? (BigDecimal) availCreditVal
							: new BigDecimal(availCreditVal.toString()));
				}).reduce(BigDecimal.ZERO, BigDecimal::add);
		return (!amount.equals(BigDecimal.ZERO) ? amount : null);
	}

	@Override
	public String getCurrencyCode() {
		return invoice.getCurrencyCode();
	}

	@Override
	public List<InvoiceItem> getInvoiceItems() {
		if ( items != null ) {
			return items;
		}
		Set<SnfInvoiceItem> items = invoice.getItems();
		if ( items == null ) {
			return Collections.emptyList();
		}
		return items.stream().map(e -> new InvoiceItemImpl(invoice, e)).collect(Collectors.toList());
	}

	@Override
	public List<InvoiceUsageRecord<Long>> getNodeUsageRecords() {
		if ( invoice == null ) {
			return Collections.emptyList();
		}
		Set<SnfInvoiceNodeUsage> usages = invoice.getUsages();
		if ( usages == null || usages.isEmpty() ) {
			return Collections.emptyList();
		}
		return usages.stream().sorted(InvoiceUsageRecordUsageKeyComparator.LONG_USAGE_COMPARATOR)
				.collect(Collectors.toList());
	}

}
