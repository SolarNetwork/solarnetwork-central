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

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
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
 * @version 2.0
 * @since 2.8
 */
public class DatumAuxiliaryEntity extends BasicIdentity<DatumAuxiliaryPK>
		implements DatumAuxiliary, Entity<DatumAuxiliaryPK>, Cloneable, Serializable {

	private static final long serialVersionUID = -3932363566089537924L;

	private final Instant updated;
	private final DatumSamples samplesFinal;
	private final DatumSamples samplesStart;
	private final String notes;
	private final GeneralDatumMetadata metadata;

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
	 */
	public DatumAuxiliaryEntity(DatumAuxiliaryPK id, Instant updated, DatumSamples samplesFinal,
			DatumSamples samplesStart, String notes, GeneralDatumMetadata metadata) {
		super(id);
		this.updated = updated;
		this.samplesFinal = samplesFinal;
		this.samplesStart = samplesStart;
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
	 */
	public DatumAuxiliaryEntity(UUID streamId, Instant timestamp, DatumAuxiliaryType kind,
			Instant updated, DatumSamples samplesFinal, DatumSamples samplesStart, String notes,
			GeneralDatumMetadata metadata) {
		this(new DatumAuxiliaryPK(streamId, timestamp, kind), updated, samplesFinal, samplesStart, notes,
				metadata);
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
	public boolean hasId() {
		DatumAuxiliaryPK id = getId();
		return (id != null && id.getStreamId() != null && id.getTimestamp() != null
				&& id.getKind() != null);
	}

	@Override
	public Instant getCreated() {
		return getTimestamp();
	}

	/**
	 * Get the last updated date.
	 * 
	 * @return the updated date
	 */
	public Instant getUpdated() {
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
	public UUID getStreamId() {
		DatumAuxiliaryPK id = getId();
		return (id != null ? id.getStreamId() : null);
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
	public Instant getTimestamp() {
		DatumAuxiliaryPK id = getId();
		return (id != null ? id.getTimestamp() : null);
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
	public DatumAuxiliaryType getType() {
		DatumAuxiliaryPK id = getId();
		return (id != null ? id.getKind() : null);
	}

	@Override
	public DatumSamples getSamplesFinal() {
		return samplesFinal;
	}

	@Override
	public DatumSamples getSamplesStart() {
		return samplesStart;
	}

	@Override
	public String getNotes() {
		return notes;
	}

	@Override
	public GeneralDatumMetadata getMetadata() {
		return metadata;
	}

}
