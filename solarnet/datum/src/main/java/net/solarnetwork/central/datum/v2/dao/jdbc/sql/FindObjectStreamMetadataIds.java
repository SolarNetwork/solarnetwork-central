/* ==================================================================
 * FindObjectStreamMetadata.java - 1/05/2022 7:47:21 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import java.sql.Connection;
import java.util.UUID;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate dynamic SQL for a "find stream metadata IDs" query.
 * 
 * <p>
 * Note that {@link #createPreparedStatement(Connection)} does not set any
 * parameter values in the prepared statement.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class FindObjectStreamMetadataIds implements SqlProvider {

	/**
	 * The SQL for finding metadata ID values for a single stream ID.
	 */
	public static final String FIND_METADATA_IDS_FOR_STREAM_ID;
	static {
		FIND_METADATA_IDS_FOR_STREAM_ID = "SELECT stream_id, obj_id, source_id, kind FROM solardatm.find_metadata_for_stream(?)";
	}

	/**
	 * Constructor.
	 */
	public FindObjectStreamMetadataIds(UUID[] streamIds) {
		super();
		ObjectUtils.requireNonEmptyArgument(streamIds, "streamIds");
	}

	@Override
	public String getSql() {
		return FIND_METADATA_IDS_FOR_STREAM_ID;
	}

}
