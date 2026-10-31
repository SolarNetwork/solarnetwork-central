/* ==================================================================
 * GnucashSettings.java - 11/10/2026 2:52:07 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Settings for recording SNF invoices and payments in GnuCash.
 *
 * @author matt
 * @version 1.0
 */
public class GnucashSettings {

	/** The {@code schema} default value. */
	public static final String DEFAULT_SCHEMA = "public";

	/** The {@code customerIdTemplate} default value. */
	public static final String DEFAULT_CUSTOMER_ID_TEMPLATE = "SN_%d";

	/** The {@code timeZone} default value. */
	public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Pacific/Auckland");

	/** The {@code duePeriod} default value. */
	public static final Period DEFAULT_DUE_PERIOD = Period.ofMonths(1);

	/** The {@code lockTimeoutSecs} default value. */
	public static final int DEFAULT_LOCK_TIMEOUT_SECS = 30;

	/**
	 * The GnuCash accounts and tax table to use for one currency.
	 */
	public static class CurrencySettings {

		private @Nullable String receivableAccount;
		private @Nullable String incomeAccount;
		private @Nullable String creditAccount;
		private @Nullable String paymentAccount;
		private @Nullable String taxTable;
		private @Nullable String summaryDescription;

		/**
		 * Get the full name of the receivable account to post invoices to.
		 *
		 * @return the account name, such as {@code Assets:Accounts Receivable}
		 */
		public final @Nullable String getReceivableAccount() {
			return receivableAccount;
		}

		/**
		 * Set the full name of the receivable account to post invoices to.
		 *
		 * @param receivableAccount
		 *        the account name to set
		 */
		public final void setReceivableAccount(@Nullable String receivableAccount) {
			this.receivableAccount = receivableAccount;
		}

		/**
		 * Get the full name of the income account for invoice lines.
		 *
		 * @return the account name, such as {@code Income:Subscription}
		 */
		public final @Nullable String getIncomeAccount() {
			return incomeAccount;
		}

		/**
		 * Set the full name of the income account for invoice lines.
		 *
		 * @param incomeAccount
		 *        the account name to set
		 */
		public final void setIncomeAccount(@Nullable String incomeAccount) {
			this.incomeAccount = incomeAccount;
		}

		/**
		 * Get the full name of the account for the lines of credit notes made
		 * for credit granted to an account.
		 *
		 * @return the account name, or {@code null} to use
		 *         {@code incomeAccount}
		 */
		public final @Nullable String getCreditAccount() {
			return creditAccount;
		}

		/**
		 * Set the full name of the account for the lines of credit notes made
		 * for credit granted to an account.
		 *
		 * @param creditAccount
		 *        the account name to set, or {@code null} to use
		 *        {@code incomeAccount}
		 */
		public final void setCreditAccount(@Nullable String creditAccount) {
			this.creditAccount = creditAccount;
		}

		/**
		 * Get the full name of the account that payments are deposited to.
		 *
		 * @return the account name, such as {@code Assets:Bank}, or
		 *         {@code null} if payments cannot be recorded
		 */
		public final @Nullable String getPaymentAccount() {
			return paymentAccount;
		}

		/**
		 * Set the full name of the account that payments are deposited to.
		 *
		 * @param paymentAccount
		 *        the account name to set, or {@code null} if payments cannot be
		 *        recorded
		 */
		public final void setPaymentAccount(@Nullable String paymentAccount) {
			this.paymentAccount = paymentAccount;
		}

		/**
		 * Get the name of the tax table for customers that do not have one of
		 * their own.
		 *
		 * @return the tax table name, such as {@code GST (Sales)}
		 */
		public final @Nullable String getTaxTable() {
			return taxTable;
		}

		/**
		 * Set the name of the tax table for customers that do not have one of
		 * their own.
		 *
		 * @param taxTable
		 *        the tax table name to set
		 */
		public final void setTaxTable(@Nullable String taxTable) {
			this.taxTable = taxTable;
		}

		/**
		 * Get the description of the invoice line used when invoices are not
		 * itemized.
		 *
		 * @return the description
		 */
		public final @Nullable String getSummaryDescription() {
			return summaryDescription;
		}

		/**
		 * Set the description of the invoice line used when invoices are not
		 * itemized.
		 *
		 * @param summaryDescription
		 *        the description to set
		 */
		public final void setSummaryDescription(@Nullable String summaryDescription) {
			this.summaryDescription = summaryDescription;
		}

	}

	private String schema = DEFAULT_SCHEMA;
	private String customerIdTemplate = DEFAULT_CUSTOMER_ID_TEMPLATE;
	private boolean createCustomers = true;
	private ZoneId timeZone = DEFAULT_TIME_ZONE;
	private Period duePeriod = DEFAULT_DUE_PERIOD;
	private boolean itemized = true;
	private boolean creditNoteTaxable;
	private int lockTimeoutSecs = DEFAULT_LOCK_TIMEOUT_SECS;
	private Map<String, CurrencySettings> currencies = new LinkedHashMap<>(4);

	/**
	 * Get the GnuCash customer ID for a SolarNetwork user.
	 *
	 * @param userId
	 *        the user ID
	 * @return the customer ID
	 */
	public String customerId(Long userId) {
		return customerIdTemplate.formatted(userId);
	}

