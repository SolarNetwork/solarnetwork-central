/* ==================================================================
 * StaleAggregateDatum.java - 11/04/2019 9:28:04 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseObjectEntity;

/**
 * A "stale" aggregate datum record.
 *
 * @author matt
 * @version 3.0
 * @since 1.39
 */
@JsonIgnoreProperties({ "id", "modified" })
@JsonPropertyOrder({ "nodeId", "sourceId", "startDate", "kind" })
public class StaleAggregateDatum extends BaseObjectEntity<GeneralNodeDatumKindPK> {

	@Serial
	private static final long serialVersionUID = -1038866556599452520L;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public StaleAggregateDatum(GeneralNodeDatumKindPK id) {
		super();
		setId(requireNonNullArgument(id, "id"));
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @param kind
	 *        the kind
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public StaleAggregateDatum(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("startDate") Instant created, @JsonProperty("sourceId") String sourceId,
			@JsonProperty("kind") String kind) {
		this(new GeneralNodeDatumKindPK(nodeId, created, sourceId, kind));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StaleAggregateDatum{nodeId=").append(getNodeId());
		builder.append(", sourceId=").append(getSourceId());
		builder.append(", startDate=").append(getStartDate());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public final Long getNodeId() {
		return nonnull(getId(), "ID").getNodeId();
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return nonnull(getId(), "ID").getSourceId();
	}

	/**
	 * Get the start date.
	 *
	 * @return the start date
	 */
	public final Instant getStartDate() {
		return nonnull(getId(), "ID").getCreated();
	}

	/**
	 * Get the kind.
	 *
	 * @return the kind
	 */
	public final String getKind() {
		return nonnull(getId(), "ID").getKind();
	}

}
