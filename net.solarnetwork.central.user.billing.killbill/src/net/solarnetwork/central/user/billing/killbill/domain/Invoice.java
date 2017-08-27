/* ==================================================================
 * Invoice.java - 27/08/2017 2:32:48 PM
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

package net.solarnetwork.central.user.billing.killbill.domain;

import java.math.BigDecimal;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.BaseObjectEntity;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;

/**
 * A Killbill invoice.
 * 
 * @author matt
 * @version 1.0
 */
public class Invoice extends BaseObjectEntity<UUID>
		implements net.solarnetwork.central.user.billing.domain.Invoice, InvoiceMatch {

	private static final long serialVersionUID = -6377572949534534745L;

	private String timeZoneId = "UTC";
	private LocalDate invoiceDate;
	private BigDecimal amount;
	private BigDecimal balance;
	private String currencyCode;

	/**
	 * Set the invoice ID.
	 * 
	 * <p>
	 * This is an alias for {@link #setId(UUID)}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID to set
	 */
	public void setInvoiceId(UUID invoiceId) {
		setId(invoiceId);
	}

	/**
	 * Get the invoice time zone.
	 * 
	 * @return the invoice time zone
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the invoice time zone.
	 * 
	 * @param timeZoneId
	 *        the time zone ID to set
	 */
	public void setTimeZoneId(String timeZoneId) {
		if ( timeZoneId != null && !timeZoneId.equals(this.timeZoneId) ) {
			this.timeZoneId = timeZoneId;
			if ( this.invoiceDate != null ) {
				setCreated(invoiceDate.toDateTimeAtStartOfDay(DateTimeZone.forID(this.timeZoneId)));
			}
		}
	}

	/**
	 * Get the invoice date.
	 * 
	 * @return the invoice date
	 */
	public LocalDate getInvoiceDate() {
		return invoiceDate;
	}

	/**
	 * Set the invoice date.
	 * 
	 * @param invoiceDate
	 *        the invoice date to set
	 */
	public void setInvoiceDate(LocalDate invoiceDate) {
		this.invoiceDate = invoiceDate;
		DateTime created = null;
		if ( invoiceDate != null ) {
			created = invoiceDate.toDateTimeAtStartOfDay(DateTimeZone.forID(this.timeZoneId));
		}
		setCreated(created);
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * Set the amount.
	 * 
	 * @param amount
	 *        the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Override
	public BigDecimal getBalance() {
		return balance;
	}

	/**
	 * Set the balance.
	 * 
	 * @param balance
	 *        the balance to set
	 */
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Override
	@JsonGetter("currency")
	public String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Set the currency code.
	 * 
	 * @param currencyCode
	 *        the currencyCode to set
	 */
	@JsonSetter("currency")
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

}
