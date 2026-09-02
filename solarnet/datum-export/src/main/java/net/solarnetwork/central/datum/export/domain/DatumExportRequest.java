/* ==================================================================
 * DatumExportRequest.java - 21/04/2018 2:36:13 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.domain.Identity;

/**
 * API for a {@link Configuration} associated with an identity.
 *
 * @author matt
 * @version 2.2
 */
public interface DatumExportRequest extends Identity<UUID> {

	@Override
	@NonNull
	UUID getId();

	/**
	 * Test if the ID is assigned.
	 *
	 * @return {@literal true} if the ID value is assigned, {@literal false} if
	 *         it is considered "not a value"
	 * @since 2.2
	 */
	default boolean idIsAssigned() {
		try {
			return EntityConstants.isAssigned(getId());
		} catch ( IllegalStateException e ) {
			return false;
		}
	}

	/**
	 * Get the ID, presumed non-null.
	 *
	 * <p>
	 * This method is a nullness-check shortcut, for example to be used after
	 * {@link #idIsAssigned()} returns {@code true}.
	 * </p>
	 *
	 * @return the ID (presumed non-null)
	 * @since 2.2
	 */
	@Override
	@SuppressWarnings("NullAway")
	default UUID id() {
		return getId();
	}

	/**
	 * Get the user ID associated with this job, if any.
	 *
	 * @return the user ID, or {@code null} if none
	 * @since 2.1
	 */
	default @Nullable Long getUserId() {
		return null;
	}

	/**
	 * Test if the user ID is assigned.
	 *
	 * @return {@literal true} if the user ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 * @since 2.2
	 */
	default boolean userIdIsAssigned() {
		try {
			return EntityConstants.isAssigned(getUserId());
		} catch ( IllegalStateException e ) {
			return false;
		}
	}

	/**
	 * Get the user ID, presumed non-null.
	 *
	 * <p>
	 * This method is a nullness-check shortcut, for example to be used after
	 * {@link #userIdIsAssigned()} returns {@code true}.
	 * </p>
	 *
	 * @return the user ID (presumed non-null)
	 * @since 2.2
	 */
	@SuppressWarnings("NullAway")
	default Long userId() {
		return getUserId();
	}

	/**
	 * Get the authorization token associated with this job, if any.
	 *
	 * @return the authorization token, or {@code null} if none
	 * @since 2.1
	 */
	default @Nullable String getTokenId() {
		return null;
	}

	/**
	 * Get the configuration associated with this entity.
	 *
	 * @return the configuration
	 */
	@Nullable
	Configuration getConfiguration();

	/**
	 * Get the data export starting date.
	 *
	 * @return the export date
	 */
	@Nullable
	Instant getExportDate();

}
