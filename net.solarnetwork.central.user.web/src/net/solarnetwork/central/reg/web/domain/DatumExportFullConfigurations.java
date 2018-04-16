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
import net.solarnetwork.central.datum.domain.export.DataConfiguration;
import net.solarnetwork.central.datum.domain.export.DestinationConfiguration;
import net.solarnetwork.central.datum.domain.export.OutputConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * DTO to consolidate datum export configuration details into a single response.
 * 
 * @author matt
 * @version 1.0
 * @since 1.26
 */
public class DatumExportFullConfigurations {

	private final List<UserDatumExportConfiguration> datumExportConfigs;
	private final List<? extends DataConfiguration> dataConfigs;
	private final List<? extends DestinationConfiguration> destintationConfigs;
	private final List<? extends OutputConfiguration> outputConfigs;

	public DatumExportFullConfigurations(List<UserDatumExportConfiguration> datumExportConfigs,
			List<? extends DataConfiguration> dataConfigs,
			List<? extends DestinationConfiguration> destintationConfigs,
			List<? extends OutputConfiguration> outputConfigs) {
		super();
		this.datumExportConfigs = datumExportConfigs;
		this.dataConfigs = dataConfigs;
		this.destintationConfigs = destintationConfigs;
		this.outputConfigs = outputConfigs;
	}

	public List<UserDatumExportConfiguration> getDatumExportConfigs() {
		return datumExportConfigs;
	}

	public List<? extends DataConfiguration> getDataConfigs() {
		return dataConfigs;
	}

	public List<? extends DestinationConfiguration> getDestintationConfigs() {
		return destintationConfigs;
	}

	public List<? extends OutputConfiguration> getOutputConfigs() {
		return outputConfigs;
	}

}
