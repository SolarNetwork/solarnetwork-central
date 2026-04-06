/* ==================================================================
 * StreamAliasCriteria.java - 26/03/2026 9:05:32 am
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

package net.solarnetwork.central.datum.v2.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for stream aliases.
 *
 * @author matt
 * @version 1.0
 */
public interface StreamAliasCriteria {

	/**
	 * Flag to include stream aliases in search.
	 *
	 * <p>
	 * Setting this to {@literal true} means the search is allowed to match
	 * stream aliases, for example when matching other criteria like stream,
	 * node, and source IDs. If {@code false} or {@code null} then stream
	 * aliases are not considered.
	 * </p>
	 *
	 * @return {@literal true} to include stream alias matches in the results
	 */
	@Nullable
	Boolean getIncludeStreamAliases();

	/**
	 * Test if this filter has stream alias criteria.
	 *
	 * @return {@literal true} if the {@link #getIncludeStreamAliases()} is
	 *         non-null
	 */
	default boolean hasStreamAliasCriteria() {
		return (getIncludeStreamAliases() != null);
	}

	/**
	 * Test if stream aliases should be included.
	 *
	 * @return {@code true} if {@link #getIncludeStreamAliases()} is
	 *         {@code true}
	 */
	default boolean includeStreamAliases() {
		final var inc = getIncludeStreamAliases();
		return (inc != null && inc);
	}

}
