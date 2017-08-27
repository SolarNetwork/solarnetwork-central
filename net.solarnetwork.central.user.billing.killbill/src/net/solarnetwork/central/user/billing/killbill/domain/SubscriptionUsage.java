/* ==================================================================
 * SubscriptionUsage.java - 23/08/2017 3:20:38 PM
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Object to associate usage with a subscription.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "subscriptionId", "unitUsageRecords" })
public class SubscriptionUsage {

	private final Subscription subscription;
	private final List<UsageUnitRecord> unitUsageRecords;

	/**
	 * Constructor.
	 * 
	 * @param subscription
	 *        the subscription
	 * @param usage
	 *        the usage
	 */
	public SubscriptionUsage(Subscription subscription, List<UsageUnitRecord> unitUsageRecords) {
		super();
		this.subscription = subscription;
		this.unitUsageRecords = unitUsageRecords;
	}

	/**
	 * Get the subscription.
	 * 
	 * @return the subscription
	 */
	@JsonIgnore
	public Subscription getSubscription() {
		return subscription;
	}

	/**
	 * Get the subscription ID.
	 * 
	 * @return the subscription ID
	 */
	public String getSubscriptionId() {
		return subscription.getSubscriptionId();
	}

	/**
	 * Get the unit usage records.
	 * 
	 * @return the unitUsageRecords
	 */
	public List<UsageUnitRecord> getUnitUsageRecords() {
		return unitUsageRecords;
	}

}
