/* ==================================================================
 * NamedCost.java - 23/07/2020 4:49:16 PM
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
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.solarnetwork.domain.Differentiable;

/**
 * A named resource with associated cost.
 * 
 * @author matt
 * @version 1.1
 */
public class NamedCost
		implements Differentiable<NamedCost>, net.solarnetwork.central.user.billing.domain.NamedCost {

	private final String name;
	private final BigInteger quantity;
	private final BigDecimal cost;

	/**
	 * Get a "tier" cost, using string number values.
	 * 
	 * @param i
	 *        the tier number
	 * @param quantity
	 *        the quantity; will be stored as {@literal 0} if {@literal null}
	 * @param cost
	 *        the cost; will be stored as {@literal 0} if {@literal null}
	 * @return the new instance
	 */
	public static NamedCost forTier(int i, String quantity, String cost) {
		return forTier(i, new BigInteger(quantity), new BigDecimal(cost));
	}

	/**
	 * Get a "tier" cost.
	 * 
	 * @param i
	 *        the tier number
	 * @param quantity
	 *        the quantity; will be stored as {@literal 0} if {@literal null}
	 * @param cost
	 *        the cost; will be stored as {@literal 0} if {@literal null}
	 * @return the new instance
	 */
	public static NamedCost forTier(int i, BigInteger quantity, BigDecimal cost) {
		return of(String.format("Tier %d", i), quantity, cost);
	}

	/**
	 * Create a new named cost instance.
	 * 
	 * @param name
	 *        the name
	 * @param quantity
	 *        the quantity; will be stored as {@literal 0} if {@literal null}
	 * @param cost
	 *        the cost; will be stored as {@literal 0} if {@literal null}
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code name} is {@literal null}
	 */
	public static NamedCost of(String name, BigInteger quantity, BigDecimal cost) {
		return new NamedCost(name, quantity, cost);
	}

	/**
	 * Get a list of instances out of a list of named cost Maps.
	 * 
	 * @param namedCosts
	 *        the named cost Maps, each of whose keys match the properties of
	 *        this class
	 * @return the named costs, or {@literal null} if {@code namedCosts} is
	 *         {@literal null} or does not contain valid property values
	 */
	public static List<NamedCost> of(List<Map<String, ?>> namedCosts) {
		if ( namedCosts == null || namedCosts.isEmpty() ) {
			return null;
		}
		return namedCosts.stream().map(NamedCost::of).collect(Collectors.toList());
	}

	/**
	 * Get an instance out of a named cost Map.
	 * 
	 * @param namedCost
	 *        the named cost Map, whose keys match the properties of this class
	 * @return the named cost, or {@literal null} if {@code namedCosts} is
	 *         {@literal null} or does not contain valid property values
	 */
	public static NamedCost of(Map<String, ?> namedCost) {
		if ( namedCost == null ) {
			return null;
		}
		Object name = namedCost.get("name");
		Object quantity = namedCost.get("quantity");
		Object cost = namedCost.get("cost");
		if ( name != null ) {
			try {
				return new NamedCost(name.toString(),
						quantity != null ? new BigInteger(quantity.toString()) : null,
						cost != null ? new BigDecimal(cost.toString()) : null);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the name
	 * @param quantity
	 *        the quantity; will be stored as {@literal 0} if {@literal null}
	 * @param cost
	 *        the cost; will be stored as {@literal 0} if {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code name} is {@literal null}
	 */
	public NamedCost(String name, BigInteger quantity, BigDecimal cost) {
		super();
		if ( name == null ) {
			throw new IllegalArgumentException("The name argument must be provided.");
		}
		this.name = name;
		this.quantity = quantity != null ? quantity : BigInteger.ZERO;
		this.cost = cost = cost != null ? cost : BigDecimal.ZERO;
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(NamedCost other) {
		return equals(other);
	}

	@Override
	public boolean differsFrom(NamedCost other) {
		return !isSameAs(other);
	}

	/**
	 * Get a map of metadata from this instance.
	 * 
	 * @return the usage Map, whose keys match the properties of this class,
	 *         never {@literal null}
	 */
	public Map<String, Object> toMetadata() {
		Map<String, Object> result = new LinkedHashMap<>(4);
		result.put("name", name);
		result.put("quantity", quantity.toString());
		result.put("cost", cost.toPlainString());
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NamedCost{name=");
		builder.append(name);
		builder.append(", quantity=");
		builder.append(quantity);
		builder.append(", cost=");
		builder.append(cost);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(cost, name, quantity);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof NamedCost) ) {
			return false;
		}
		NamedCost other = (NamedCost) obj;
		return Objects.equals(name, other.name) && Objects.equals(quantity, other.quantity)
				&& (cost == other.cost) || (cost != null && cost.compareTo(other.cost) == 0);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BigInteger getQuantity() {
		return quantity;
	}

	@Override
	public BigDecimal getCost() {
		return cost;
	}

}
