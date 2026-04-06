/* ==================================================================
 * ObjectDatumStreamAliasEntityInput.java - 30/03/2026 7:28:16 am
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

package net.solarnetwork.central.user.datum.stream.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * DTO for {@link ObjectDatumStreamAliasEntity}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class ObjectDatumStreamAliasEntityInput {

	@NotNull
	private @Nullable Long objectId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String sourceId;

	@NotNull
	private @Nullable Long originalObjectId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String originalSourceId;

	/**
	 * Constructor.
	 */
	public ObjectDatumStreamAliasEntityInput() {
		super();
	}

	/**
	 * Create an entity from the input properties and a given primary key.
	 *
	 * @param id
	 *        the primary key to use
	 * @param date
	 *        the creation date to use
	 * @return the new entity
	 */
	@SuppressWarnings("NullAway")
	public ObjectDatumStreamAliasEntity toEntity(UUID id, Instant date) {
		ObjectDatumStreamAliasEntity conf = new ObjectDatumStreamAliasEntity(id, date, Instant.now(),
				ObjectDatumKind.Node, objectId, sourceId, originalObjectId, originalSourceId);
		return conf;
	}

	/**
	 * Get the alias object ID.
	 * 
	 * @return the object ID
	 */
	public final @Nullable Long getObjectId() {
		return objectId;
	}

	/**
	 * Set the alias object ID.
	 * 
	 * @param objectId
	 *        the ID to set
	 */
	public final void setObjectId(@Nullable Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * Get the alias source ID.
	 * 
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the alias source ID.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the original object ID.
	 * 
	 * @return the original object ID
	 */
	public final @Nullable Long getOriginalObjectId() {
		return originalObjectId;
	}

	/**
	 * Set the original object ID.
	 * 
	 * @param originalObjectId
	 *        the original object ID to set
	 */
	public final void setOriginalObjectId(@Nullable Long originalObjectId) {
		this.originalObjectId = originalObjectId;
	}

	/**
	 * Get the original source ID.
	 * 
	 * @return the original source ID
	 */
	public final @Nullable String getOriginalSourceId() {
		return originalSourceId;
	}

	/**
	 * Set the original source ID.
	 * 
	 * @param originalSourceId
	 *        the original source ID to set
	 */
	public final void setOriginalSourceId(@Nullable String originalSourceId) {
		this.originalSourceId = originalSourceId;
	}

}
