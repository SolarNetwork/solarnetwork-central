/* ==================================================================
 * DatumRollupCriteria.java - 14/11/2020 4:55:30 pm
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

package net.solarnetwork.central.datum.v2.dao;

import net.solarnetwork.central.datum.domain.DatumRollupType;

/**
 * Search criteria for datum rollup queries.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumRollupCriteria {

	/**
	 * Get the rollup type.
	 * 
	 * <p>
	 * This should always return the first value from
	 * {@link #getDatumRollupTypes()}.
	 * </p>
	 * 
	 * @return the rollup, or {@literal null} for no rollup
	 */
	DatumRollupType getDatumRollupType();

	/**
	 * Get an ordered list of rollup types.
	 * 
	 * @return the rollup values, or {@literal null} for no rollup
	 */
	DatumRollupType[] getDatumRollupTypes();

	/**
	 * Test if this filter has any datum rollup criteria.
	 * 
	 * @return {@literal true} if the datum rollup type is non-null and not
	 *         {@code None}
	 */
	default boolean hasDatumRollupCriteria() {
		return getDatumRollupType() != null && !hasDatumRollupType(DatumRollupType.None);
	}

	/**
	 * Test if a particular rollup type is present.
	 * 
	 * @param type
	 *        the type to search for
	 * @return {@literal true} if {@code type} is found
	 */
	default boolean hasDatumRollupType(DatumRollupType type) {
		DatumRollupType[] types = getDatumRollupTypes();
		if ( types == null ) {
			return false;
		}
		for ( DatumRollupType t : types ) {
			if ( t == type ) {
				return true;
			}
		}
		return false;
	}

}
