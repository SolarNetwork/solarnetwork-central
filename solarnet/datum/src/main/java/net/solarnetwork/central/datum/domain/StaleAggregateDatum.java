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

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseObjectEntity;

/**
 * A "stale" aggregate datum record.
 * 
 * @author matt
 * @version 2.0
 * @since 1.39
 */
@JsonIgnoreProperties({ "id", "modified" })
@JsonPropertyOrder({ "nodeId", "sourceId", "startDate", "kind", "created" })
public class StaleAggregateDatum extends BaseObjectEntity<GeneralNodeDatumKindPK> {

	private static final long serialVersionUID = -1038866556599452520L;

	public StaleAggregateDatum() {
		super();
		setId(new GeneralNodeDatumKindPK());
	}

	private GeneralNodeDatumKindPK getOrCreateId() {
		GeneralNodeDatumKindPK pk = getId();
		if ( pk == null ) {
			pk = new GeneralNodeDatumKindPK();
			setId(pk);
		}
		return pk;
	}

	public Long getNodeId() {
		GeneralNodeDatumKindPK pk = getId();
		return (pk != null ? pk.getNodeId() : null);
	}

	public void setNodeId(Long nodeId) {
		getOrCreateId().setNodeId(nodeId);
	}

	public String getSourceId() {
		GeneralNodeDatumKindPK pk = getId();
		return (pk != null ? pk.getSourceId() : null);
	}

	public void setSourceId(String sourceId) {
		getOrCreateId().setSourceId(sourceId);
	}

	public Instant getStartDate() {
		GeneralNodeDatumKindPK pk = getId();
		return (pk != null ? pk.getCreated() : null);
	}

	public void setStartDate(Instant date) {
		getOrCreateId().setCreated(date);
	}

	public String getKind() {
		GeneralNodeDatumKindPK pk = getId();
		return (pk != null ? pk.getKind() : null);
	}

	public void setKind(String kind) {
		getOrCreateId().setKind(kind);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StaleAggregateDatum{");
		builder.append("nodeId=").append(getNodeId());
		builder.append(", sourceId=").append(getSourceId());
		builder.append(", startDate=").append(getStartDate());
		builder.append(", created=").append(getCreated());
		builder.append("}");
		return builder.toString();
	}

}
