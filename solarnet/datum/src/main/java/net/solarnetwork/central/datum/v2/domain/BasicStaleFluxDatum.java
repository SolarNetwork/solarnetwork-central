/* ==================================================================
 * BasicStaleFluxDatum.java - 9/11/2020 7:34:31 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.util.UUID;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Basic implementation of {@link StaleFluxDatum}.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public class BasicStaleFluxDatum implements StaleFluxDatum {

	private final UUID streamId;
	private final Aggregation kind;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param kind
	 *        the aggregate kind
	 */
	public BasicStaleFluxDatum(UUID streamId, Aggregation kind) {
		super();
		this.streamId = streamId;
		this.kind = kind;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicStaleFluxDatum{");
		if ( streamId != null ) {
			builder.append("streamId=");
			builder.append(streamId);
			builder.append(", ");
		}
		if ( kind != null ) {
			builder.append("kind=");
			builder.append(kind.getKey());
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public UUID getStreamId() {
		return streamId;
	}

	@Override
	public Aggregation getKind() {
		return kind;
	}

}
