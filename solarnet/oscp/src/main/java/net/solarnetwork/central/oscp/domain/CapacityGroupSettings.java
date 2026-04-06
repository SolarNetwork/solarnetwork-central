/* ==================================================================
 * CapacityGroupSettings.java - 10/10/2022 7:59:02 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.util.ObjectUtils;

/**
 * OSCP settings for a specific Capacity Group.
 *
 * <p>
 * The {@link UserLongCompositePK#getEntityId()} value represents the associated
 * {@link CapacityGroupConfiguration} ID.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "groupId", "userId", "created", "publishToSolarIn", "publishToSolarFlux", "nodeId",
		"sourceIdTemplate" })
public class CapacityGroupSettings extends BasicEntity<UserLongCompositePK>
		implements CopyingIdentity<CapacityGroupSettings, UserLongCompositePK>,
		Differentiable<CapacityGroupSettings>, UserRelatedEntity<UserLongCompositePK>,
		DatumPublishSettings {

	@Serial
	private static final long serialVersionUID = 2982265655654356895L;

	private @Nullable Instant modified;
	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private @Nullable Long nodeId;
	private @Nullable String sourceIdTemplate;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument except {@code created} is {@code null}
	 * @since 2.1
	 */
	public CapacityGroupSettings(UserLongCompositePK id, @Nullable Instant created) {
		super(ObjectUtils.requireNonNullArgument(id, "id"), created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param groupId
	 *        the group ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument except {@code created} is {@code null}
	 */
	@JsonCreator
	public CapacityGroupSettings(@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty(value = "groupId", required = true) Long groupId,
			@JsonProperty("created") @Nullable Instant created) {
		this(new UserLongCompositePK(userId, groupId), created);
	}

	@Override
	public CapacityGroupSettings copyWithId(UserLongCompositePK id) {
		var copy = new CapacityGroupSettings(id.getUserId(), id.getEntityId(), getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CapacityGroupSettings entity) {
		entity.setModified(modified);
		entity.setPublishToSolarIn(publishToSolarIn);
		entity.setPublishToSolarFlux(publishToSolarFlux);
		entity.setSourceIdTemplate(sourceIdTemplate);
		entity.setNodeId(nodeId);
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(@Nullable CapacityGroupSettings other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return publishToSolarIn == other.publishToSolarIn
				&& publishToSolarFlux == other.publishToSolarFlux
				&& Objects.equals(sourceIdTemplate, other.sourceIdTemplate)
				&& Objects.equals(nodeId, other.nodeId);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(@Nullable CapacityGroupSettings other) {
		return !isSameAs(other);
	}

	/**
	 * Get the group ID.
	 *
	 * <p>
	 * This is an alias for {@code getId().getEntityId()}.
	 * </p>
	 *
	 * @return the group ID
	 */
	public final Long getGroupId() {
		return id().getEntityId();
	}

	@Override
	public final Long getUserId() {
		return id().getUserId();
	}

	/**
	 * Get the last modification date.
	 *
	 * @return the modified
	 */
	public final @Nullable Instant getModified() {
		return modified;
	}

	/**
	 * SGet the last modification date.
	 *
	 * @param modified
	 *        the modified to set
	 */
	public final void setModified(@Nullable Instant modified) {
		this.modified = modified;
	}

	@Override
	public final boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 *
	 * @param publishToSolarIn
	 *        {@literal true} if data from this group should be published to
	 *        SolarIn
	 */
	public final void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	@Override
	public final boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this group should be published to
	 *        SolarFlux
	 */
	public final void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

	@Override
	public final @Nullable String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 *
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public final void setSourceIdTemplate(@Nullable String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	@Override
	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

}
