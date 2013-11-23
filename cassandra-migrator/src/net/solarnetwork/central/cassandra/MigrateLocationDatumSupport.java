/* ==================================================================
 * MigrateLocationDatumSupport.java - Nov 23, 2013 5:48:20 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra;

/**
 * Base class for migrating location datum.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class MigrateLocationDatumSupport extends MigrateDatumSupport {

	public static final String DEFAULT_CQL = "INSERT INTO solardata.loc_datum (loc_id, ltype, year, ts, data_num) "
			+ "VALUES (?, ?, ?, ?, ?)";

	public MigrateLocationDatumSupport() {
		super();
		setCql(DEFAULT_CQL);
	}

}
