/* ==================================================================
 * PropertyNameFilter.java - 29/01/2026 11:36:28 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.Filter;

/**
 * Filter API for property names.
 *
 * @author matt
 * @version 1.0
 */
public interface PropertyNameFilter extends Filter {

	/**
	 * Get an array of property names.
	 *
	 * @return array of properties, or {@code null}
	 */
	String[] getPropertyNames();

	/**
	 * Get the first available property name.
	 *
	 * @return the first available property name, or {@code null}
	 */
	default String getPropertyName() {
		final String[] names = getPropertyNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

}
