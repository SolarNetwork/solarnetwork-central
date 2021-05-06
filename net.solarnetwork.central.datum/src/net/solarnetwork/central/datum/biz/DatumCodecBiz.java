/* ==================================================================
 * DatumCodecBiz.java - 6/05/2021 9:30:59 AM
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

package net.solarnetwork.central.datum.biz;

import java.util.Map;
import net.solarnetwork.domain.Identifiable;
import net.solarnetwork.io.ObjectCodec;

/**
 * API for a service that supports datum coder/decoder configurations.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumCodecBiz extends Identifiable {

	/**
	 * Get a codec for a given source ID.
	 * 
	 * @param nodeId
	 *        the node ID to get the codec for
	 * @param sourceId
	 *        the source ID to get the codec for
	 * @param parameters
	 *        optional parameters, implementation specific
	 * @return the codec to use, or {@literal null} if none available
	 */
	ObjectCodec codecForSource(Long nodeId, String sourceId, Map<String, ?> parameters);

	/**
	 * Get a codec for a given source ID.
	 * 
	 * <p>
	 * This calls {@link #codecForSourceId(Long, String, Map)} with a
	 * {@literal null} {@code parameters} argument.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID to get the codec for
	 * @param sourceId
	 *        the source ID to get the codec for
	 * @return the codec to use, or {@literal null} if none available
	 */
	default ObjectCodec codecForSource(Long nodeId, String sourceId) {
		return codecForSource(nodeId, sourceId, null);
	}

}
