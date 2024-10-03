/* ==================================================================
 * BasicFilter.java - 1/10/2024 7:46:04 am
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

package net.solarnetwork.central.c2c.dao;

import java.util.Arrays;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.IndexCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of cloud integration query filter.
 *
 * @author matt
 * @version 1.0
 */
public class BasicFilter extends BasicCoreCriteria
		implements CloudIntegrationFilter, CloudDatumStreamPropertyFilter {

	private Long[] integrationIds;
	private Long[] datumStreamIds;
	private Integer[] indexes;

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
		if ( criteria instanceof CloudIntegrationCriteria f ) {
			setIntegrationIds(f.getIntegrationIds());
		}
		if ( criteria instanceof CloudDatumStreamCriteria f ) {
			setDatumStreamIds(f.getDatumStreamIds());
		}
		if ( criteria instanceof IndexCriteria f ) {
			setIndexes(f.getIndexes());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(integrationIds);
		result = prime * result + Arrays.hashCode(datumStreamIds);
		result = prime * result + Arrays.hashCode(indexes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicFilter) ) {
			return false;
		}
		BasicFilter other = (BasicFilter) obj;
		return Arrays.equals(integrationIds, other.integrationIds)
				&& Arrays.equals(datumStreamIds, other.datumStreamIds)
				&& Arrays.equals(indexes, other.indexes);
	}

	@Override
	public Long getIntegrationId() {
		return CloudIntegrationFilter.super.getIntegrationId();
	}

	/**
	 * Set the integration ID.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public void setIntegrationId(Long integrationId) {
		setIntegrationIds(integrationId != null ? new Long[] { integrationId } : null);
	}

	@Override
	public Long[] getIntegrationIds() {
		return integrationIds;
	}

	/**
	 * Set the integration IDs.
	 *
	 * @param integrationIds
	 *        the integration IDs to set
	 */
	public void setIntegrationIds(Long[] integrationIds) {
		this.integrationIds = integrationIds;
	}

	@Override
	public Long getDatumStreamId() {
		return CloudDatumStreamPropertyFilter.super.getDatumStreamId();
	}

	/**
	 * Set the datumStream ID.
	 *
	 * @param datumStreamId
	 *        the datumStream ID to set
	 */
	public void setDatumStreamId(Long datumStreamId) {
		setDatumStreamIds(datumStreamId != null ? new Long[] { datumStreamId } : null);
	}

	@Override
	public Long[] getDatumStreamIds() {
		return datumStreamIds;
	}

	/**
	 * Set the datumStream IDs.
	 *
	 * @param datumStreamIds
	 *        the datumStream IDs to set
	 */
	public void setDatumStreamIds(Long[] datumStreamIds) {
		this.datumStreamIds = datumStreamIds;
	}

	@Override
	public Integer getIndex() {
		return CloudDatumStreamPropertyFilter.super.getIndex();
	}

	/**
	 * Set the index.
	 *
	 * @param index
	 *        the index to set
	 */
	public void setIndex(Integer index) {
		setIndexes(index != null ? new Integer[] { index } : null);
	}

	/**
	 * Get the indexes.
	 *
	 * @return the indexes
	 */
	@Override
	public Integer[] getIndexes() {
		return indexes;
	}

	/**
	 * Set the indexes.
	 *
	 * @param indexes
	 *        the indexes to set
	 */
	public void setIndexes(Integer[] indexes) {
		this.indexes = indexes;
	}

}
