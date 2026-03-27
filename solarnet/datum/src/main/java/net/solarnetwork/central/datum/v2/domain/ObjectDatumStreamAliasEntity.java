/* ==================================================================
 * ObjectDatumStreamAliasEntity.java - 27/03/2026 4:30:17 pm
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

package net.solarnetwork.central.datum.v2.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Entity for an object datum stream alias.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
public final class ObjectDatumStreamAliasEntity extends BasicEntity<UUID>
		implements ObjectDatumStreamIdentity, CopyingIdentity<ObjectDatumStreamAliasEntity, UUID>,
		Differentiable<ObjectDatumStreamAliasEntity>, Serializable, Cloneable {

	@Serial
	private static final long serialVersionUID = -5549743564570744069L;

	private final ObjectDatumKind kind;
	private final Long objectId;
	private final String sourceId;
	private final Long originalObjectId;
	private final String originalSourceId;
	private @Nullable Instant modified;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param kind
	 *        the stream kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param originalObjectId
	 *        the object ID this alias points to
	 * @param originalSourceId
	 *        the source ID this alias points to
	 * @throws IllegalArgumentException
	 *         if any argument except {@code modified} is {@code null}
	 */
	public ObjectDatumStreamAliasEntity(UUID streamId, Instant created, @Nullable Instant modified,
			ObjectDatumKind kind, Long objectId, String sourceId, Long originalObjectId,
			String originalSourceId) {
		super(requireNonNullArgument(streamId, "streamId"), requireNonNullArgument(created, "created"));
		this.kind = requireNonNullArgument(kind, "kind");
		this.objectId = requireNonNullArgument(objectId, "objectId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
		this.originalObjectId = requireNonNullArgument(originalObjectId, "originalObjectId");
		this.originalSourceId = requireNonNullArgument(originalSourceId, "originalSourceId");
		this.modified = requireNonNullArgument(modified, "modified");
	}

	@Override
	public ObjectDatumStreamAliasEntity copyWithId(UUID id) {
		return new ObjectDatumStreamAliasEntity(id, created(), modified, kind, objectId, sourceId,
				originalObjectId, originalSourceId);
	}

	@Override
	public void copyTo(ObjectDatumStreamAliasEntity entity) {
		// nothing mutable to copy
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} and {@code modified} properties are
	 * not compared by this method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(@Nullable ObjectDatumStreamAliasEntity other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return kind == other.kind
				&& Objects.equals(objectId, other.objectId)
				&& Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(originalObjectId, other.originalObjectId)
				&& Objects.equals(originalSourceId, other.originalSourceId)
				;
		// @formatter:on
	}

	@Override
	public boolean differsFrom(@Nullable ObjectDatumStreamAliasEntity other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObjectDatumStreamAliasEntity{kind=");
		builder.append(kind);
		builder.append(", objectId=");
		builder.append(objectId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append(", originalObjectId=");
		builder.append(originalObjectId);
		builder.append(", originalSourceId=");
		builder.append(originalSourceId);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public UUID getStreamId() {
		return id();
	}

	@Override
	public ObjectDatumKind getKind() {
		return kind;
	}

	@Override
	public Long getObjectId() {
		return objectId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Get the modification date.
	 *
	 * @return the modified date
	 */
	public final @Nullable Instant getModified() {
		return modified;
	}

	/**
	 * Get the object ID this alias points to.
	 *
	 * @return the object ID this alias points to
	 */
	public final Long getOriginalObjectId() {
		return originalObjectId;
	}

	/**
	 * Get the source ID this alias points to.
	 *
	 * @return the source ID this alias points to
	 */
	public final String getOriginalSourceId() {
		return originalSourceId;
	}

}
