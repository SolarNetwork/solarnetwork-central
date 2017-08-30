/* ==================================================================
 * Subscription.java - 21/08/2017 4:57:11 PM
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
 * A subscription.
 * 
 * @author matt
 * @version 1.0
 */
public class Subscription {

	public static final String BASE_PRODUCT_CATEGORY = "BASE";

	public static final String ADDON_PRODUCT_CATEGORY = "ADD_ON";

	private String subscriptionId;
	private String productCategory;
	private String productName;
	private String planName;
	private Integer billCycleDayLocal;
	private String phaseType;

	/**
	 * Create with a plan name.
	 * 
	 * @param planName
	 *        the plan name
	 */
	public static Subscription withPlanName(String planName) {
		Subscription s = new Subscription();
		s.setPlanName(planName);
		return s;
	}

	/**
	 * Default constructor.
	 */
	public Subscription() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param subscriptionId
	 *        the ID
	 */
	public Subscription(String subscriptionId) {
		super();
		this.subscriptionId = subscriptionId;
	}

	/**
	 * @return the subscriptionId
	 */
	public String getSubscriptionId() {
		return subscriptionId;
	}

	/**
	 * @param subscriptionId
	 *        the subscriptionId to set
	 */
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	/**
	 * @return the productCategory
	 */
	public String getProductCategory() {
		return productCategory;
	}

	/**
	 * @param productCategory
	 *        the productCategory to set
	 */
	public void setProductCategory(String productCategory) {
		this.productCategory = productCategory;
	}

	/**
	 * @return the planName
	 */
	public String getPlanName() {
		return planName;
	}

	/**
	 * @param planName
	 *        the planName to set
	 */
	public void setPlanName(String planName) {
		this.planName = planName;
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

	/**
	 * @return the phaseType
	 */
	public String getPhaseType() {
		return phaseType;
	}

	/**
	 * @param phaseType
	 *        the phaseType to set
	 */
	public void setPhaseType(String phaseType) {
		this.phaseType = phaseType;
	}

	/**
	 * Get the product name.
	 * 
	 * @return the product name
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Set the product name.
	 * 
	 * @param productName
	 *        the product name to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

}
