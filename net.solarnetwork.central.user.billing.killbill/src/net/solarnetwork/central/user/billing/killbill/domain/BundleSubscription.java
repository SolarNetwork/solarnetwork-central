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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A bundle and subscription combo, for setting up a new bundle.
 * 
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "accountId", "bundleId", "externalKey" })
public class BundleSubscription {

	private final Bundle bundle;
	private final Subscription subscription;

	/**
	 * Create a list of bundle subscriptions from a bundle instance with at
	 * least one base subscription and any number of add-on subscriptions.
	 * 
	 * @param bundle
	 *        the bundle
	 * @param accountId
	 *        the account ID
	 * @return the list, or an empty list if there are no subscriptions in
	 *         {@code bundle}
	 * @since 1.1
	 */
	public static List<BundleSubscription> entitlementsForBundle(Bundle bundle, String accountId) {
		List<BundleSubscription> result = new ArrayList<>(
				bundle.getSubscriptions() != null ? bundle.getSubscriptions().size() : 0);
		if ( bundle.getSubscriptions() != null ) {
			for ( Subscription sub : bundle.getSubscriptions() ) {
				Bundle b = (Bundle) bundle.clone();
				b.setAccountId(accountId);
				if ( !result.isEmpty() ) {
					// only the first can have an external key because applied at bundle level
					b.setExternalKey(null);
				}
				result.add(new BundleSubscription(b, sub));
			}
		}
		return result;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This constructor will clone {@code bundle} and set the {@code bundleId}
	 * to {@literal null}, and extract the first subscription from
	 * {@link Bundle#getSubscriptions()}. This is designed for when a bundle is
	 * getting created the first time.
	 * </p>
	 * 
	 * @param bundle
	 *        the bundle to create from
	 */
	public BundleSubscription(Bundle bundle) {
		super();
		this.bundle = (Bundle) bundle.clone();
		this.bundle.setBundleId(null);
		this.subscription = bundle.getSubscriptions() != null && !bundle.getSubscriptions().isEmpty()
				? bundle.getSubscriptions().get(0)
				: null;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This constructor will not modify {@code bundle} nor {@code subscription}.
	 * </p>
	 * 
	 * @param the
	 *        bundle
	 * @param subscription
	 *        the subscription
	 * @since 1.1
	 */
	public BundleSubscription(Bundle bundle, Subscription subscription) {
		super();
		this.bundle = bundle;
		this.subscription = subscription;
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
	 * Get the bundle ID.
	 * 
	 * @return the bundle ID, or {@literal null} when creating a new bundle
	 */
	public String getBundleId() {
		return bundle.getBundleId();
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
		return subscription;
	}

}
