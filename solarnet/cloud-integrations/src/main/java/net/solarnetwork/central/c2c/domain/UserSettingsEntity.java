/* ==================================================================
 * UserSettingsEntity.java - 28/10/2024 6:59:38 am
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

package net.solarnetwork.central.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * Account-wide cloud integrations settings.
 *
 * <p>
 * The {@link #getId()} value represents the SolarNet user ID.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "created", "modified", "publishToSolarIn", "publishToSolarFlux" })
public final class UserSettingsEntity extends BasicLongEntity
		implements Differentiable<UserSettingsEntity>, UserRelatedEntity<Long>,
		CopyingIdentity<Long, UserSettingsEntity>, Serializable, Cloneable, CloudDatumStreamSettings {

	@Serial
	private static final long serialVersionUID = 2463852724878062639L;

	private Instant modified;
	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = false;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSettingsEntity(Long userId, Instant created) {
		super(userId, created);
	}

	@Override
	public UserSettingsEntity copyWithId(Long id) {
		var copy = new UserSettingsEntity(requireNonNullArgument(id, "id"), getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserSettingsEntity entity) {
		entity.setModified(modified);
		entity.setPublishToSolarIn(publishToSolarIn);
		entity.setPublishToSolarFlux(publishToSolarFlux);
	}

	@Override
	public boolean differsFrom(UserSettingsEntity other) {
		return !isSameAs(other);
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
	public boolean isSameAs(UserSettingsEntity other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return publishToSolarIn == other.publishToSolarIn
				&& publishToSolarFlux == other.publishToSolarFlux
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudDatumStream{userId=");
		builder.append(getUserId());
		builder.append(", publishToSolarIn=");
		builder.append(publishToSolarIn);
		builder.append(", publishToSolarFlux=");
		builder.append(publishToSolarFlux);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public UserSettingsEntity clone() {
		return (UserSettingsEntity) super.clone();
	}

	/**
	 * Get the user ID.
	 *
	 * <p>
	 * This is an alias for {@link #getId()}.
	 * </p>
	 *
	 * @return the user ID
	 */
	@Override
	public Long getUserId() {
		return getId();
	}

	/**
	 * Get the modification date.
	 *
	 * @return the modified date
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 *
	 * @param modified
	 *        the modified date to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	@Override
	public boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 *
	 * @param publishToSolarIn
	 *        {@literal true} if data should be published to SolarIn
	 */
	public void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	@Override
	public boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data should be published to SolarFlux
	 */
	public void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

}
