/* ==================================================================
 * ObjectDatum.java - 10/08/2023 7:06:39 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.ObjectUtils;

/**
 * A {@link net.solarnetwork.domain.datum.Datum} that also implements
 * {@link StreamDatum}.
 * 
 * <p>
 * This entity is designed to support functions that require both (object ID +
 * source ID) and stream ID metadata. <b>Note</b> that the identity of this
 * object is based on {@link net.solarnetwork.domain.datum.DatumId} only (the
 * stream ID is ignored). This entity also implements {@link UserRelatedEntity}
 * to support functions that require a user ID as well.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public final class ObjectDatum extends GeneralDatum implements StreamDatum, UserRelatedEntity<DatumId> {

	private static final long serialVersionUID = -7851022673629407913L;

	private final Long userId;
	private final UUID streamId;
	private final DatumProperties properties;

	/**
	 * Create a new instance from a datum and associated metadata.
	 * 
	 * @param datum
	 *        the datum to convert
	 * @param userId
	 *        the user ID
	 * @param id
	 *        the ID
	 * @param meta
	 *        the metadata
	 * @param ignorePropertyErrors
	 *        if {@literal true} then ignore any
	 *        {@link IllegalArgumentException} that occurs when trying to derive
	 *        a {@link DatumProperties} instance from the given datum and
	 *        metadata, and instead use an empty instance
	 * @return the datum
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	@SuppressWarnings("unchecked")
	public static ObjectDatum forDatum(net.solarnetwork.domain.datum.Datum datum, Long userId,
			DatumId id, ObjectDatumStreamMetadata meta, boolean ignorePropertyErrors) {
		DatumProperties p;
		try {
			p = DatumProperties.propertiesFrom(datum, meta);
		} catch ( IllegalArgumentException e ) {
			if ( !ignorePropertyErrors ) {
				throw e;
			}
			// just use empty props
			p = new DatumProperties();
		}
		if ( datum instanceof GeneralDatum gd ) {
			return new ObjectDatum(id, gd.getSamples(), userId, meta.getStreamId(), p);
		}
		DatumSamples s = new DatumSamples();
		DatumSamplesOperations ops = datum.asSampleOperations();
		s.setAccumulating((Map<String, Number>) ops.getSampleData(DatumSamplesType.Accumulating));
		s.setInstantaneous((Map<String, Number>) ops.getSampleData(DatumSamplesType.Instantaneous));
		s.setStatus((Map<String, Object>) ops.getSampleData(DatumSamplesType.Status));
		return new ObjectDatum(id, s, userId, meta.getStreamId(), p);
	}

	/**
	 * Create a new instance from a datum and associated metadata.
	 * 
	 * @param datum
	 *        the stream datum to convert
	 * @param userId
	 *        the user ID
	 * @param id
	 *        the ID
	 * @param meta
	 *        the metadata
	 * @return the datum
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static ObjectDatum forStreamDatum(net.solarnetwork.domain.datum.StreamDatum datum,
			Long userId, DatumId id, ObjectDatumStreamMetadata meta) {
		DatumProperties p = datum.getProperties();
		DatumSamples s = new DatumSamples();
		DatumUtils.populateGeneralDatumSamples(s, p, meta);
		return new ObjectDatum(id, s, userId, meta.getStreamId(), p);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code userId} will be unassigned.
	 * </p>
	 * 
	 * @param id
	 *        the ID
	 * @param samples
	 *        the samples
	 * @param streamId
	 *        the stream ID
	 * @param properties
	 *        the properties
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ObjectDatum(DatumId id, DatumSamples samples, UUID streamId, DatumProperties properties) {
		this(id, samples, null, streamId, properties);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param samples
	 *        the samples
	 * @param userId
	 *        the user ID
	 * @param streamId
	 *        the stream ID
	 * @param properties
	 *        the properties
	 * @throws IllegalArgumentException
	 *         if any argument except {@code userId} is {@literal null}
	 */
	public ObjectDatum(DatumId id, DatumSamples samples, Long userId, UUID streamId,
			DatumProperties properties) {
		super(id, samples);
		this.userId = userId;
		this.streamId = ObjectUtils.requireNonNullArgument(streamId, "streamId");
		this.properties = ObjectUtils.requireNonNullArgument(properties, "properties");
	}

	@Override
	public ObjectDatum clone() {
		return (ObjectDatum) super.clone();
	}

	@Override
	public Instant getCreated() {
		return getTimestamp();
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public UUID getStreamId() {
		return streamId;
	}

	@Override
	public DatumProperties getProperties() {
		return properties;
	}

}
