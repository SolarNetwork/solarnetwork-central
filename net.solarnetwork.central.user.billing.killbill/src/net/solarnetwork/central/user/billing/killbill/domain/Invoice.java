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
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.BaseObjectEntity;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;

/**
 * A Killbill invoice.
 * 
 * @author matt
 * @version 1.0
 */
public class Invoice extends BaseObjectEntity<String>
		implements net.solarnetwork.central.user.billing.domain.Invoice, InvoiceMatch {

	private static final long serialVersionUID = -9188893829597927136L;

	private String timeZoneId = "UTC";
	private LocalDate invoiceDate;
	private String invoiceNumber;
	private BigDecimal amount;
	private BigDecimal balance;
	private String currencyCode;

	private List<InvoiceItem> items;

	/**
	 * Default constructor.
	 */
	public Invoice() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param id
	 *        the ID
	 */
	public Invoice(String id) {
		super();
		setId(id);
	}

	/**
	 * Set the invoice ID.
	 * 
	 * <p>
	 * This is an alias for {@link #setId(String)} passing
	 * {@link UUID#toString()}.
	 * </p>
	 * 
	 * @param invoiceId
	 *        the invoice ID to set
	 */
	public void setInvoiceId(UUID invoiceId) {
		setId(invoiceId.toString());
	}

	/**
	 * Get the invoice number.
	 * 
	 * @return the invoice number
	 */
	@Override
	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	/**
	 * Set the invoice number.
	 * 
	 * @param invoiceNumber
	 *        the invoiceNumber to set
	 */
	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	/**
	 * Get the invoice time zone.
	 * 
	 * @return the invoice time zone
	 */
	@Override
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
			applyTimeZone(timeZoneId);
		}
	}

	private void applyTimeZone(String timeZoneId) {
		if ( timeZoneId == null ) {
			timeZoneId = "UTC";
		}
		if ( this.invoiceDate != null ) {
			setCreated(invoiceDate.toDateTimeAtStartOfDay(DateTimeZone.forID(this.timeZoneId)));
		}
		if ( this.items != null ) {
			for ( InvoiceItem item : this.items ) {
				item.setTimeZoneId(timeZoneId);
			}
		}
	}

	/**
	 * Get the invoice date.
	 * 
	 * @return the invoice date
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<net.solarnetwork.central.user.billing.domain.InvoiceItem> getInvoiceItems() {
		return (List) getItems();
	}

	/**
	 * Get the invoice items.
	 * 
	 * @return the invoice items
	 */
	@JsonIgnore
	public List<InvoiceItem> getItems() {
		return items;
	}

	/**
	 * Set the invoice items
	 * 
	 * @param items
	 *        the invoice items to set
	 */
	@JsonSetter("items")
	public void setItems(List<InvoiceItem> items) {
		this.items = items;
	}

}
