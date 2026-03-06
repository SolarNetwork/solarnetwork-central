/* ==================================================================
 * DatumAuxiliaryEntity.java - 4/11/2020 2:07:44 pm
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicIdentity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * An auxiliary datum that is part of a datum stream but managed outside the
 * stream itself.
 *
 * @author matt
 * @version 2.1
 * @since 2.8
 */
public class DatumAuxiliaryEntity extends BasicIdentity<DatumAuxiliaryPK>
		implements DatumAuxiliary, Entity<DatumAuxiliaryPK>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = -3932363566089537924L;

	private final Instant updated;
	private final DatumSamples samplesFinal;
	private final DatumSamples samplesStart;
	private final @Nullable String notes;
	private final @Nullable GeneralDatumMetadata metadata;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param updated
	 *        the date the record was last updated
	 * @param samplesFinal
	 *        the final sample properties
	 * @param samplesStart
	 *        the start sample properties
	 * @param notes
	 *        the notes
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if any argument except {@code notes} or {@code metadata} is
	 *         {@code null}
	 */
	public DatumAuxiliaryEntity(DatumAuxiliaryPK id, Instant updated, DatumSamples samplesFinal,
			DatumSamples samplesStart, @Nullable String notes, @Nullable GeneralDatumMetadata metadata) {
		super(requireNonNullArgument(id, "id"));
		this.updated = requireNonNullArgument(updated, "updated");
		this.samplesFinal = requireNonNullArgument(samplesFinal, "samplesFinal");
		this.samplesStart = requireNonNullArgument(samplesStart, "samplesStart");
		this.notes = notes;
		this.metadata = metadata;
	}

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param kind
	 *        the auxiliary type
	 * @param updated
	 *        the date the record was last updated
	 * @param samplesFinal
	 *        the final sample properties
	 * @param samplesStart
	 *        the start sample properties
	 * @param notes
	 *        the notes
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if any argument except {@code notes} or {@code metadata} is
	 *         {@code null}
	 */
	public DatumAuxiliaryEntity(UUID streamId, Instant timestamp, DatumAuxiliaryType kind,
			Instant updated, DatumSamples samplesFinal, DatumSamples samplesStart,
			@Nullable String notes, @Nullable GeneralDatumMetadata metadata) {
		this(new DatumAuxiliaryPK(streamId, timestamp, kind), updated, samplesFinal, samplesStart, notes,
				metadata);
	}

	@Override
	public DatumAuxiliaryEntity clone() {
		return (DatumAuxiliaryEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DatumAuxiliaryEntity{");
		if ( getId() != null ) {
			builder.append("streamId=");
			builder.append(getId().getStreamId());
			builder.append(", ts=");
			builder.append(getId().getTimestamp());
			builder.append(", kind=");
			builder.append(getId().getKind());
		}
		if ( samplesFinal != null ) {
			if ( samplesFinal.getAccumulating() != null ) {
				builder.append(", af=");
				builder.append(samplesFinal.getAccumulating());
			}
		}
		if ( samplesStart != null ) {
			if ( samplesStart.getAccumulating() != null ) {
				builder.append(", as=");
				builder.append(samplesStart.getAccumulating());
			}
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public final Instant getCreated() {
		return getTimestamp();
	}

	/**
	 * Get the last updated date.
	 *
	 * @return the updated date
	 */
	public final Instant getUpdated() {
		return updated;
	}

	/**
	 * Get the datum stream ID.
	 *
	 * <p>
	 * This method is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 *
	 * @return the stream ID
	 */
	@Override
	public final UUID getStreamId() {
		return nonnull(getId(), "ID").getStreamId();
	}

	/**
	 * Get the datum timestamp.
	 *
	 * <p>
	 * The {@link #getCreated()} method is an alias for this method. This method
	 * is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 *
	 * @return the datum timestamp
	 */
	@Override
	public final Instant getTimestamp() {
		return nonnull(getId(), "ID").getTimestamp();
	}

	/**
	 * Get the datum auxiliary type.
	 *
	 * <p>
	 * This method is a shortcut for {@code getId().getKind()}.
	 * </p>
	 *
	 * @return the datum auxiliary type
	 */
	@Override
	public final DatumAuxiliaryType getType() {
		return nonnull(getId(), "ID").getKind();
	}

	@Override
	public final DatumSamples getSamplesFinal() {
		return samplesFinal;
	}

	@Override
	public final DatumSamples getSamplesStart() {
		return samplesStart;
	}

	@Override
	public final @Nullable String getNotes() {
		return notes;
	}

	@Override
	public final @Nullable GeneralDatumMetadata getMetadata() {
		return metadata;
	}

}
