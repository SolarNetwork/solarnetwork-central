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
import io.swagger.v3.oas.annotations.media.Schema;
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
public class StreamDatumResult extends Result<List<List<Number>>> {

	private final List<ObjectDatumStreamMetadataResult> meta;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the data
	 * @param meta
	 *        the metadata
	 */
	public StreamDatumResult(List<List<Number>> data, List<ObjectDatumStreamMetadataResult> meta) {
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

	@Schema(description = """
			The list of stream data values. Each list item as an array of values, in the following order:

			* **meta index** - the `0`-based offset within the `meta` list, representing the datum stream metadata associated with this datum
			* **timestamp** - the datum timestamp, as a millisecond epoch or an array of `[start, end]` millisecond epoch values for aggregate results
			* **instantaneous property values** - ordered by the `meta.i` array of the associated datum stream metadata
			* **accumulating property values** - ordered by the `meta.a` array of the associated datum stream metadata
			* **status property values** - string status values ordered by the `meta.s` array of the associated datum stream metadata
			* **tags** - any tags associated with the datum

			**Note** that the data is declared as `number[][]` but really the type of status property values is `string`, and the **timestamp**
			value can also be represented as an array of `[start, end]` millisecond epoch values for aggregate results.
			""")
	@Override
	public List<List<Number>> getData() {
		return super.getData();
	}

}
