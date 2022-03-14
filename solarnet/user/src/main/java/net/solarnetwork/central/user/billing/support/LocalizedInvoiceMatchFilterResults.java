/* ==================================================================
 * LocalizedInvoiceMatchFilterResults.java - 29/05/2018 1:14:28 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.support;

import static net.solarnetwork.central.user.billing.support.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.InvoiceMatchFilterResults;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceMatchFilterResultsInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceMatchInfo;

/**
 * Localized version of {@link InvoiceMatchFilterResults}.
 * 
 * @author matt
 * @version 2.0
 */
public class LocalizedInvoiceMatchFilterResults
		implements InvoiceMatchFilterResults, LocalizedInvoiceMatchFilterResultsInfo {

	private final FilterResults<InvoiceMatch> delegate;
	private final Locale locale;
	private final String currencyCode;

	/**
	 * Construct with locale.
	 * 
	 * <p>
	 * The currency code is taken from the first available invoice, or defaults
	 * to {@literal NZD}.
	 * </p>
	 * 
	 * @param delegate
	 *        the results
	 * @param locale
	 *        the desired locale
	 */
	public LocalizedInvoiceMatchFilterResults(FilterResults<InvoiceMatch> delegate, Locale locale) {
		this(delegate, locale, currencyCodeFromInvoices(delegate));
	}

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the results
	 * @param locale
	 *        the desired locale
	 * @param currencyCode
	 *        the currency code to use; will default to {@literal NZD} if
	 *        {@literal null}
	 */
	public LocalizedInvoiceMatchFilterResults(FilterResults<InvoiceMatch> delegate, Locale locale,
			String currencyCode) {
		super();
		assert delegate != null;
		this.delegate = localizedInvoices(delegate, locale);
		if ( locale == null ) {
			locale = Locale.getDefault();
		}
		this.locale = locale;
		if ( currencyCode == null ) {
			currencyCode = "NZD";
		}
		this.currencyCode = currencyCode;
	}

	private static String currencyCodeFromInvoices(FilterResults<InvoiceMatch> delegate) {
		// take currency code from first invoice
		assert delegate != null;
		Iterator<InvoiceMatch> itr = delegate.iterator();
		if ( itr.hasNext() ) {
			return itr.next().getCurrencyCode();
		}
		return null;
	}

	private static FilterResults<InvoiceMatch> localizedInvoices(FilterResults<InvoiceMatch> delegate,
			Locale locale) {
		if ( delegate == null ) {
			return null;
		}
		List<InvoiceMatch> list = StreamSupport.stream(delegate.spliterator(), false).map(item -> {
			if ( item instanceof LocalizedInvoiceMatchInfo ) {
				return item;
			}
			return (InvoiceMatch) new LocalizedInvoiceMatch(item, locale);
		}).collect(Collectors.toList());
		return new BasicFilterResults<>(list, delegate.getTotalResults(), delegate.getStartingOffset(),
				delegate.getReturnedResultCount());
	}

	@Override
	public Iterator<InvoiceMatch> iterator() {
		return delegate.iterator();
	}

	@Override
	public void forEach(Consumer<? super InvoiceMatch> action) {
		delegate.forEach(action);
	}

	@Override
	public Iterable<InvoiceMatch> getResults() {
		return delegate.getResults();
	}

	@Override
	public Long getTotalResults() {
		return delegate.getTotalResults();
	}

	@Override
	public Integer getStartingOffset() {
		return delegate.getStartingOffset();
	}

	@Override
	public Spliterator<InvoiceMatch> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public Integer getReturnedResultCount() {
		return delegate.getReturnedResultCount();
	}

	/**
	 * Sum a set of {@code BigDecimal} values.
	 * 
	 * @param func
	 *        the method to use that returns the value to sum, e.g.
	 *        {@code InvoiceMatch::getAmount}
	 * @return the sum total, or {@literal 0} if no results
	 */
	private BigDecimal sum(Function<InvoiceMatch, BigDecimal> func) {
		return StreamSupport.stream(spliterator(), false).map(func).reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}

	@Override
	public BigDecimal getTotalAmount() {
		return sum(InvoiceMatch::getAmount);
	}

	@Override
	public BigDecimal getTotalBalance() {
		return sum(InvoiceMatch::getBalance);
	}

	@Override
	public BigDecimal getTotalTaxAmount() {
		return sum(InvoiceMatch::getTaxAmount);
	}

	@Override
	public String getCurrencyCode() {
		return currencyCode;
	}

	@Override
	public String getLocalizedTotalAmount() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getTotalAmount());
	}

	@Override
	public String getLocalizedTotalBalance() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getTotalBalance());
	}

	@Override
	public String getLocalizedTotalTaxAmount() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getTotalTaxAmount());
	}

	@Override
	public Iterable<LocalizedInvoiceMatchInfo> getLocalizedInvoices() {
		if ( delegate == null ) {
			return null;
		}
		return StreamSupport.stream(spliterator(), false).map(item -> {
			if ( item instanceof LocalizedInvoiceMatchInfo ) {
				return (LocalizedInvoiceMatchInfo) item;
			}
			return new LocalizedInvoiceMatch(item, locale);
		}).collect(Collectors.toList());
	}

}
