/* ==================================================================
 * Bundle.java - 21/08/2017 4:58:00 PM
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

/**
 * A subscription bundle.
 * 
 * @author matt
 * @version 1.0
 */
public class Bundle implements Cloneable {

	private String accountId;
	private String bundleId;
	private String externalKey;
	private List<Subscription> subscriptions;

	/**
	 * Get a subscription based on a plan name.
	 * 
	 * @param planName
	 *        the plan name to look for
	 * @return the first matching subscription, or {@literal null} if not found
	 */
	public Subscription subscriptionWithPlanName(String planName) {
		if ( subscriptions != null ) {
			for ( Subscription subscription : subscriptions ) {
				if ( planName.equals(subscription.getPlanName()) ) {
					return subscription;
				}
			}
		}
		return null;
	}

	/**
	 * Clone the bundle.
	 * 
	 * <p>
	 * <b>Note</b> the {@code subscriptions} list is <b>not</b> cloned, so the
	 * returned instance shares the same list as this object.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the bundleId
	 */
	public String getBundleId() {
		return bundleId;
	}

	/**
	 * @param bundleId
	 *        the bundleId to set
	 */
	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * @return the externalKey
	 */
	public String getExternalKey() {
		return externalKey;
	}

	/**
	 * @param externalKey
	 *        the externalKey to set
	 */
	public void setExternalKey(String externalKey) {
		this.externalKey = externalKey;
	}

	/**
	 * @return the subscriptions
	 */
	public List<Subscription> getSubscriptions() {
		return subscriptions;
	}

	/**
	 * @param subscriptions
	 *        the subscriptions to set
	 */
	public void setSubscriptions(List<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId
	 *        the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

}
