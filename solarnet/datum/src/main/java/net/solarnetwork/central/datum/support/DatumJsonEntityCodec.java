/* ==================================================================
 * DatumJsonEntityCodec.java - 19/03/2026 9:15:25 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import static net.solarnetwork.central.datum.v2.domain.ObjectDatumPK.unassignedStream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.support.EntityCodec;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StatTracker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service to map datum entities to/from JSON.
 *
 * <p>
 * This service is designed to support the following datum forms:
 * </p>
 *
 * <ol>
 * <li>{@link Datum} (with unassigned stream IDs)</li>
 * <li>{@link GeneralObjectDatum} (with unassigned stream IDs)</li>
 * <li>{@link StreamDatum}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class DatumJsonEntityCodec implements EntityCodec<Object, DatumPK, String> {

	/** Datum counted fields. */
	public enum DatumCount {

		/** Datum deserialized. */
		DatumDeserialized,

		/** Datum serialized. */
		DatumSerialized,

		/** Location datum deserialized. */
		LocationDatumDeserialized,

		/** Location datum serialized. */
		LocationDatumSerialized,

		/** Stream datum deserialized. */
		StreamDatumDeserialized,

		/** Stream datum serialized. */
		StreamDatumSerialized,

		;

	}

	private final StatTracker stats;
	private final JsonMapper jsonMapper;

	/**
	 * Constructor.
	 *
	 * @param stats
	 *        the stats to populate
	 * @param jsonMapper
	 *        the JSON mapper
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumJsonEntityCodec(StatTracker stats, JsonMapper jsonMapper) {
		super();
		this.stats = requireNonNullArgument(stats, "stats");
		this.jsonMapper = requireNonNullArgument(jsonMapper, "jsonMapper");
	}

	@Override
	public String serialize(Object entity) {
		final Object d = switch (entity) {
			case GeneralObjectDatum<?> gd -> {
				final GeneralObjectDatumKey id = gd.id();
				final ObjectDatumKind kind = id.getKind() == ObjectDatumKind.Location
						? ObjectDatumKind.Location
						: ObjectDatumKind.Node;
				final Long objectId = id.getObjectId();
				final String sourceId = id.getSourceId();
				final Instant ts = id.getTimestamp();
				if ( kind == ObjectDatumKind.Location ) {
					stats.increment(DatumCount.LocationDatumSerialized);
				} else {
					stats.increment(DatumCount.DatumSerialized);
				}
				// to ensure consistent JSON serialization, convert this to a GeneralDatum instance
				yield new GeneralDatum(DatumId.datumId(kind, objectId, sourceId, ts), gd.getSamples());
			}

			case StreamDatum sd -> {
				stats.increment(DatumCount.StreamDatumSerialized);
				yield sd;
			}

			case Datum cd -> {
				stats.increment(DatumCount.DatumSerialized);
				yield cd;
			}

			default -> entity;
		};

		// assume any other supported datum type
		return jsonMapper.writeValueAsString(d);
	}

	@Override
	public Object deserialize(String json) {
		final JsonNode tree = jsonMapper.readTree(json);
		final Object d = ObjectUtils.nonnull(DatumJsonUtils.parseDatum(jsonMapper, tree),
				"Datum instance");
		switch (d) {
			case GeneralObjectDatum<?> gd -> {
				final GeneralObjectDatumKey id = gd.id();
				final ObjectDatumKind kind = id.getKind() == ObjectDatumKind.Location
						? ObjectDatumKind.Location
						: ObjectDatumKind.Node;
				if ( kind == ObjectDatumKind.Location ) {
					stats.increment(DatumCount.LocationDatumDeserialized);
				} else {
					stats.increment(DatumCount.DatumDeserialized);
				}
			}

			case StreamDatum _ -> {
				stats.increment(DatumCount.StreamDatumDeserialized);
			}

			case Datum cd -> {
				final ObjectDatumKind kind = cd.getKind() == ObjectDatumKind.Location
						? ObjectDatumKind.Location
						: ObjectDatumKind.Node;
				if ( kind == ObjectDatumKind.Location ) {
					stats.increment(DatumCount.LocationDatumDeserialized);
				} else {
					stats.increment(DatumCount.DatumDeserialized);
				}
			}

			default -> {
				// do nothing
			}
		}
		return d;
	}

	@Override
	public DatumPK entityId(Object entity) {
		return switch (entity) {
			case GeneralObjectDatum<?> gd -> {
				final GeneralObjectDatumKey id = gd.id();
				final ObjectDatumKind kind = id.getKind() == ObjectDatumKind.Location
						? ObjectDatumKind.Location
						: ObjectDatumKind.Node;
				final Long objectId = id.getObjectId();
				final String sourceId = id.getSourceId();
				final Instant ts = id.getTimestamp();
				yield unassignedStream(kind, objectId, sourceId, ts);
			}

			case StreamDatum sd -> (sd instanceof DatumEntity de ? de.id()
					: new DatumPK(sd.getStreamId(), sd.getTimestamp()));

			case Datum cd -> unassignedStream(requireNonNullArgument(cd.getKind(), "datum.kind"),
					requireNonNullArgument(cd.getObjectId(), "datum.objectId"),
					requireNonNullArgument(cd.getSourceId(), "datum.sourceId"),
					requireNonNullArgument(cd.getTimestamp(), "datum.timestamp"));

			default -> throw new IllegalArgumentException("Unsupported datum type: " + entity);
		};
	}

}
