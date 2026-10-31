/* ==================================================================
 * ConsolidatedInvoiceImpl.java - 15/10/2026 1:05:20 pm
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

package net.solarnetwork.central.user.billing.snf.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.BaseStringEntity;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;

/**
 * Implementation of {@link Invoice} for a consolidated invoice.
 *
 * <p>
 * The amounts are the sums of those of the account invoices, and the items are
 * all the items of the account invoices, so the tax items of all accounts can
 * be totalled together.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoiceImpl extends BaseStringEntity implements Invoice, InvoiceMatch {

	@Serial
	private static final long serialVersionUID = 4302049553722407711L;

	private final SnfConsolidatedInvoice invoice;
	private final List<Invoice> invoices;

	/**
	 * Constructor.
	 *
	 * @param invoice
	 *        the consolidated invoice
	 * @param invoices
	 *        the account invoices of the consolidated invoice
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ConsolidatedInvoiceImpl(SnfConsolidatedInvoice invoice, List<? extends Invoice> invoices) {
		super();
		this.invoice = requireNonNullArgument(invoice, "invoice");
		this.invoices = List.copyOf(requireNonNullArgument(invoices, "invoices"));
		setId(invoice.getInvoiceId().toString());
		setCreated(invoice.getCreated());
	}

	private BigDecimal sum(Function<Invoice, @Nullable BigDecimal> amount) {
		return invoices.stream().map(amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,
				BigDecimal::add);
	}

	/**
	 * Get the account invoices.
	 *
	 * @return the account invoices
	 */
	public List<Invoice> getInvoices() {
		return invoices;
	}

	@Override
	public @Nullable YearMonth getInvoiceMonth() {
		// as long as the start/end date range is two consecutive start-of-month values (day 1) then return the month
		LocalDate startDate = invoice.getStartDate();
		LocalDate endDate = invoice.getEndDate();
		if ( startDate.getDayOfMonth() == 1 && startDate.plusMonths(1).equals(endDate) ) {
			return YearMonth.of(startDate.getYear(), startDate.getMonth());
		}
		return null;
	}

	@Override
	public String getTimeZoneId() {
		return nonnull(invoice.getAddress(), "address").getTimeZoneId();
	}

	@Override
	public String getInvoiceNumber() {
		return nonnull(SnfBillingUtils.invoiceNumForId(invoice.getInvoiceId()), "invoice.invoiceId");
	}

	@Override
	public BigDecimal getAmount() {
		return sum(Invoice::getAmount);
	}

	@Override
	public BigDecimal getBalance() {
		return sum(Invoice::getBalance);
	}

	@Override
	public BigDecimal getTaxAmount() {
		return sum(Invoice::getTaxAmount);
	}

	@Override
	public @Nullable BigDecimal getCreditAmount() {
		BigDecimal sum = sum(Invoice::getCreditAmount);
		return (sum.compareTo(BigDecimal.ZERO) != 0 ? sum : null);
	}

	/**
	 * Get the remaining credit amount.
	 *
	 * <p>
	 * Credit belongs to each account, so is not combined.
	 * </p>
	 *
	 * @return {@code null}
	 */
	@Override
	public @Nullable BigDecimal getRemainingCreditAmount() {
		return null;
	}

	@Override
	public String getCurrencyCode() {
		return invoice.getCurrencyCode();
	}

	@Override
	public List<InvoiceItem> getInvoiceItems() {
		return invoices.stream().flatMap(e -> e.getInvoiceItems().stream()).toList();
	}

	@Override
	public List<InvoiceUsageRecord<Long>> getNodeUsageRecords() {
		return List.of();
	}

	@Override
	public boolean isConsolidated() {
		return true;
	}

	@Override
	public @Nullable String getConsolidatedInvoiceNumber() {
		return null;
	}

}
