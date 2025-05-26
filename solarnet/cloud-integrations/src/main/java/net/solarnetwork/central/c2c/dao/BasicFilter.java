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

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.ClaimableJobStateCriteria;
import net.solarnetwork.central.common.dao.IdentifiableCriteria;
import net.solarnetwork.central.common.dao.IndexCriteria;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.ClaimableJobState;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of cloud integration query filter.
 *
 * @author matt
 * @version 1.2
 */
public class BasicFilter extends BasicCoreCriteria
		implements CloudIntegrationFilter, CloudDatumStreamFilter, CloudDatumStreamMappingFilter,
		CloudDatumStreamPropertyFilter, CloudDatumStreamPollTaskFilter, CloudDatumStreamSettingsFilter {

	private Long[] integrationIds;
	private Long[] datumStreamIds;
	private Long[] datumStreamMappingIds;
	private Integer[] indexes;
	private BasicClaimableJobState[] claimableJobStates;
	private String[] serviceIdentifiers;
	private Instant startDate;
	private Instant endDate;

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
		if ( criteria instanceof BasicFilter f ) {
			setIntegrationIds(f.getIntegrationIds());
			setDatumStreamIds(f.getDatumStreamIds());
			setDatumStreamMappingIds(f.getDatumStreamMappingIds());
			setIndexes(f.getIndexes());
			setClaimableJobStates(f.getClaimableJobStates());
			setServiceIdentifiers(f.getServiceIdentifiers());
			setStartDate(f.getStartDate());
			setEndDate(f.getEndDate());
		} else {
			if ( criteria instanceof CloudIntegrationCriteria f ) {
				setIntegrationIds(f.getIntegrationIds());
			}
			if ( criteria instanceof CloudDatumStreamCriteria f ) {
				setDatumStreamIds(f.getDatumStreamIds());
			}
			if ( criteria instanceof CloudDatumStreamMappingCriteria f ) {
				setDatumStreamMappingIds(f.getDatumStreamMappingIds());
			}
			if ( criteria instanceof IndexCriteria f ) {
				setIndexes(f.getIndexes());
			}
			if ( criteria instanceof ClaimableJobStateCriteria f ) {
				var states = f.getClaimableJobStates();
				SequencedSet<BasicClaimableJobState> copy = null;
				if ( states != null && states.length > 0 ) {
					for ( ClaimableJobState s : states ) {
						if ( s instanceof BasicClaimableJobState j ) {
							if ( copy == null ) {
								copy = new LinkedHashSet<>(states.length);
							}
							copy.add(j);
						}
					}
				}
				setClaimableJobStates(copy.toArray(BasicClaimableJobState[]::new));
			}
			if ( criteria instanceof IdentifiableCriteria f ) {
				setServiceIdentifiers(f.getServiceIdentifiers());
			}
			if ( criteria instanceof DateRangeCriteria f ) {
				setStartDate(f.getStartDate());
				setEndDate(f.getEndDate());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(integrationIds);
		result = prime * result + Arrays.hashCode(datumStreamIds);
		result = prime * result + Arrays.hashCode(datumStreamMappingIds);
		result = prime * result + Arrays.hashCode(indexes);
		result = prime * result + Arrays.hashCode(claimableJobStates);
		result = prime * result + Arrays.hashCode(serviceIdentifiers);
		result = prime * result + Objects.hash(startDate, endDate);
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
		if ( !(obj instanceof BasicFilter other) ) {
			return false;
		}
		return Arrays.equals(integrationIds, other.integrationIds)
				&& Arrays.equals(datumStreamIds, other.datumStreamIds)
				&& Arrays.equals(datumStreamMappingIds, other.datumStreamMappingIds)
				&& Arrays.equals(indexes, other.indexes)
				&& Arrays.equals(claimableJobStates, other.claimableJobStates)
				&& Arrays.equals(serviceIdentifiers, other.serviceIdentifiers)
				&& Objects.equals(startDate, other.startDate) && Objects.equals(endDate, other.endDate);
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
		return CloudDatumStreamFilter.super.getDatumStreamId();
	}

	/**
	 * Set the datum stream ID.
	 *
	 * @param datumStreamId
	 *        the datum stream ID to set
	 */
	public void setDatumStreamId(Long datumStreamId) {
		setDatumStreamIds(datumStreamId != null ? new Long[] { datumStreamId } : null);
	}

	@Override
	public Long[] getDatumStreamIds() {
		return datumStreamIds;
	}

	/**
	 * Set the datum stream IDs.
	 *
	 * @param datumStreamIds
	 *        the datum stream IDs to set
	 */
	public void setDatumStreamIds(Long[] datumStreamIds) {
		this.datumStreamIds = datumStreamIds;
	}

	@Override
	public Long getDatumStreamMappingId() {
		return CloudDatumStreamMappingFilter.super.getDatumStreamMappingId();
	}

	/**
	 * Set the datum stream mapping ID.
	 *
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID to set
	 */
	public void setDatumStreamMappingId(Long datumStreamMappingId) {
		setDatumStreamMappingIds(
				datumStreamMappingId != null ? new Long[] { datumStreamMappingId } : null);
	}

	@Override
	public Long[] getDatumStreamMappingIds() {
		return datumStreamMappingIds;
	}

	/**
	 * Set the datum stream mapping IDs.
	 *
	 * @param datumStreamMappingIds
	 *        the datum stream mapping IDs to set
	 */
	public void setDatumStreamMappingIds(Long[] datumStreamMappingIds) {
		this.datumStreamMappingIds = datumStreamMappingIds;
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

	@Override
	public final BasicClaimableJobState[] getClaimableJobStates() {
		return claimableJobStates;
	}

	/**
	 * Set the claimable job states.
	 *
	 * @param claimableJobStates
	 *        the states to set
	 */
	public final void setClaimableJobStates(BasicClaimableJobState[] claimableJobStates) {
		this.claimableJobStates = claimableJobStates;
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public final String[] getServiceIdentifiers() {
		return serviceIdentifiers;
	}

	/**
	 * Set the service identifiers.
	 *
	 * @param serviceIdentifiers
	 *        the service identifiers to set
	 * @since 1.2
	 */
	public final void setServiceIdentifiers(String[] serviceIdentifiers) {
		this.serviceIdentifiers = serviceIdentifiers;
	}

}
