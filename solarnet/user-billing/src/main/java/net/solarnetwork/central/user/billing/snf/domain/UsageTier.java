/* ==================================================================
 * UsageTier.java - 27/05/2021 12:22:54 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * A usage tier.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class UsageTier {

	/**
	 * Comparator that sorts {@link UsageTier} objects by {@code key} then
	 * {@code quantity} in ascending order.
	 */
	public static final Comparator<UsageTier> SORT_BY_KEY_QUANTITY = new UsageTierQuantityComparator();

	private final String key;
	private final BigInteger quantity;
	private final BigDecimal cost;
	private final LocalDate date;

	/**
	 * Compare {@link UsageTier} instances by key then quantity in ascending
	 * order.
	 */
	public static final class UsageTierQuantityComparator implements Comparator<UsageTier> {

		@Override
		public int compare(UsageTier o1, UsageTier o2) {
			int result = o1.key.compareTo(o2.key);
			if ( result != 0 ) {
				return result;
			}
			return o1.quantity.compareTo(o2.quantity);
		}

	}

	/**
	 * Create a tier without a date.
	 * 
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param cost
	 *        the cost
	 * @return the new tier instance
	 */
	public static UsageTier tier(String key, long quantity, BigDecimal cost) {
		return new UsageTier(key, quantity, cost);
	}

	/**
	 * Create a tier without a date.
	 * 
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param cost
	 *        the cost
	 * @param date
	 *        the associated date
	 * @return the new tier instance
	 */
	public static UsageTier tier(String key, long quantity, BigDecimal cost, LocalDate date) {
		return new UsageTier(key, quantity, cost, date);
	}

	/**
	 * Create a tier without a date.
	 * 
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param cost
	 *        the cost as a string
	 * @return the new tier instance
	 */
	public static UsageTier tier(String key, long quantity, String cost) {
		return new UsageTier(key, quantity, new BigDecimal(cost));
	}

	/**
	 * Create a tier without a date.
	 * 
	 * @param key
	 *        the key
	 * @param quantity
	 *        the quantity
	 * @param cost
	 *        the cost as a string
	 * @param date
	 *        the associated date
	 * @return the new tier instance
	 */
	public static UsageTier tier(String key, long quantity, String cost, LocalDate date) {
		return new UsageTier(key, quantity, new BigDecimal(cost), date);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the tier key
	 * @param quantity
	 *        the tier quantity
	 * @param cost
	 *        the cost associated with the tier
	 */
	public UsageTier(String key, long quantity, BigDecimal cost) {
		this(key, BigInteger.valueOf(quantity), cost, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the tier key
	 * @param quantity
	 *        the tier quantity
	 * @param cost
	 *        the cost associated with the tier
	 * @param date
	 *        the associated date
	 */
	public UsageTier(String key, long quantity, BigDecimal cost, LocalDate date) {
		this(key, BigInteger.valueOf(quantity), cost, date);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the tier key
	 * @param quantity
	 *        the tier quantity
	 * @param cost
	 *        the cost associated with the tier
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code date} is {@literal null}
	 */
	public UsageTier(String key, BigInteger quantity, BigDecimal cost, LocalDate date) {
		super();
		if ( key == null ) {
			throw new IllegalArgumentException("The key argument must be provided.");
		}
		this.key = key;
		if ( quantity == null ) {
			throw new IllegalArgumentException("The quantity argument must be provided.");
		}
		this.quantity = quantity;
		if ( cost == null ) {
			throw new IllegalArgumentException("The cost argument must be provided.");
		}
		this.cost = cost;
		this.date = date;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, cost, quantity);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UsageTier) ) {
			return false;
		}
		UsageTier other = (UsageTier) obj;
		return Objects.equals(key, other.key) && Objects.equals(cost, other.cost)
				&& Objects.equals(quantity, other.quantity);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UsageTier{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( quantity != null ) {
			builder.append("quantity=");
			builder.append(quantity);
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
	 * Get the tier key (type).
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the tier quantity.
	 * 
	 * <p>
	 * The {@code quantity} value can be interpreted in different ways,
	 * depending on the context it is used in. For example, when used to detail
	 * a tiered schedule, the {@code quantity} represents the starting (or
	 * minimum) value for the tier. When used to detail an invoice item cost,
	 * the {@code quantity} represents the count of usage units included in the
	 * tier.
	 * </p>
	 * 
	 * @return the quantity
	 */
	public BigInteger getQuantity() {
		return quantity;
	}

	/**
	 * Get the tier cost.
	 * 
	 * <p>
	 * The {@code cost} value can be interpreted in different ways, depending on
	 * the context it is used in. For example, when used to detail a tiered
	 * schedule, the {@code cost} represents the cost per unit of usage for the
	 * tier. When used to detail an invoice item cost, the {@code cost}
	 * represents the cost of usage units within the tier.
	 * </p>
	 * 
	 * @return the costs
	 */
	public BigDecimal getCost() {
		return cost;
	}

	/**
	 * Get the tier date.
	 * 
	 * <p>
	 * The {@code date} might be interpreted as an effective date.
	 * </p>
	 * 
	 * @return the date, or {@literal null}
	 */
	public LocalDate getDate() {
		return date;
	}

}
