/* ==================================================================
 * BasicObjectDatumStreamAliasGrantFilter.java - 31/10/2026 4:16:45 pm
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

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link ObjectDatumStreamAliasGrantFilter}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicObjectDatumStreamAliasGrantFilter extends BasicCoreCriteria
		implements ObjectDatumStreamAliasGrantFilter {

	private Long @Nullable [] grantIds;
	private Long @Nullable [] granteeIds;

	/**
	 * Constructor.
	 */
	public BasicObjectDatumStreamAliasGrantFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicObjectDatumStreamAliasGrantFilter(@Nullable PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public BasicObjectDatumStreamAliasGrantFilter clone() {
		return (BasicObjectDatumStreamAliasGrantFilter) super.clone();
	}

	@Override
	public void copyFrom(@Nullable PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof ObjectDatumStreamAliasGrantFilter f ) {
			setGrantIds(f.getGrantIds());
			setGranteeIds(f.getGranteeIds());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(grantIds);
		result = prime * result + Arrays.hashCode(granteeIds);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof BasicObjectDatumStreamAliasGrantFilter other) ) {
			return false;
		}
		return Arrays.equals(grantIds, other.grantIds) && Arrays.equals(granteeIds, other.granteeIds);
	}

	/**
	 * Set a single grant ID.
	 *
	 * @param grantId
	 *        the grant ID to set
	 */
	public final void setGrantId(@Nullable Long grantId) {
		setGrantIds(grantId != null ? new Long[] { grantId } : null);
	}

	@Override
	public final Long @Nullable [] getGrantIds() {
		return grantIds;
	}

	/**
	 * Set the grant IDs.
	 *
	 * @param grantIds
	 *        the grant IDs to set
	 */
	public final void setGrantIds(Long @Nullable [] grantIds) {
		this.grantIds = grantIds;
	}

	/**
	 * Set a single grantee ID.
	 *
	 * @param granteeId
	 *        the grantee ID to set
	 */
	public final void setGranteeId(@Nullable Long granteeId) {
		setGranteeIds(granteeId != null ? new Long[] { granteeId } : null);
	}

	@Override
	public final Long @Nullable [] getGranteeIds() {
		return granteeIds;
	}

	/**
	 * Set the grantee IDs.
	 *
	 * @param granteeIds
	 *        the grantee IDs to set
	 */
	public final void setGranteeIds(Long @Nullable [] granteeIds) {
		this.granteeIds = granteeIds;
	}

}
