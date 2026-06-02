/* ==================================================================
 * DatumAuxiliaryFilter.java - 26/05/2026 3:45:43 pm
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

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;

/**
 * Filter API for datum auxiliary related data.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumAuxiliaryFilter extends Filter {

	/**
	 * Get an auxiliary type.
	 *
	 * @return the type
	 */
	@Nullable
	DatumAuxiliaryType getDatumAuxiliaryType();

	/**
	 * Test if a datum auxiliary type criteria is available.
	 *
	 * @return {@code true} if a {@link DatumAuxiliaryType} is specified
	 */
	default boolean hasDatumAuxiliaryTypeCriteria() {
		return getDatumAuxiliaryType() != null;
	}

}