	/**
	 * Get the settings for a currency.
	 *
	 * @param currencyCode
	 *        the currency code
	 * @return the settings, or {@code null} if not configured
	 */
	public @Nullable CurrencySettings currency(String currencyCode) {
		return currencies.get(currencyCode);
	}

	/**
	 * Get the schema of the GnuCash tables.
	 *
	 * @return the schema; defaults to {@link #DEFAULT_SCHEMA}
	 */
	public final String getSchema() {
		return schema;
	}

	/**
	 * Set the schema of the GnuCash tables.
	 *
	 * @param schema
	 *        the schema to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setSchema(String schema) {
		this.schema = requireNonNullArgument(schema, "schema");
	}

	/**
	 * Get the template for GnuCash customer IDs.
	 *
	 * @return the template, with a {@code %d} parameter for the user ID;
	 *         defaults to {@link #DEFAULT_CUSTOMER_ID_TEMPLATE}
	 */
	public final String getCustomerIdTemplate() {
		return customerIdTemplate;
	}

	/**
	 * Set the template for GnuCash customer IDs.
	 *
	 * @param customerIdTemplate
	 *        the template to set, with a {@code %d} parameter for the user ID
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setCustomerIdTemplate(String customerIdTemplate) {
		this.customerIdTemplate = requireNonNullArgument(customerIdTemplate, "customerIdTemplate");
	}

	/**
	 * Get the setting to create a GnuCash customer when an invoice's account
	 * does not have one.
	 *
	 * @return {@code true} to create customers; defaults to {@code true}
	 */
	public final boolean isCreateCustomers() {
		return createCustomers;
	}

	/**
	 * Set the setting to create a GnuCash customer when an invoice's account
	 * does not have one.
	 *
	 * @param createCustomers
	 *        {@code true} to create customers
	 */
	public final void setCreateCustomers(boolean createCustomers) {
		this.createCustomers = createCustomers;
	}

	/**
	 * Get the time zone of the GnuCash book, used for invoice dates.
	 *
	 * @return the time zone; defaults to {@link #DEFAULT_TIME_ZONE}
	 */
	public final ZoneId getTimeZone() {
		return timeZone;
	}

	/**
	 * Set the time zone of the GnuCash book, used for invoice dates.
	 *
	 * @param timeZone
	 *        the time zone to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setTimeZone(ZoneId timeZone) {
		this.timeZone = requireNonNullArgument(timeZone, "timeZone");
	}

	/**
	 * Get the time after an invoice's date that it is due.
	 *
	 * @return the period; defaults to {@link #DEFAULT_DUE_PERIOD}
	 */
	public final Period getDuePeriod() {
		return duePeriod;
	}

	/**
	 * Set the time after an invoice's date that it is due.
	 *
	 * @param duePeriod
	 *        the period to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setDuePeriod(Period duePeriod) {
		this.duePeriod = requireNonNullArgument(duePeriod, "duePeriod");
	}

	/**
	 * Get the setting to add one GnuCash invoice line per invoice item.
	 *
	 * @return {@code true} for one line per item, or {@code false} for a single
	 *         summary line; defaults to {@code true}
	 */
	public final boolean isItemized() {
		return itemized;
	}

	/**
	 * Set the setting to add one GnuCash invoice line per invoice item.
	 *
	 * @param itemized
	 *        {@code true} for one line per item, or {@code false} for a single
	 *        summary line
	 */
	public final void setItemized(boolean itemized) {
		this.itemized = itemized;
	}

	/**
	 * Get the setting to include tax in the credit notes made for credit
	 * granted to an account.
	 *
	 * <p>
	 * SNF invoices apply account credit after tax, so by default granted credit
	 * does not change tax.
	 * </p>
	 *
	 * @return {@code true} if the credit amount includes tax; defaults to
	 *         {@code false}
	 */
	public final boolean isCreditNoteTaxable() {
		return creditNoteTaxable;
	}

	/**
	 * Set the setting to include tax in the credit notes made for credit
	 * granted to an account.
	 *
	 * @param creditNoteTaxable
	 *        {@code true} if the credit amount includes tax
	 */
	public final void setCreditNoteTaxable(boolean creditNoteTaxable) {
		this.creditNoteTaxable = creditNoteTaxable;
	}

	/**
	 * Get the maximum number of seconds to wait to lock the GnuCash book.
	 *
	 * @return the timeout, in seconds; defaults to
	 *         {@link #DEFAULT_LOCK_TIMEOUT_SECS}
	 */
	public final int getLockTimeoutSecs() {
		return lockTimeoutSecs;
	}

	/**
	 * Set the maximum number of seconds to wait to lock the GnuCash book.
	 *
	 * @param lockTimeoutSecs
	 *        the timeout to set, in seconds, or {@literal 0} for no limit
	 */
	public final void setLockTimeoutSecs(int lockTimeoutSecs) {
		this.lockTimeoutSecs = lockTimeoutSecs;
	}

	/**
	 * Get the settings for each currency.
	 *
	 * @return the settings, keyed by currency code
	 */
	public final Map<String, CurrencySettings> getCurrencies() {
		return currencies;
	}

	/**
	 * Set the settings for each currency.
	 *
	 * @param currencies
	 *        the settings to set, keyed by currency code
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setCurrencies(Map<String, CurrencySettings> currencies) {
		this.currencies = requireNonNullArgument(currencies, "currencies");
	}

}
