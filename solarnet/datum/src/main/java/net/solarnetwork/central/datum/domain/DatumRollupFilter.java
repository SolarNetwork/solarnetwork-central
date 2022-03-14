/* ==================================================================
 * DatumRollupFilter.java - 12/07/2018 8:00:10 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

/**
 * A filter for datum rollup queries.
 * 
 * @author matt
 * @version 1.0
 * @since 1.27
 */
public interface DatumRollupFilter {

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

}
