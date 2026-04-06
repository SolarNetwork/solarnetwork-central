/* ==================================================================
 * BasicFilter.java - 21/02/2024 8:36:18 am
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

package net.solarnetwork.central.din.dao;

import java.util.Arrays;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of datum input endpoint query filter.
 *
 * @author matt
 * @version 1.1
 */
public class BasicFilter extends BasicCoreCriteria
		implements CredentialFilter, TransformFilter, EndpointFilter, EndpointAuthFilter {

	private Long @Nullable [] credentialIds;
	private Long @Nullable [] transformIds;
	private UUID @Nullable [] endpointIds;

	@Override
	public BasicFilter clone() {
		return (BasicFilter) super.clone();
	}

	/**
	 * Constructor.
	 */
	public BasicFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicFilter(@Nullable PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(@Nullable PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof CredentialCriteria f ) {
			setCredentialIds(f.getCredentialIds());
		}
		if ( criteria instanceof TransformCriteria f ) {
			setTransformIds(f.getTransformIds());
		}
		if ( criteria instanceof EndpointCriteria f ) {
			setEndpointIds(f.getEndpointIds());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(credentialIds);
		result = prime * result + Arrays.hashCode(endpointIds);
		result = prime * result + Arrays.hashCode(transformIds);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof BasicFilter other) ) {
			return false;
		}
		return Arrays.equals(credentialIds, other.credentialIds)
				&& Arrays.equals(endpointIds, other.endpointIds)
				&& Arrays.equals(transformIds, other.transformIds);
	}

	@Override
	public boolean hasAnyCriteria() {
		// @formatter:off
		return     super.hasAnyCriteria()
				|| (credentialIds != null && credentialIds.length > 0)
				|| (endpointIds != null && endpointIds.length > 0)
				|| (transformIds != null && transformIds.length > 0)
				;
		// @formatter:on
	}

	@Override
	public final @Nullable Long getCredentialId() {
		return CredentialFilter.super.getCredentialId();
	}

	/**
	 * Set the credential ID.
	 *
	 * @param credentialId
	 *        the credential ID to set
	 */
	public final void setCredentialId(@Nullable Long credentialId) {
		setCredentialIds(credentialId != null ? new Long[] { credentialId } : null);
	}

	@Override
	public final Long @Nullable [] getCredentialIds() {
		return credentialIds;
	}

	/**
	 * Set the credential IDs.
	 *
	 * @param credentialIds
	 *        the credential IDs to set
	 */
	public final void setCredentialIds(Long @Nullable [] credentialIds) {
		this.credentialIds = credentialIds;
	}

	@Override
	public final @Nullable Long getTransformId() {
		return TransformFilter.super.getTransformId();
	}

	/**
	 * Set the transform ID.
	 *
	 * @param transformId
	 *        the transform ID to set
	 */
	public final void setTransformId(@Nullable Long transformId) {
		setTransformIds(transformId != null ? new Long[] { transformId } : null);
	}

	@Override
	public final Long @Nullable [] getTransformIds() {
		return transformIds;
	}

	/**
	 * Set the transform IDs.
	 *
	 * @param transformIds
	 *        the transform IDs to set
	 */
	public final void setTransformIds(Long @Nullable [] transformIds) {
		this.transformIds = transformIds;
	}

	@Override
	public final @Nullable UUID getEndpointId() {
		return EndpointFilter.super.getEndpointId();
	}

	/**
	 * Set the endpoint ID.
	 *
	 * @param endpointId
	 *        the endpoint ID to set
	 */
	public final void setEndpointId(@Nullable UUID endpointId) {
		setEndpointIds(endpointId != null ? new UUID[] { endpointId } : null);
	}

	@Override
	public final UUID @Nullable [] getEndpointIds() {
		return endpointIds;
	}

	/**
	 * Set the endpoint IDs.
	 *
	 * @param endpointIds
	 *        the endpoint IDs to set
	 */
	public final void setEndpointIds(UUID @Nullable [] endpointIds) {
		this.endpointIds = endpointIds;
	}

}
