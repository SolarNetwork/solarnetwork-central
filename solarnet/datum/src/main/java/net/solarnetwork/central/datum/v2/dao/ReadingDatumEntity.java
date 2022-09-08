/* ==================================================================
 * ReadingDatumEntity.java - 17/11/2020 11:31:40 am
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
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;

/**
 * Extension of {@link AggregateDatumEntity} to support reading entities.
 * 
 * @author matt
 * @version 1.2
 * @since 2.8
 */
public class ReadingDatumEntity extends AggregateDatumEntity
		implements ReadingDatum, Cloneable, Serializable {

	private static final long serialVersionUID = -78598605399387148L;

	private final Instant endTimestamp;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param aggregation
	 *        the aggregation
	 * @param endTimestamp
	 *        the end timestamp
	 * @param properties
	 *        the properties
	 * @param statistics
	 *        the statistics
	 */
	public ReadingDatumEntity(DatumPK id, Aggregation aggregation, Instant endTimestamp,
			DatumProperties properties, DatumPropertiesStatistics statistics) {
		super(id, aggregation, properties, statistics);
		this.endTimestamp = endTimestamp;
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
	 * @param endTimestamp
	 *        the end timestamp
	 * @param properties
	 *        the samples
	 * @param statistics
	 *        the statistics
	 */
	public ReadingDatumEntity(UUID streamId, Instant timestamp, Aggregation aggregation,
			Instant endTimestamp, DatumProperties properties, DatumPropertiesStatistics statistics) {
		super(streamId, timestamp, aggregation, properties, statistics);
		this.endTimestamp = endTimestamp;
	}

	@Override
	public ReadingDatumEntity clone() {
		return (ReadingDatumEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ReadingDatumEntity{");
		if ( getId() != null ) {
			builder.append("streamId=");
			builder.append(getId().getStreamId());
			builder.append(", ts=");
			builder.append(getId().getTimestamp());
		}
		if ( endTimestamp != null ) {
			builder.append(", tsEnd=");
			builder.append(endTimestamp);
		}
		if ( getAggregation() != null ) {
			builder.append(", kind=");
			builder.append(getAggregation().getKey());
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
		if ( getStatistics() != null ) {
			if ( getStatistics().getInstantaneous() != null ) {
				builder.append(", si=[");
				for ( BigDecimal[] a : getStatistics().getInstantaneous() ) {
					builder.append(Arrays.toString(a));
					builder.append(",");
				}
				builder.replace(builder.length() - 1, builder.length(), "]");
			}
			if ( getStatistics().getAccumulating() != null ) {
				builder.append(", sa=[");
				for ( BigDecimal[] a : getStatistics().getAccumulating() ) {
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
	public Instant getEndTimestamp() {
		return endTimestamp;
	}

}
