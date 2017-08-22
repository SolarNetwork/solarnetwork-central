/* ==================================================================
 * KillbillClient.java - 21/08/2017 3:44:17 PM
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

package net.solarnetwork.central.user.billing.killbill;

import java.util.Collection;
import java.util.Map;
import org.joda.time.LocalDate;
import net.solarnetwork.central.user.billing.killbill.domain.Account;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;

/**
 * API for interaction with Killbill.
 * 
 * @author matt
 * @version 1.0
 */
public interface KillbillClient {

	/**
	 * Get the account associated with an external key.
	 * 
	 * @param key
	 *        the key to find
	 * @return the account, or {@literal null} if not available
	 */
	Account accountForExternalKey(String key);

	/**
	 * Create a new account.
	 * 
	 * @param info
	 *        the account info to persist
	 * @return the account ID for the new account
	 */
	String createAccount(Account info);

	/**
	 * Add a payment method to an account.
	 * 
	 * @param account
	 *        the account to update
	 * @param paymentData
	 *        the payment data to add
	 * @param defaultMethod
	 *        {@literal true} to treat this as the default payment method
	 * @return the payment method ID
	 */
	String addPaymentMethodToAccount(Account account, Map<String, Object> paymentData,
			boolean defaultMethod);

	/**
	 * Get a bundle for an external key.
	 * 
	 * @param key
	 *        the external key of the bundle to get
	 * @return the bundle, or {@literal null} if not available
	 */
	Bundle bundleForExternalKey(String key);

	/**
	 * Create a bundle for an account.
	 * 
	 * @param account
	 *        the account to update
	 * @param requestedDate
	 *        the request date, or {@code null} for the current date
	 * @param info
	 *        the bundle info to create
	 * @return the bundle ID
	 */
	String createAccountBundle(Account account, LocalDate requestedDate, Bundle info);

	/**
	 * Add usage records to a subscription.
	 * 
	 * @param subscription
	 *        the subscription to add usage to
	 * @param unit
	 *        the usage unit name
	 * @param usage
	 *        the usage records
	 */
	void addUsage(Subscription subscription, String unit, Collection<UsageRecord> usage);
}
