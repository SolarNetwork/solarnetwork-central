/* ==================================================================
 * Account.java - 21/08/2017 3:47:48 PM
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

/**
 * Killbill account info.
 * 
 * @author matt
 * @version 1.0
 */
public class Account {

	private String externalKey;
	private String accountId;
	private String name;
	private String email;
	private String timeZone;
	private String country;
	private String currency;
	private String paymentMethodId;
	private Integer billCycleDayLocal;

	/**
	 * Default constructor.
	 */
	public Account() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param accountId
	 *        the account ID
	 */
	public Account(String accountId) {
		super();
		this.accountId = accountId;
	}

	/**
	 * Construct with an ID and time zone.
	 * 
	 * @param accountId
	 *        the account ID
	 * @param timeZone
	 *        the account time zone
	 */
	public Account(String accountId, String timeZone) {
		super();
		this.accountId = accountId;
		this.timeZone = timeZone;
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

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email
	 *        the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the timeZone
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * @param timeZone
	 *        the timeZone to set
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country
	 *        the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}

	/**
	 * @param currency
	 *        the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * @return the paymentMethodId
	 */
	public String getPaymentMethodId() {
		return paymentMethodId;
	}

	/**
	 * @param paymentMethodId
	 *        the paymentMethodId to set
	 */
	public void setPaymentMethodId(String paymentMethodId) {
		this.paymentMethodId = paymentMethodId;
	}

	/**
	 * @return the billCycleDayLocal
	 */
	public Integer getBillCycleDayLocal() {
		return billCycleDayLocal;
	}

	/**
	 * @param billCycleDayLocal
	 *        the billCycleDayLocal to set
	 */
	public void setBillCycleDayLocal(Integer billCycleDayLocal) {
		this.billCycleDayLocal = billCycleDayLocal;
	}

}
