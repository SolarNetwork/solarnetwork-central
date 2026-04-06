/* ==================================================================
 * DatumAuxiliary.java - 4/11/2020 2:33:11 pm
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

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for an auxiliary datum that is part of a datum stream but managed outside
 * the stream itself.
 *
 * @author matt
 * @version 2.0
 * @since 2.8
 */
public interface DatumAuxiliary extends Identity<DatumAuxiliaryPK> {

	/**
	 * Get the primary key.
	 *
	 * @return the key
	 */
	@Override
	@NonNull
	DatumAuxiliaryPK getId();

	/**
	 * Get the unique ID of the stream this datum is a part of.
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 *
	 * @return the stream ID
	 */
	default UUID getStreamId() {
		return getId().getStreamId();
	}

	/**
	 * Get the associated timestamp of this datum.
	 *
	 * <p>
	 * This value represents the point in time the "start" sample properties
	 * associated with this datum take effect.
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 *
	 * @return the timestamp for this datum
	 */
	default Instant getTimestamp() {
		return getId().getTimestamp();
	}

	/**
	 * Get the type of auxiliary datum this instance represents.
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getKind()}.
	 * </p>
	 *
	 * @return the type
	 */
	default DatumAuxiliaryType getType() {
		return getId().getKind();
	}

	/**
	 * Get a set of datum properties that represent a "final" values to assume
	 * the datum stream had before {@link #getTimestamp()}.
	 *
	 * @return the final sample values
	 */
	@Nullable
	DatumSamples getSamplesFinal();

	/**
	 * Get a set of datum properties that represent the "start" values to assume
	 * the datum stream has starting at {@link #getTimestamp()}.
	 *
	 * @return the start sample values
	 */
	@Nullable
	DatumSamples getSamplesStart();

	/**
	 * Get optional notes or comments about this auxiliary record.
	 *
	 * @return the notes
	 */
	@Nullable
	String getNotes();

	/**
	 * Get optional metadata about this auxiliary record.
	 *
	 * @return the metadata
	 */
	@Nullable
	GeneralDatumMetadata getMetadata();

}
