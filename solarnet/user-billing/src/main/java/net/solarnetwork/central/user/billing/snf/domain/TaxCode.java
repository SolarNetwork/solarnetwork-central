/* ==================================================================
 * TaxCode.java - 24/07/2020 6:13:23 AM
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
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * Tax code entity, which defines a tax rate to apply to a given item key.
 * 
 * @author matt
 * @version 1.1
 */
public class TaxCode extends BasicLongEntity implements Differentiable<TaxCode> {

	private static final long serialVersionUID = 3366589227496726636L;

	private final String zone;
	private final String itemKey;
	private final String code;
	private final BigDecimal rate;
	private final Instant validFrom;
	private final Instant validTo;

	/**
	 * Constructor.
	 * 
	 * @param zone
	 *        the tax zone the tax applies to, e.g. "NZ"
	 * @param itemKey
	 *        the item key the tax applies to, e.g. "datum-posted"
	 * @param code
	 *        the tax code, e.g. "GST"
	 * @param rate
	 *        the tax rate
	 * @param validFrom
	 *        the minimum date from which this tax rate is applicable
	 * @param validTo
	 *        the maximum date to which this tax rate is applicable, or
	 *        {@literal null} for "forever"
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code validTo} is {@literal null}
	 */
	public TaxCode(String zone, String itemKey, String code, BigDecimal rate, Instant validFrom,
			Instant validTo) {
		this(null, Instant.now(), zone, itemKey, code, rate, validFrom, validTo);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param zone
	 *        the tax zone the tax applies to, e.g. "NZ"
	 * @param itemKey
	 *        the item key the tax applies to, e.g. "datum-posted"
	 * @param code
	 *        the tax code, e.g. "GST"
	 * @param rate
	 *        the tax rate
	 * @param validFrom
	 *        the minimum date from which this tax rate is applicable
	 * @param validTo
	 *        the maximum date to which this tax rate is applicable, or
	 *        {@literal null} for "forever"
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code id}, {@code created}, or
	 *         {@code validTo} are {@literal null}
	 */
	public TaxCode(Long id, Instant created, String zone, String itemKey, String code, BigDecimal rate,
			Instant validFrom, Instant validTo) {
		super(id, created);
		if ( zone == null ) {
			throw new IllegalArgumentException("The zone argument must be provided.");
		}
		this.zone = zone;
		if ( itemKey == null ) {
			throw new IllegalArgumentException("The itemKey argument must be provided.");
		}
		this.itemKey = itemKey;
		if ( code == null ) {
			throw new IllegalArgumentException("The code argument must be provided.");
		}
		this.code = code;
		if ( rate == null ) {
			throw new IllegalArgumentException("The rate argument must be provided.");
		}
		this.rate = rate;
		if ( validFrom == null ) {
			throw new IllegalArgumentException("The validFrom argument must be provided.");
		}
		this.validFrom = validFrom;
		this.validTo = validTo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TaxCode{zone=");
		builder.append(zone);
		builder.append(", itemKey=");
		builder.append(itemKey);
		builder.append(", code=");
		builder.append(code);
		builder.append(", rate=");
		builder.append(rate);
		builder.append(", validFrom=");
		builder.append(validFrom);
		builder.append(", validTo=");
		builder.append(validTo);
		builder.append("}");
		return builder.toString();
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
	public boolean isSameAs(TaxCode other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(code, other.code)
				&& Objects.equals(itemKey, other.itemKey)
				&& (rate == other.rate) || (rate != null && rate.compareTo(other.rate) == 0)
				&& Objects.equals(validFrom, other.validFrom)
				&& Objects.equals(validTo, other.validTo);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(TaxCode other) {
		return !isSameAs(other);
	}

	/**
	 * Get the tax zone.
	 * 
	 * @return the zone, never {@literal null}
	 */
	public String getZone() {
		return zone;
	}

	/**
	 * Get the item key.
	 * 
	 * @return the item key, never {@literal null}
	 */
	public String getItemKey() {
		return itemKey;
	}

	/**
	 * Get the tax code.
	 * 
	 * @return the code, never {@literal null}
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Get the tax rate.
	 * 
	 * @return the rate, never {@literal null}
	 */
	public BigDecimal getRate() {
		return rate;
	}

	/**
	 * Get the minimum date from which this tax code is applicable.
	 * 
	 * @return the date, never {@literal null}
	 */
	public Instant getValidFrom() {
		return validFrom;
	}

	/**
	 * Get the maximum date to which this tax code is applicable.
	 * 
	 * @return the date, or {@literal null} if the tax code is applicable
	 *         "forever"
	 */
	public Instant getValidTo() {
		return validTo;
	}

}
