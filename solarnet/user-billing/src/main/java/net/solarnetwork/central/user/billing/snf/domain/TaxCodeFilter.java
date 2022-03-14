/* ==================================================================
 * TaxCodeFilter.java - 24/07/2020 6:29:23 AM
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.SimplePagination;

/**
 * Query filter for {@code TaxCode} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class TaxCodeFilter extends SimplePagination implements Differentiable<TaxCodeFilter> {

	private String[] zones;
	private String itemKey;
	private String code;
	private Instant date;

	/**
	 * Create a filter for a given date and list of zones.
	 * 
	 * @param date
	 *        the date
	 * @param zones
	 *        the zones
	 * @return the filter, never {@literal null}
	 */
	public static TaxCodeFilter filterFor(Instant date, String... zones) {
		TaxCodeFilter f = new TaxCodeFilter();
		f.setDate(date);
		f.setZones(zones);
		return f;
	}

	/**
	 * Test if the properties of another instance are the same as in this
	 * instance.
	 * 
	 * <p>
	 * The {@link SimplePagination} properties are not compared by this method.
	 * </p>
	 * 
	 * @param other
	 *        the other instance to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(TaxCodeFilter other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(code, other.code)
				&& Objects.equals(date, other.date)
				&& Objects.equals(itemKey, other.itemKey)
				&& Arrays.equals(zones, other.zones);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(TaxCodeFilter other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TaxCodeFilter{");
		if ( date != null ) {
			builder.append("date=");
			builder.append(date);
			builder.append(", ");
		}
		if ( zones != null ) {
			builder.append("zones=");
			builder.append(Arrays.toString(zones));
			builder.append(", ");
		}
		if ( itemKey != null ) {
			builder.append("itemKey=");
			builder.append(itemKey);
			builder.append(", ");
		}
		if ( code != null ) {
			builder.append("code=");
			builder.append(code);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the tax zones.
	 * 
	 * @return the zones
	 */
	public String[] getZones() {
		return zones;
	}

	/**
	 * Set the tax zones.
	 * 
	 * @param zones
	 *        the zones to set
	 */
	public void setZones(String[] zones) {
		this.zones = zones;
	}

	/**
	 * Get the tax zone.
	 * 
	 * <p>
	 * This returns the first-available value from the {@code zones} array.
	 * </p>
	 * 
	 * @return the zone
	 */
	public String getZone() {
		String[] zones = getZones();
		return zones != null && zones.length > 0 ? zones[0] : null;
	}

	/**
	 * Set the tax zone.
	 * 
	 * <p>
	 * This replaces the configured {@code zones} array with a single-element
	 * array if {@code zone} is not {@literal null}, otherwise sets
	 * {@code zones} to {@literal null}.
	 * </p>
	 * 
	 * @param zone
	 *        the zone to set
	 */
	public void setZone(String zone) {
		setZones(zone != null ? new String[] { zone } : null);
	}

	/**
	 * Get the item key.
	 * 
	 * @return the item key
	 */
	public String getItemKey() {
		return itemKey;
	}

	/**
	 * Set the item key.
	 * 
	 * @param itemKey
	 *        the item key to set
	 */
	public void setItemKey(String itemKey) {
		this.itemKey = itemKey;
	}

	/**
	 * Get the tax code.
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Set the tax code.
	 * 
	 * @param code
	 *        the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Get the effective date.
	 * 
	 * @return the date
	 */
	public Instant getDate() {
		return date;
	}

	/**
	 * Set the effective date.
	 * 
	 * @param date
	 *        the date to set
	 */
	public void setDate(Instant date) {
		this.date = date;
	}

}
