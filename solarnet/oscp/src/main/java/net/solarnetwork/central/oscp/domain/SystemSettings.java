/* ==================================================================
 * SystemSettings.java - 19/08/2022 2:29:05 pm
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

package net.solarnetwork.central.oscp.domain;

import static java.util.stream.Collectors.toSet;
import java.util.Set;
import oscp.v20.MeasurementConfiguration;
import oscp.v20.RequiredBehaviour;

/**
 * System settings.
 * 
 * @param heartbeatSeconds
 *        the heartbeat seconds
 * @param measurementStyles
 *        the measurement styles
 * @author matt
 * @version 1.0
 */
public record SystemSettings(Integer heartbeatSeconds, Set<MeasurementStyle> measurementStyles) {

	/**
	 * Get an OSCP 2.0 value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public RequiredBehaviour toOscp20Value() {
		Double heartbeat = null;
		if ( heartbeatSeconds != null ) {
			heartbeat = heartbeatSeconds.doubleValue();
		}
		Set<MeasurementConfiguration> confs = null;
		if ( measurementStyles != null ) {
			confs = measurementStyles.stream().map(MeasurementStyle::toOscp20Value).collect(toSet());
		}
		RequiredBehaviour result = new RequiredBehaviour(confs);
		result.setHeartbeatInterval(heartbeat);
		return result;
	}

	/**
	 * Get an instance for an OSCP 2.0 value.
	 * 
	 * @param direction
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static SystemSettings forOscp20Value(RequiredBehaviour behaviour) {
		Integer heartbeatSeconds = null;
		if ( behaviour.getHeartbeatInterval() != null ) {
			heartbeatSeconds = behaviour.getHeartbeatInterval().intValue();
		}
		Set<MeasurementStyle> styles = null;
		if ( behaviour.getMeasurementConfiguration() != null ) {
			styles = behaviour.getMeasurementConfiguration().stream()
					.map(MeasurementStyle::forOscp20Value).collect(toSet());
		}
		return new SystemSettings(heartbeatSeconds, styles);
	}

}
