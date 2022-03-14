/* ==================================================================
 * NodeUsageCost.java - 22/07/2020 2:11:05 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Costs associated with node usage.
 * 
 * <p>
 * This object is used in different contexts, such as tiered pricing schedules
 * and general node usage calculations.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class NodeUsageCost {

	private BigDecimal datumPropertiesInCost;
	private BigDecimal datumDaysStoredCost;
	private BigDecimal datumOutCost;

	/**
	 * Constructor.
	 */
	public NodeUsageCost() {
		this(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This constructor converts all costs to {@link BigDecimal} values.
	 * </p>
	 * 
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 */
	public NodeUsageCost(String datumPropertiesInCost, String datumOutCost, String datumDaysStoredCost) {
		this(new BigDecimal(datumPropertiesInCost), new BigDecimal(datumOutCost),
				new BigDecimal(datumDaysStoredCost));
	}

	/**
	 * Constructor.
	 * 
	 * @param datumPropertiesInCost
	 *        the properties in cost
	 * @param datumOutCost
	 *        the datum out cost
	 * @param datumDaysStoredCost
	 *        the days stored cost
	 */
	public NodeUsageCost(BigDecimal datumPropertiesInCost, BigDecimal datumOutCost,
			BigDecimal datumDaysStoredCost) {
		super();
		setDatumPropertiesInCost(datumPropertiesInCost);
		setDatumOutCost(datumOutCost);
		setDatumDaysStoredCost(datumDaysStoredCost);
	}

	@Override
	public int hashCode() {
		return Objects.hash(datumDaysStoredCost, datumOutCost, datumPropertiesInCost);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof NodeUsageCost) ) {
			return false;
		}
		NodeUsageCost other = (NodeUsageCost) obj;
		return Objects.equals(datumDaysStoredCost, other.datumDaysStoredCost)
				&& Objects.equals(datumOutCost, other.datumOutCost)
				&& Objects.equals(datumPropertiesInCost, other.datumPropertiesInCost);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeUsage{");
		builder.append("datumPropertiesInCost=");
		builder.append(datumPropertiesInCost);
		builder.append(", datumOutCost=");
		builder.append(datumOutCost);
		builder.append(", datumDaysStoredCost=");
		builder.append(datumDaysStoredCost);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the cost of datum properties added.
	 * 
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumPropertiesInCost() {
		return datumPropertiesInCost;
	}

	/**
	 * Set the cost of datum properties added.
	 * 
	 * @param datumPropertiesInCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumPropertiesInCost(BigDecimal datumPropertiesInCost) {
		if ( datumPropertiesInCost == null ) {
			datumPropertiesInCost = BigDecimal.ZERO;
		}
		this.datumPropertiesInCost = datumPropertiesInCost;
	}

	/**
	 * Get the cost of datum stored per day (accumulating).
	 * 
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumDaysStoredCost() {
		return datumDaysStoredCost;
	}

	/**
	 * Set the cost of datum stored per day (accumulating).
	 * 
	 * @param datumDaysStoredCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumDaysStoredCost(BigDecimal datumDaysStoredCost) {
		if ( datumDaysStoredCost == null ) {
			datumDaysStoredCost = BigDecimal.ZERO;
		}
		this.datumDaysStoredCost = datumDaysStoredCost;
	}

	/**
	 * Get the cost of datum queried.
	 * 
	 * @return the cost, never {@literal null}
	 */
	public BigDecimal getDatumOutCost() {
		return datumOutCost;
	}

	/**
	 * Set the cost of datum queried.
	 * 
	 * @param datumOutCost
	 *        the cost to set (null will be stored as {@literal 0}
	 */
	public void setDatumOutCost(BigDecimal datumOutCost) {
		if ( datumOutCost == null ) {
			datumOutCost = BigDecimal.ZERO;
		}
		this.datumOutCost = datumOutCost;
	}

}
