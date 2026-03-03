/* ==================================================================
 * LocalizedInvoiceItem.java - 30/08/2017 8:24:31 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;

/**
 * Localized version of {@link InvoiceItem}.
 * 
 * @author matt
 * @version 3.0
 */
public class LocalizedInvoiceItem implements InvoiceItem, LocalizedInvoiceItemInfo {

	private final InvoiceItem item;
	private final Locale locale;
	private final String localizedDescription;
	private final String @Nullable [] localizedUsageTierDescriptions;

	/**
	 * Convenience builder.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @return the localized invoice
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public static LocalizedInvoiceItem of(InvoiceItem item, Locale locale) {
		return new LocalizedInvoiceItem(item, locale);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public LocalizedInvoiceItem(InvoiceItem item, Locale locale) {
		this(item, locale, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param localizedDescription
	 *        the localized description
	 * @throws IllegalArgumentException
	 *         if {@code item} or {@code locale} is {@code null}
	 */
	public LocalizedInvoiceItem(InvoiceItem item, Locale locale, @Nullable String localizedDescription) {
		this(item, locale, localizedDescription, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param localizedDescription
	 *        the localized description
	 * @param localizedUsageTierDescriptions
	 *        the localized tier usage descriptions
	 * @throws IllegalArgumentException
	 *         if {@code item} or {@code locale} is {@code null}
	 * @since 1.1
	 */
	public LocalizedInvoiceItem(InvoiceItem item, Locale locale, @Nullable String localizedDescription,
			String @Nullable [] localizedUsageTierDescriptions) {
		super();
		this.item = requireNonNullArgument(item, "item");
		this.locale = requireNonNullArgument(locale, "locale");
		this.localizedDescription = (localizedDescription != null ? localizedDescription
				: item.getDescription());
		this.localizedUsageTierDescriptions = localizedUsageTierDescriptions;
	}

	@Override
	public String getLocalizedDescription() {
		return localizedDescription;
	}

	@Override
	public String getLocalizedStartDate() {
		DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(ZoneId.of(tz));
		}
		return fmt.format(getStartDate());
	}

	@Override
	public String getLocalizedEndDate() {
		DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(ZoneId.of(tz));
		}
		return fmt.format(getEndDate());
	}

	@Override
	public String getLocalizedAmount() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getAmount());
	}

	@Override
	public @Nullable Instant getCreated() {
		return item.getCreated();
	}

	@Override
	public String getTimeZoneId() {
		return item.getTimeZoneId();
	}

	@Override
	public @Nullable String getId() {
		return item.getId();
	}

	@Override
	public @Nullable Map<String, Object> getMetadata() {
		return item.getMetadata();
	}

	@Override
	public String getPlanName() {
		return item.getPlanName();
	}

	@Override
	public String getItemType() {
		return item.getItemType();
	}

	@Override
	public String getDescription() {
		return item.getDescription();
	}

	@Override
	public LocalDate getStartDate() {
		return item.getStartDate();
	}

	@Override
	public LocalDate getEndDate() {
		return item.getEndDate();
	}

	@Override
	public BigDecimal getAmount() {
		return item.getAmount();
	}

	@Override
	public String getCurrencyCode() {
		return item.getCurrencyCode();
	}

	@Override
	public Instant getEnded() {
		return item.getEnded();
	}

	@Override
	public List<InvoiceItemUsageRecord> getItemUsageRecords() {
		return item.getItemUsageRecords();
	}

	@Override
	public BigDecimal getTotalUsageAmount() {
		return item.getTotalUsageAmount();
	}

	@Override
	public List<LocalizedInvoiceItemUsageRecordInfo> getLocalizedInvoiceItemUsageRecords() {
		List<InvoiceItemUsageRecord> recs = getItemUsageRecords();
		if ( recs == null || recs.isEmpty() ) {
			return Collections.emptyList();
		}
		return recs.stream().map(record -> {
			if ( record instanceof LocalizedInvoiceItemUsageRecordInfo li ) {
				return li;
			}
			return LocalizedInvoiceItemUsageRecord.of(record, locale, getCurrencyCode(),
					localizedUsageTierDescriptions);
		}).collect(Collectors.toList());
	}

	@Override
	public String getLocalizedTotalUsageAmount() {
		NumberFormat fmt = DecimalFormat.getNumberInstance(locale);
		return fmt.format(getTotalUsageAmount());
	}

}
