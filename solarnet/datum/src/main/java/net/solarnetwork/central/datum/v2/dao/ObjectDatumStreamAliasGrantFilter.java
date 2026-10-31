/* ==================================================================
 * ObjectDatumStreamAliasGrantFilter.java - 31/10/2026 4:15:10 pm
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
import net.solarnetwork.central.common.dao.EnabledCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * A filter for {@link ObjectDatumStreamAliasGrantEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface ObjectDatumStreamAliasGrantFilter
		extends UserCriteria, EnabledCriteria, PaginationCriteria {

	/**
	 * Get the first grant ID.
	 *
	 * @return the first grant ID, or {@code null} if not available
	 */
	default @Nullable Long getGrantId() {
		final Long[] ids = getGrantIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Get the grant IDs.
	 *
	 * @return the grant IDs, or {@code null} if not available
	 */
	Long @Nullable [] getGrantIds();

	/**
	 * Test if this filter has any grant criteria.
	 *
	 * @return {@code true} if a grant ID is available
	 */
	default boolean hasGrantCriteria() {
		return getGrantId() != null;
	}

	/**
	 * Get the first grantee ID.
	 *
	 * @return the first grantee ID, or {@code null} if not available
	 */
	default @Nullable Long getGranteeId() {
		final Long[] ids = getGranteeIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Get the grantee IDs.
	 *
	 * @return the grantee IDs, or {@code null} if not available
	 */
	Long @Nullable [] getGranteeIds();

	/**
	 * Test if this filter has any grantee criteria.
	 *
	 * @return {@code true} if a grantee ID is available
	 */
	default boolean hasGranteeCriteria() {
		return getGranteeId() != null;
	}

}
