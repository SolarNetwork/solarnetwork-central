/* ==================================================================
 * CapacityForecast.java - 24/08/2022 11:41:14 am
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

import java.util.List;
import oscp.v20.UpdateGroupCapacityForecast;

/**
 * A capacity forecast record of capacity amount blocks.
 * 
 * @author matt
 * @version 1.0
 */
public record CapacityForecast(ForecastType type, List<TimeBlockAmount> blocks) {

	/**
	 * Get an OSCP 2.0 value for this instance.
	 * 
	 * @param groupId
	 *        the group ID to use for the forecast
	 * @return the OSCP 2.0 group capacity value
	 */
	public UpdateGroupCapacityForecast toOscp20GroupCapacityValue(String groupId) {
		var result = new UpdateGroupCapacityForecast();
		result.setGroupId(groupId);
		if ( type != null ) {
			result.setType(type.toOscp20CapacityValue());
		}
		if ( blocks != null ) {
			result.setForecastedBlocks(
					blocks.stream().map(TimeBlockAmount::toOscp20ForecastValue).toList());
		}
		return result;
	}

	/**
	 * Get an instance for an OSCP 2.0 value.
	 * 
	 * <p>
	 * Note that {@link UpdateGroupCapacityForecast#getGroupId()} is not part of
	 * the returned object!
	 * </p>
	 * 
	 * @param update
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static CapacityForecast forOscp20Value(UpdateGroupCapacityForecast update) {
		if ( update == null ) {
			return null;
		}
		ForecastType type = (update.getType() != null ? ForecastType.forOscp20Value(update.getType())
				: null);
		List<TimeBlockAmount> blocks = (update.getForecastedBlocks() != null ? update
				.getForecastedBlocks().stream().map(TimeBlockAmount::forOscp20ForecastValue).toList()
				: null);
		return new CapacityForecast(type, blocks);
	}

}
