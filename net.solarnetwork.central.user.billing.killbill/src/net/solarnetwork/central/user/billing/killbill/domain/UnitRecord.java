/* ==================================================================
 * UnitRecord.java - 30/08/2017 3:08:31 PM
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

import java.math.BigDecimal;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;

/**
 * A unit usage record.
 * 
 * @author matt
 * @version 1.0
 */
public class UnitRecord implements InvoiceItemUsageRecord {

	private String unitType;
	private BigDecimal amount;

	/**
	 * Get the usage unit type.
	 * 
	 * @return the unit type
	 */
	@Override
	public String getUnitType() {
		return unitType;
	}

	/**
	 * Set the usage unit type.
	 * 
	 * @param unitType
	 *        the unitType to set
	 */
	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	/**
	 * Set the amount.
	 * 
	 * @return the amount
	 */
	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * Set the amount.
	 * 
	 * @param amount
	 *        the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}
