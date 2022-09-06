/* ==================================================================
 * AssetInstantaneousDatumConfigurationInput.java - 6/09/2022 7:40:33 pm
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

package net.solarnetwork.central.user.oscp.domain;

import net.solarnetwork.central.oscp.domain.AssetInstantaneousDatumConfiguration;

/**
 * Input for asset instantaneous datum configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class AssetInstantaneousDatumConfigurationInput extends BaseAssetDatumConfigurationInput {

	/**
	 * Create an entity from this input.
	 * 
	 * @return the entity
	 */
	public AssetInstantaneousDatumConfiguration toEntity() {
		AssetInstantaneousDatumConfiguration conf = new AssetInstantaneousDatumConfiguration();
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Populate an entity configuration with values from this input.
	 * 
	 * @param conf
	 *        the configuration to populate
	 */
	public void populateConfiguration(AssetInstantaneousDatumConfiguration conf) {
		super.populateConfiguration(conf);

	}

}
