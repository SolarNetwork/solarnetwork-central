/* ==================================================================
 * CombiningFilter.java - 25/05/2018 12:05:09 PM
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

import net.solarnetwork.central.domain.Filter;

/**
 * Extension of {@link Filter} specific to combined results.
 * 
 * @author matt
 * @version 1.0
 * @since 1.25
 */
public interface CombiningFilter extends Filter {

	/**
	 * Get the type of combining action to perform when combining data.
	 * 
	 * @return the combining type
	 */
	CombiningType getCombiningType();

}
