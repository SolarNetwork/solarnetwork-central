/* ==================================================================
 * DatumExportFullConfigurations.java - 29/03/2018 5:29:21 PM
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

package net.solarnetwork.central.reg.web.domain;

import java.util.List;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;

/**
 * DTO to consolidate datum export configuration details into a single response.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumExportFullConfigurations {

	private final List<UserDatumExportConfiguration> datumExportConfigs;
	private final List<UserDataConfiguration> dataConfigs;
	private final List<UserDestinationConfiguration> destintationConfigs;
	private final List<UserOutputConfiguration> outputConfigs;

	public DatumExportFullConfigurations(List<UserDatumExportConfiguration> datumExportConfigs,
			List<UserDataConfiguration> dataConfigs,
			List<UserDestinationConfiguration> destintationConfigs,
			List<UserOutputConfiguration> outputConfigs) {
		super();
		this.datumExportConfigs = datumExportConfigs;
		this.dataConfigs = dataConfigs;
		this.destintationConfigs = destintationConfigs;
		this.outputConfigs = outputConfigs;
	}

	public List<UserDatumExportConfiguration> getDatumExportConfigs() {
		return datumExportConfigs;
	}

	public List<UserDataConfiguration> getDataConfigs() {
		return dataConfigs;
	}

	public List<UserDestinationConfiguration> getDestintationConfigs() {
		return destintationConfigs;
	}

	public List<UserOutputConfiguration> getOutputConfigs() {
		return outputConfigs;
	}

}
