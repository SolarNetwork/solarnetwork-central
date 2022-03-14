/* ==================================================================
 * UsageInfo.java - 22/07/2020 8:48:28 AM
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.domain.Differentiable;

/**
 * Information about resource usage.
 * 
 * @author matt
 * @version 1.1
 */
public class UsageInfo implements InvoiceItemUsageRecord, Differentiable<UsageInfo> {

	private final String unitType;
	private final BigDecimal cost;
	private final BigDecimal amount;
	private final List<NamedCost> tiers;

	/**
	 * Get an instance out of a usage Map.
	 * 
	 * @param usage
	 *        the usage Map, whose keys match the properties of this class
	 * @return the usage, or {@literal null} if {@code usage} is {@literal null}
	 *         or does not contain valid property values
	 */
	public static UsageInfo of(Map<String, ?> usage) {
		return of(usage, null);
	}

	/**
	 * Get an instance out of a usage Map.
	 * 
	 * @param usage
	 *        the usage Map, whose keys match the properties of this class
	 * @param tiers
	 *        the usage tiers, as an array of cost objects
	 * @return the usage, or {@literal null} if {@code usage} is {@literal null}
	 *         or does not contain valid property values
	 */
	public static UsageInfo of(Map<String, ?> usage, List<Map<String, ?>> tiers) {
		if ( usage == null ) {
			return null;
		}
		Object unitType = usage.get("unitType");
		Object amount = usage.get("amount");
		Object cost = usage.get("cost");

		if ( unitType != null ) {
			List<NamedCost> namedCostTiers = NamedCost.of(tiers);
			try {
				return new UsageInfo(unitType.toString(),
						amount != null ? new BigDecimal(amount.toString()) : null,
						cost != null ? new BigDecimal(cost.toString()) : null, namedCostTiers);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code cost} will be stored as {@literal 0}.
	 * </p>
	 * 
	 * @param unitType
	 *        the usage unit type
	 * @param amount
	 *        the usage amount; will be stored as {@literal 0} if
	 *        {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code unitType} is {@literal null}
	 * @since 1.1
	 */
	public UsageInfo(String unitType, BigDecimal amount) {
		this(unitType, amount, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param unitType
	 *        the usage unit type
	 * @param amount
	 *        the usage amount; will be stored as {@literal 0} if
	 *        {@literal null}
	 * @param cost
	 *        the usage cost, in the currency of the account or invoice this
	 *        usage is associated with; will be stored as {@literal 0} if
	 *        {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code unitType} is {@literal null}
	 */
	public UsageInfo(String unitType, BigDecimal amount, BigDecimal cost) {
		this(unitType, amount, cost, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param unitType
	 *        the usage unit type
	 * @param amount
	 *        the usage amount; will be stored as {@literal 0} if
	 *        {@literal null}
	 * @param cost
	 *        the usage cost, in the currency of the account or invoice this
	 *        usage is associated with; will be stored as {@literal 0} if
	 *        {@literal null}
	 * @param tiers
	 *        the named cost tiers
	 * @throws IllegalArgumentException
	 *         if {@code unitType} is {@literal null}
	 * @since 1.1
	 */
	public UsageInfo(String unitType, BigDecimal amount, BigDecimal cost, List<NamedCost> tiers) {
		super();
		if ( unitType == null ) {
			throw new IllegalArgumentException("The unitType argument must be provided.");
		}
		this.unitType = unitType;
		this.amount = amount != null ? amount : BigDecimal.ZERO;
		this.cost = cost != null ? cost : BigDecimal.ZERO;
		this.tiers = tiers;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UsageInfo{");
		if ( unitType != null ) {
			builder.append("unitType=");
			builder.append(unitType);
			builder.append(", ");
		}
		if ( amount != null ) {
			builder.append("amount=");
			builder.append(amount);
			builder.append(", ");
		}
		if ( cost != null ) {
			builder.append("cost=");
			builder.append(cost);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get a map of metadata from this instance.
	 * 
	 * @return the usage Map, whose keys match the properties of this class,
	 *         never {@literal null}
	 */
	public Map<String, Object> toMetadata() {
		Map<String, Object> result = new LinkedHashMap<>(4);
		result.put("unitType", unitType);
		result.put("amount", amount.toString());
		result.put("cost", cost.toString());
		return result;
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 * 
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
	 * </p>
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(UsageInfo other) {
		return equals(other);
	}

	@Override
	public boolean differsFrom(UsageInfo other) {
		return !isSameAs(other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount, cost, unitType);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UsageInfo) ) {
			return false;
		}
		UsageInfo other = (UsageInfo) obj;
		return Objects.equals(amount, other.amount) && Objects.equals(cost, other.cost)
				&& Objects.equals(unitType, other.unitType);
	}

	@Override
	public String getUnitType() {
		return unitType;
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	@Override
	public BigDecimal getCost() {
		return cost;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<net.solarnetwork.central.user.billing.domain.NamedCost> getUsageTiers() {
		return (List) tiers;
	}

}
