/* ==================================================================
 * SecurityTokenCriteria.java - 30/11/2020 10:40:13 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for security token related data.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public interface SecurityTokenCriteria {

	/**
	 * Get the first token ID.
	 * 
	 * <p>
	 * This returns the first available token ID from the {@link #getTokenIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first token ID, or {@code null} if not available
	 */
	@Nullable
	default String getTokenId() {
		var a = getTokenIds();
		return (a != null && a.length > 0 ? a[0] : null);
	}

	/**
	 * Get an array of token IDs.
	 * 
	 * @return array of token IDs (may be {@code null})
	 */
	String @Nullable [] getTokenIds();

	/**
	 * Test if this filter has any token criteria.
	 * 
	 * @return {@literal true} if the token ID is non-null
	 */
	default boolean hasTokenCriteria() {
		return getTokenId() != null;
	}

	/**
	 * Get the first token ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTokenCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first source ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String tokenId() {
		return getTokenId();
	}

	/**
	 * Get an array of token IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTokenCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of source IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String[] tokenIds() {
		return getTokenIds();
	}

}
