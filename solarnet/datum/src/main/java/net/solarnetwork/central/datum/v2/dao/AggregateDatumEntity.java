/* ==================================================================
 * AggregateDatumEntity.java - 30/10/2020 4:33:26 pm
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

package net.solarnetwork.central.datum.v2.dao;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Extension of {@link DatumEntity} to support aggregated (e.g. "rollup")
 * entities.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class AggregateDatumEntity extends DatumEntity
		implements AggregateDatum, Cloneable, Serializable {

	private static final long serialVersionUID = 7976275982566577572L;

	private final Aggregation aggregation;
	private final DatumPropertiesStatistics statistics;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param aggregation
	 *        the aggregation
	 * @param properties
	 *        the properties
	 * @param statistics
	 *        the statistics
	 */
	public AggregateDatumEntity(DatumPK id, Aggregation aggregation, DatumProperties properties,
			DatumPropertiesStatistics statistics) {
		super(id, null, properties);
		this.aggregation = aggregation;
		this.statistics = statistics;
	}

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @param properties
	 *        the samples
	 * @param statistics
	 *        the statistics
	 */
	public AggregateDatumEntity(UUID streamId, Instant timestamp, Aggregation aggregation,
			DatumProperties properties, DatumPropertiesStatistics statistics) {
		super(streamId, timestamp, null, properties);
		this.aggregation = aggregation;
		this.statistics = statistics;
	}

	@Override
	public AggregateDatumEntity clone() {
		return (AggregateDatumEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AggregateDatumEntity{");
		if ( getId() != null ) {
			builder.append("streamId=");
			builder.append(getId().getStreamId());
			builder.append(", ts=");
			builder.append(getId().getTimestamp());
		}
		if ( aggregation != null ) {
			builder.append(", kind=");
			builder.append(aggregation.getKey());
		}
		if ( getProperties() != null ) {
			if ( getProperties().getInstantaneous() != null ) {
				builder.append(", i=");
				builder.append(Arrays.toString(getProperties().getInstantaneous()));
			}
			if ( getProperties().getAccumulating() != null ) {
				builder.append(", a=");
				builder.append(Arrays.toString(getProperties().getAccumulating()));
			}
			if ( getProperties().getStatus() != null ) {
				builder.append(", s=");
				builder.append(Arrays.toString(getProperties().getStatus()));
			}
			if ( getProperties().getTags() != null ) {
				builder.append(", t=");
				builder.append(Arrays.toString(getProperties().getTags()));
			}
		}
		if ( statistics != null ) {
			if ( statistics.getInstantaneous() != null ) {
				builder.append(", si=[");
				for ( BigDecimal[] a : statistics.getInstantaneous() ) {
					builder.append(Arrays.toString(a));
					builder.append(",");
				}
				builder.replace(builder.length() - 1, builder.length(), "]");
			}
			if ( statistics.getAccumulating() != null ) {
				builder.append(", sa=[");
				for ( BigDecimal[] a : statistics.getAccumulating() ) {
					builder.append(Arrays.toString(a));
					builder.append(",");
				}
				builder.replace(builder.length() - 1, builder.length(), "]");
			}
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Get the property statistics.
	 * 
	 * @return the statistics
	 */
	@Override
	public DatumPropertiesStatistics getStatistics() {
		return statistics;
	}

}
