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

import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of datum input endpoint query filter.
 *
 * @author matt
 * @version 1.0
 */
public class BasicFilter extends BasicCoreCriteria implements CredentialFilter, TransformFilter {

	private Long[] credentialIds;
	private Long[] transformIds;

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
	public BasicFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof CredentialCriteria f ) {
			setCredentialIds(f.getCredentialIds());
		}
		if ( criteria instanceof TransformCriteria f ) {
			setTransformIds(f.getTransformIds());
		}
	}

	@Override
	public Long getCredentialId() {
		return CredentialFilter.super.getCredentialId();
	}

	/**
	 * Set the credential ID.
	 *
	 * @param credentialId
	 *        the credential ID to set
	 */
	public void setCredentialId(Long credentialId) {
		setCredentialIds(credentialId != null ? new Long[] { credentialId } : null);
	}

	@Override
	public Long[] getCredentialIds() {
		return credentialIds;
	}

	/**
	 * Set the credential IDs.
	 *
	 * @param credentialIds
	 *        the credential IDs to set
	 */
	public void setCredentialIds(Long[] credentialIds) {
		this.credentialIds = credentialIds;
	}

	@Override
	public Long getTransformId() {
		return TransformFilter.super.getTransformId();
	}

	/**
	 * Set the transform ID.
	 *
	 * @param transformId
	 *        the transform ID to set
	 */
	public void setTransformId(Long transformId) {
		setTransformIds(transformId != null ? new Long[] { transformId } : null);
	}

	@Override
	public Long[] getTransformIds() {
		return transformIds;
	}

	/**
	 * Set the transform IDs.
	 *
	 * @param transformIds
	 *        the transform IDs to set
	 */
	public void setTransformIds(Long[] transformIds) {
		this.transformIds = transformIds;
	}

}
