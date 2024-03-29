/* ==================================================================
 * DatumCacheSettings.java - 4/10/2021 4:50:12 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import net.solarnetwork.central.support.CacheSettings;

/**
 * Settings for a datum cache.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumCacheSettings extends CacheSettings {

	private int tempMaxEntries = 100;

	/**
	 * Constructor.
	 */
	public DatumCacheSettings() {
		super();
		setDiskPersistent(true);
	}

	/**
	 * Get the temporary max entries count.
	 * 
	 * @return the count
	 */
	public int getTempMaxEntries() {
		return tempMaxEntries;
	}

	/**
	 * Set the temporary max entries count.
	 * 
	 * @param tempMaxEntries
	 *        the count to set
	 */
	public void setTempMaxEntries(int tempMaxEntries) {
		this.tempMaxEntries = tempMaxEntries;
	}

}
