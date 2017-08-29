/* ==================================================================
 * InvoiceItem.java - 30/08/2017 6:34:10 AM
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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.BaseObjectEntity;

/**
 * A Killbill invoice item.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceItem extends BaseObjectEntity<String> {

	private static final long serialVersionUID = -2905307143638318134L;

	private String bundleId;
	private String subscriptionId;
	private String planName;
	private String phaseName;
	private String usageName;
	private String itemType;
	private String description;
	private LocalDate startDate;
	private LocalDate endDate;
	private DateTime ended;
	private BigDecimal amount;
	private String currencyCode;
	private String timeZoneId = "UTC";

	/**
	 * Default constructor.
	 */
	public InvoiceItem() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param id
	 *        the ID
	 */
	public InvoiceItem(String id) {
		super();
		setId(id);
	}

	/**
	 * Set the invoice item ID.
	 * 
	 * <p>
	 * This is an alias for {@link #setId(String)} passing
	 * {@link UUID#toString()}.
	 * </p>
	 * 
	 * @param invoiceItemId
	 *        the invoice item ID to set
	 */
	public void setInvoiceItemId(UUID invoiceItemId) {
		setId(invoiceItemId.toString());
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
			applyTimeZone(timeZoneId);
		}
	}

	private void applyTimeZone(String timeZoneId) {
		if ( timeZoneId == null ) {
			timeZoneId = "UTC";
		}
		if ( this.startDate != null ) {
			setCreated(startDate.toDateTimeAtStartOfDay(DateTimeZone.forID(timeZoneId)));
		}
		if ( this.endDate != null ) {
			setEnded(endDate.toDateTimeAtStartOfDay(DateTimeZone.forID(timeZoneId)));
		}
	}

	/**
	 * Get the bundle ID.
	 * 
	 * @return the bundle ID
	 */
	public String getBundleId() {
		return bundleId;
	}

	/**
	 * Set the bundle ID.
	 * 
	 * @param bundleId
	 *        the bundle ID to set
	 */
	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * Get the subscription ID.
	 * 
	 * @return the subscription ID
	 */
	public String getSubscriptionId() {
		return subscriptionId;
	}

	/**
	 * Set the subscription ID.
	 * 
	 * @param subscriptionId
	 *        the subscription ID to set
	 */
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	/**
	 * Get the plan name.
	 * 
	 * @return the plan name
	 */
	public String getPlanName() {
		return planName;
	}

	/**
	 * Set the plan name.
	 * 
	 * @param planName
	 *        the plan name to set
	 */
	public void setPlanName(String planName) {
		this.planName = planName;
	}

	/**
	 * Get the phase name.
	 * 
	 * @return the phase name
	 */
	public String getPhaseName() {
		return phaseName;
	}

	/**
	 * Set the phase name.
	 * 
	 * @param phaseName
	 *        the phase name to set
	 */
	public void setPhaseName(String phaseName) {
		this.phaseName = phaseName;
	}

	/**
	 * Get the usage name.
	 * 
	 * @return the usage name
	 */
	public String getUsageName() {
		return usageName;
	}

	/**
	 * Set the usage name.
	 * 
	 * @param usageName
	 *        the usage name to set
	 */
	public void setUsageName(String usageName) {
		this.usageName = usageName;
	}

	/**
	 * Get the item type.
	 * 
	 * @return the item type
	 */
	public String getItemType() {
		return itemType;
	}

	/**
	 * Set the item type.
	 * 
	 * @param itemType
	 *        the item type to set
	 */
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 * 
	 * @param startDate
	 *        the start date to set
	 */
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
		DateTime created = null;
		if ( startDate != null ) {
			created = startDate.toDateTimeAtStartOfDay(DateTimeZone.forID(this.timeZoneId));
		}
		setCreated(created);
	}

	/**
	 * Get the ended date.
	 * 
	 * @return the ended date
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Set the ended date.
	 * 
	 * @param endDate
	 *        the ended date to set
	 */
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the amount.
	 * 
	 * @return the amount
	 */
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

	/**
	 * Get the currency code.
	 * 
	 * @return the currencyCode
	 */
	@JsonGetter("currency")
	public String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Set the currency code.
	 * 
	 * @param currencyCode
	 *        the currency code to set
	 */
	@JsonSetter("currency")
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	/**
	 * Get the item ended date (with time zone).
	 * 
	 * @return the ended date
	 */
	public DateTime getEnded() {
		return ended;
	}

	/**
	 * Set the item ended date (with time zone).
	 * 
	 * @param ended
	 *        the ended to set
	 */
	public void setEnded(DateTime end) {
		this.ended = end;
	}

}
