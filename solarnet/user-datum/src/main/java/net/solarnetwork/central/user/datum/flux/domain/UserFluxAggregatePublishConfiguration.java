/* ==================================================================
 * UserFluxAggregatePublishConfiguration.java - 24/06/2024 6:24:48 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.flux.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * SolarFlux aggregate datum stream publish configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "userId", "id", "created", "modified", "nodeIds", "sourceIds", "publish",
		"retain" })
public class UserFluxAggregatePublishConfiguration
		extends BaseUserModifiableEntity<UserFluxAggregatePublishConfiguration, UserLongCompositePK>
		implements FluxPublishSettings {

	@Serial
	private static final long serialVersionUID = -2910899403927385582L;

	private Long @Nullable [] nodeIds;
	private String @Nullable [] sourceIds;
	private boolean publish;
	private boolean retain;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserFluxAggregatePublishConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
		setEnabled(true);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param configurationId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserFluxAggregatePublishConfiguration(Long userId, Long configurationId, Instant created) {
		this(new UserLongCompositePK(userId, configurationId), created);
	}

	@Override
	public UserFluxAggregatePublishConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new UserFluxAggregatePublishConfiguration(id, created());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserFluxAggregatePublishConfiguration entity) {
		super.copyTo(entity);
		entity.setNodeIds(nodeIds);
		entity.setSourceIds(sourceIds);
		entity.setPublish(publish);
		entity.setRetain(retain);
	}

	@Override
	public boolean isSameAs(@Nullable UserFluxAggregatePublishConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Arrays.equals(this.nodeIds, o.nodeIds)
				&& Arrays.equals(this.sourceIds, o.sourceIds)
				&& publish == o.publish
				&& retain == o.retain
				;
		// @formatter:on
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	@JsonGetter("id")
	public final Long getConfigurationId() {
		return id().getEntityId();
	}

	/**
	 * Get the node IDs.
	 *
	 * @return the node IDs, or {@code null} for any node
	 */
	public final Long @Nullable [] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 *
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public final void setNodeIds(Long @Nullable [] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the source IDs.
	 *
	 * <p>
	 * Source ID Ant-style patterns are allowed.
	 * </p>
	 *
	 * @return the source IDs
	 */
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 *
	 * @param sourceIds
	 *        the source IDs or source ID Ant-style patterns to set
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

	@Override
	public final boolean isPublish() {
		return publish;
	}

	/**
	 * Set the publish mode.
	 *
	 * @param publish
	 *        {@code true} to publish messages for matching datum streams
	 */
	public final void setPublish(boolean publish) {
		this.publish = publish;
	}

	@Override
	public final boolean isRetain() {
		return retain;
	}

	/**
	 * Set the message retain flag to use.
	 *
	 * @param retain
	 *        {@code true} to set the retain flag on published messages
	 */
	public final void setRetain(boolean retain) {
		this.retain = retain;
	}

}
