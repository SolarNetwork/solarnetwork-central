/* ==================================================================
 * SubscriptionUsageRecords.java - 30/08/2017 7:24:39 PM
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

import java.util.List;
import org.joda.time.LocalDate;

/**
 * Usage records associated with a subscription time range.
 * 
 * @author matt
 * @version 1.0
 */
public class SubscriptionUsageRecords {

	private String subscriptionId;
	private LocalDate startDate;
	private LocalDate endDate;
	private List<UnitRecord> rolledUpUnits;

	/**
	 * Get the subscription ID.
	 * 
	 * @return the subscriptionId
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
	 * Get the start date.
	 * 
	 * @return the startDate
	 */
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
	}

	/**
	 * Get the end date.
	 * 
	 * @return the endDate
	 */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 * 
	 * @param endDate
	 *        the end date to set
	 */
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the usage records.
	 * 
	 * @return the records
	 */
	public List<UnitRecord> getRolledUpUnits() {
		return rolledUpUnits;
	}

	/**
	 * Set the usage records.
	 * 
	 * @param rolledUpUnits
	 *        the usage records to set
	 */
	public void setRolledUpUnits(List<UnitRecord> rolledUpUnits) {
		this.rolledUpUnits = rolledUpUnits;
	}

}
