/* ==================================================================
 * BundleSubscription.java - 23/08/2017 2:45:52 PM
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
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A bundle and subscription combo, for setting up a new bundle.
 * 
 * @author matt
 * @version 1.0
 */
public class BundleSubscription {

	private final Bundle bundle;

	/**
	 * Constructor.
	 */
	public BundleSubscription(Bundle bundle) {
		super();
		this.bundle = bundle;
	}

	/**
	 * Get the bundle.
	 * 
	 * @return the bundle
	 */
	@JsonIgnore
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Get the bundle account ID.
	 * 
	 * @return the bundle account ID
	 */
	public String getAccountId() {
		return bundle.getAccountId();
	}

	/**
	 * Get the bundle external key.
	 * 
	 * @return the bundle external key
	 */
	public String getExternalKey() {
		return bundle.getExternalKey();
	}

	/**
	 * Get the first subscription.
	 * 
	 * @return the first subscription, or {@literal null}
	 */
	@JsonUnwrapped
	public Subscription getSubscription() {
		List<Subscription> subs = bundle.getSubscriptions();
		return (subs != null && !subs.isEmpty() ? subs.get(0) : null);
	}

}
