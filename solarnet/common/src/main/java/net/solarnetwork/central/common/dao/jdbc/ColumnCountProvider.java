/* ==================================================================
 * ColumnCountProvider.java - 6/10/2022 3:49:42 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

/**
 * API for a service that provides a SQL column count.
 * 
 * <p>
 * One use case for this API is for
 * {@link org.springframework.jdbc.core.RowMapper} implementations to implement
 * this API as well so consumers can know how many columns the mapper consumes.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ColumnCountProvider {

	/**
	 * Get the count of SQL columns referenced by this instance.
	 * 
	 * @return the column count
	 */
	int getColumnCount();

}
