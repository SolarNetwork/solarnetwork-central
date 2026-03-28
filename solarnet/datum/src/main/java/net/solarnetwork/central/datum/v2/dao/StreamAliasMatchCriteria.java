/* ==================================================================
 * StreamAliasMatchCriteria.java - 29/03/2026 8:13:50 am
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
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;

/**
 * Search criteria for object datum original streams.
 *
 * @author matt
 * @version 1.0
 */
public interface StreamAliasMatchCriteria {

	/**
	 * Get the stream alias match type.
	 *
	 * @return the stream alias match type
	 */
	@Nullable
	ObjectDatumStreamAliasMatchType getStreamAliasMatchType();

	/**
	 * Test if this filter has stream alias match criteria.
	 *
	 * @return {@literal true} if the {@link #getStreamAliasMatchType()} is
	 *         non-null
	 */
	default boolean hasStreamAliasMatchCriteria() {
		return (getStreamAliasMatchType() != null);
	}

	/**
	 * Get a non-null stream alias match type.
	 *
	 * @return the {@link #getStreamAliasMatchType()} if that is non-null,
	 *         otherwise {@link ObjectDatumStreamAliasMatchType#OriginalOrAlias}
	 */
	default ObjectDatumStreamAliasMatchType streamAliasMatchType() {
		final var type = getStreamAliasMatchType();
		return (type != null ? type : ObjectDatumStreamAliasMatchType.OriginalOrAlias);
	}

}
