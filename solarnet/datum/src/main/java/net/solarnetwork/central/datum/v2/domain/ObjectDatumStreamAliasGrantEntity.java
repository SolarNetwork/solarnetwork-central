/* ==================================================================
 * ObjectDatumStreamAliasGrantEntity.java - 31/10/2026 4:12:30 pm
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * A grant that allows the datum stream aliases of another user to resolve to
 * the datum streams of the grant owner.
 *
 * <p>
 * A grant only ever applies to the nodes its owner owns at the time of a query,
 * whatever {@code nodeIds} contains. A {@code null} {@code nodeIds} or
 * {@code sourceIds} grants any node or source.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "grantId", "created", "modified", "enabled", "allowPublic", "granteeId",
		"nodeIds", "sourceIds" })
public final class ObjectDatumStreamAliasGrantEntity
		extends BaseUserModifiableEntity<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> {

	@Serial
	private static final long serialVersionUID = 3361702405538215497L;

	private boolean allowPublic;
	private Long granteeId;
	private Long @Nullable [] nodeIds;
	private String @Nullable [] sourceIds;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param granteeId
	 *        the ID of the user granted access
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ObjectDatumStreamAliasGrantEntity(UserLongCompositePK id, Instant created, Long granteeId) {
		super(id, created);
		setGranteeId(granteeId);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param grantId
	 *        the grant ID
	 * @param created
	 *        the creation date
	 * @param granteeId
	 *        the ID of the user granted access
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ObjectDatumStreamAliasGrantEntity(Long userId, Long grantId, Instant created,
			Long granteeId) {
		this(new UserLongCompositePK(userId, grantId), created, granteeId);
	}

	@Override
	public ObjectDatumStreamAliasGrantEntity copyWithId(UserLongCompositePK id) {
		var copy = new ObjectDatumStreamAliasGrantEntity(id, created(), granteeId);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ObjectDatumStreamAliasGrantEntity entity) {
		super.copyTo(entity);
		entity.setAllowPublic(allowPublic);
		entity.setGranteeId(granteeId);
		entity.setNodeIds(nodeIds);
		entity.setSourceIds(sourceIds);
	}

	@Override
	public boolean isSameAs(@Nullable ObjectDatumStreamAliasGrantEntity other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return this.allowPublic == o.allowPublic
				&& Objects.equals(this.granteeId, o.granteeId)
				&& Arrays.equals(this.nodeIds, o.nodeIds)
				&& Arrays.equals(this.sourceIds, o.sourceIds)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObjectDatumStreamAliasGrantEntity{userId=");
		builder.append(getUserId());
		builder.append(", grantId=");
		builder.append(getGrantId());
		builder.append(", granteeId=");
		builder.append(granteeId);
		if ( nodeIds != null ) {
			builder.append(", nodeIds=");
			builder.append(Arrays.toString(nodeIds));
		}
		if ( sourceIds != null ) {
			builder.append(", sourceIds=");
			builder.append(Arrays.toString(sourceIds));
		}
		builder.append(", enabled=");
		builder.append(isEnabled());
		builder.append(", allowPublic=");
		builder.append(allowPublic);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the grant ID.
	 *
	 * @return the grant ID
	 */
	public final Long getGrantId() {
		return id().getEntityId();
	}

	/**
	 * Get the allow public mode.
	 *
	 * @return {@code true} if aliases on public nodes may resolve, which makes
	 *         the granted streams readable by anyone
	 */
	public final boolean isAllowPublic() {
		return allowPublic;
	}

	/**
	 * Set the allow public mode.
	 *
	 * @param allowPublic
	 *        {@code true} if aliases on public nodes may resolve, which makes
	 *        the granted streams readable by anyone
	 */
	public final void setAllowPublic(boolean allowPublic) {
		this.allowPublic = allowPublic;
	}

	/**
	 * Get the ID of the user granted access.
	 *
	 * @return the grantee ID
	 */
	public final Long getGranteeId() {
		return granteeId;
	}

	/**
	 * Set the ID of the user granted access.
	 *
	 * @param granteeId
	 *        the grantee ID to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@code null}
	 */
	public final void setGranteeId(Long granteeId) {
		this.granteeId = requireNonNullArgument(granteeId, "granteeId");
	}

	/**
	 * Get the granted node IDs.
	 *
	 * @return the node IDs, or {@code null} for any node
	 */
	public final Long @Nullable [] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the granted node IDs.
	 *
	 * @param nodeIds
	 *        the node IDs to set, or {@code null} for any node
	 */
	public final void setNodeIds(Long @Nullable [] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the granted source ID patterns.
	 *
	 * @return the source ID patterns, or {@code null} for any source
	 */
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the granted source ID patterns.
	 *
	 * @param sourceIds
	 *        the source ID patterns to set, or {@code null} for any source
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

}
