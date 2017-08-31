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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	private Map<String, CustomField> customFields;

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

	/**
	 * Get all available custom fields.
	 * 
	 * @return the custom fields
	 */
	public Collection<CustomField> getCustomFields() {
		return (customFields != null ? customFields.values() : null);
	}

	/**
	 * Set all custom fields.
	 * 
	 * @param fields
	 *        the fields to set
	 */
	public void setCustomFields(List<CustomField> fields) {
		Map<String, CustomField> fieldMap = null;
		if ( fields != null ) {
			fieldMap = fields.stream()
					.collect(Collectors.toMap(CustomField::getName, Function.identity()));
		}
		this.customFields = fieldMap;
	}

	/**
	 * Get a custom field by its name.
	 * 
	 * @param name
	 *        the name of the field to get
	 * @return the field, or {@literal null} if not available
	 */
	public CustomField getCustomField(String name) {
		return (customFields != null ? customFields.get(name) : null);
	}

	/**
	 * Add a single custom field.
	 * 
	 * @param field
	 *        the field to add
	 */
	public void addCustomField(CustomField field) {
		Map<String, CustomField> fieldMap = this.customFields;
		if ( fieldMap == null && field != null ) {
			fieldMap = new LinkedHashMap<>(4);
			this.customFields = fieldMap;
		}
		if ( field != null ) {
			fieldMap.put(field.getName(), field);
		}
	}

}
