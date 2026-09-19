/* ==================================================================
 * StreamDatumResult.java - 5/02/2025 11:02:07 am
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

package net.solarnetwork.central.query.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.Result;

/**
 * Result extension with support for datum stream metadata.
 * 
 * <p>
 * This class is used for API documentation only.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "success", "code", "message", "errors", "meta", "data" })
public class StreamDatumResult extends Result<StreamDatumListData> {

	private final List<ObjectDatumStreamMetadataResult> meta;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the data
	 * @param meta
	 *        the metadata
	 */
	public StreamDatumResult(StreamDatumListData data, List<ObjectDatumStreamMetadataResult> meta) {
		super(data);
		this.meta = meta;
	}

	/**
	 * Get the datum stream metadata.
	 * 
	 * @return the metadata
	 */
	public List<ObjectDatumStreamMetadataResult> getMeta() {
		return meta;
	}

}
