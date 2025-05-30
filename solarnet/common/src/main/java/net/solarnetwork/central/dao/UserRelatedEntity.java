/* ==================================================================
 * UserRelatedEntity.java - 25/03/2018 1:55:08 PM
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.dao.Entity;

/**
 * API for an entity associated with a user ID.
 *
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 2.0
 * @since 2.0
 */
public interface UserRelatedEntity<T extends UserRelatedEntity<T, K>, K extends Comparable<K> & Serializable>
		extends Entity<T, K>, UserIdRelated {

	/**
	 * Mask any sensitive information.
	 *
	 * @param sensitiveKeyProvider
	 *        a function that can supply a set of "sensitive" information keys
	 *        (names) that should be masked
	 * @param encryptor
	 *        the encryptor to use
	 * @since 1.2
	 */
	default void maskSensitiveInformation(Function<String, Set<String>> sensitiveKeyProvider,
			TextEncryptor encryptor) {
		// nothing
	}

	/**
	 * Unmask any sensitive information.
	 *
	 * @param sensitiveKeyProvider
	 *        a function that can supply a set of "sensitive" information keys
	 *        (names) that should be masked
	 * @param encryptor
	 *        the encryptor to use
	 * @since 1.2
	 */
	default void unmaskSensitiveInformation(Function<String, Set<String>> sensitiveKeyProvider,
			TextEncryptor encryptor) {
		// nothing
	}

}
