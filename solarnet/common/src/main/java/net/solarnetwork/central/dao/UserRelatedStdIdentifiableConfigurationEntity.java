/* ==================================================================
 * UserRelatedStdIdentifiableConfigurationEntity.java - 28/09/2024 5:15:39 pm
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Extension of {@link UserRelatedIdentifiableConfigurationEntity} that supports
 * {@link UserRelatedStdEntity}.
 *
 * @param <C>
 *        the entity type
 * @param <K>
 *        the key type
 *
 * @author matt
 * @version 1.1
 */
public interface UserRelatedStdIdentifiableConfigurationEntity<C extends UserRelatedStdIdentifiableConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends UserRelatedStdEntity<C, K>, UserRelatedIdentifiableConfigurationEntity<K>, Serializable,
		Cloneable {

	/**
	 * Erase any sensitive credentials.
	 */
	default void eraseCredentials() {
		// extending classes can implement as needed
	}

	/**
	 * Cryptographically digest any sensitive settings.
	 *
	 * @param sensitiveKeyProvider
	 *        a function that can supply a set of "sensitive" information keys
	 *        (names) that should be masked
	 * @param returns
	 *        this object for method chaining
	 * @since 1.1
	 */
	default UserRelatedStdIdentifiableConfigurationEntity<C, K> digestSensitiveInformation(
			Function<String, Set<String>> sensitiveKeyProvider) {
		return this;
	}

}
