/* ==================================================================
 * UsageUnitRecord.java - 23/08/2017 3:24:04 PM
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Usage records of a specific type.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "unitType", "usageRecords" })
public class UsageUnitRecord {

	private final String unitType;
	private final List<UsageRecord> usageRecords;

	/**
	 * Constructor.
	 */
	public UsageUnitRecord(String unitType, List<UsageRecord> usageRecords) {
		super();
		this.unitType = unitType;
		this.usageRecords = usageRecords;
	}

	/**
	 * Get the unit type.
	 * 
	 * @return the unitType
	 */
	public String getUnitType() {
		return unitType;
	}

	/**
	 * Get the usage records.
	 * 
	 * @return the usageRecords
	 */
	public List<UsageRecord> getUsageRecords() {
		return usageRecords;
	}

}
