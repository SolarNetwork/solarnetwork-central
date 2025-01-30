/* ==================================================================
 * SwaggerUtils.java - 31/01/2025 11:18:55 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web;

import java.util.Comparator;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Swagger documentation utilities.
 * 
 * @author matt
 * @version 1.0
 */
public class SwaggerUtils {

	/**
	 * Sort API tags by name.
	 * 
	 * <p>
	 * Names are split on the {@code -} character, and each component compared
	 * in order.
	 * </p>
	 */
	public static class ApiTagSorter implements Comparator<Tag> {

		@Override
		public int compare(Tag a, Tag b) {
			var ac = a.getName().split("-");
			var bc = b.getName().split("-");
			var acLen = ac.length;
			var bcLen = bc.length;
			for ( int i = 0, len = Math.min(acLen, bcLen); i < len; i += 1 ) {
				int res = ac[i].compareToIgnoreCase(bc[i]);
				if ( res != 0 ) {
					return res;
				} else if ( i + 1 == acLen ) {
					return -1;
				} else if ( i + 1 == bcLen ) {
					return 1;
				}
			}
			return 0;
		}

	}

}
