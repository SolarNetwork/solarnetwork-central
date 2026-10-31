/* ==================================================================
 * ObjectDatumStreamAliasGrantEntityInput.java - 31/10/2026 4:31:20 pm
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
import java.util.List;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.dao.BaseUserRelatedStdInput;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for {@link ObjectDatumStreamAliasGrantEntity}.
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumStreamAliasGrantEntityInput
		extends BaseUserRelatedStdInput<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> {

	private boolean allowPublic;

	@NotNull
	private @Nullable Long granteeId;

	@Size(min = 1, max = 1000)
	private Long @Nullable [] nodeIds;

	@Size(min = 1, max = 100)
	private @Nullable List<@NotBlank @Size(max = 64) String> sourceIds;

	/**
	 * Constructor.
	 */
	public ObjectDatumStreamAliasGrantEntityInput() {
		super();
	}

	@SuppressWarnings("NullAway")
	@Override
	public ObjectDatumStreamAliasGrantEntity toEntity(UserLongCompositePK id, Instant date) {
		ObjectDatumStreamAliasGrantEntity conf = new ObjectDatumStreamAliasGrantEntity(id, date,
				granteeId);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(ObjectDatumStreamAliasGrantEntity conf) {
		super.populateConfiguration(conf);
		conf.setAllowPublic(allowPublic);
		conf.setNodeIds(nodeIds);
		conf.setSourceIds(sourceIds != null ? sourceIds.toArray(String[]::new) : null);
	}

	/**
	 * Test if at least one of the node or source IDs is provided.
	 *
	 * <p>
	 * A grant of every stream of an account is not supported.
	 * </p>
	 *
	 * @return {@code true} if node or source IDs are provided
	 */
	@AssertTrue
	public boolean isNodeOrSourceIdsProvided() {
		return (nodeIds != null || sourceIds != null);
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
	public final @Nullable Long getGranteeId() {
		return granteeId;
	}

	/**
	 * Set the ID of the user granted access.
	 *
	 * @param granteeId
	 *        the grantee ID to set
	 */
	public final void setGranteeId(@Nullable Long granteeId) {
		this.granteeId = granteeId;
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
	public final @Nullable List<String> getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the granted source ID patterns.
	 *
	 * @param sourceIds
	 *        the source ID patterns to set, or {@code null} for any source
	 */
	public final void setSourceIds(@Nullable List<String> sourceIds) {
		this.sourceIds = sourceIds;
	}

}
