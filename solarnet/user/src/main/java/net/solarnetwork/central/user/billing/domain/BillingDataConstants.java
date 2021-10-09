/* ==================================================================
 * BillingDataConstants.java - 22/08/2017 2:00:21 PM
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

package net.solarnetwork.central.user.billing.domain;

import java.util.Collections;
import net.solarnetwork.central.domain.UserFilterCommand;

/**
 * Constants related to billing data.
 * 
 * @author matt
 * @version 1.0
 */
public final class BillingDataConstants {

	private BillingDataConstants() {
		// don't construct me
	}

	/** The billing data property that holds the accounting integration name. */
	public static final String ACCOUNTING_DATA_PROP = "accounting";

	/**
	 * Create a new filter for searching for a specific accounting type.
	 * 
	 * @param type
	 *        the type of accounting to search for
	 * @return the filter
	 */
	public static UserFilterCommand filterForAccountingType(String type) {
		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(Collections.singletonMap(ACCOUNTING_DATA_PROP, type));
		return criteria;
	}
}
