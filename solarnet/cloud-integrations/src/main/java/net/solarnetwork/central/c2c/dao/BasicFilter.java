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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.ClaimableJobStateCriteria;
import net.solarnetwork.central.common.dao.ControlCriteria;
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
 * @version 1.6
 */
public class BasicFilter extends BasicCoreCriteria
		implements CloudIntegrationFilter, CloudDatumStreamFilter, CloudDatumStreamMappingFilter,
		CloudDatumStreamPropertyFilter, CloudDatumStreamPollTaskFilter, CloudDatumStreamRakeTaskFilter,
		CloudDatumStreamSettingsFilter, CloudControlFilter {

	private Long @Nullable [] integrationIds;
	private Long @Nullable [] datumStreamIds;
	private Long @Nullable [] datumStreamMappingIds;
	private Long @Nullable [] cloudControlIds;
	private String @Nullable [] controlIds;
	private Long @Nullable [] taskIds;
	private Integer @Nullable [] indexes;
	private BasicClaimableJobState @Nullable [] claimableJobStates;
	private String @Nullable [] serviceIdentifiers;
	private @Nullable Instant startDate;
	private @Nullable Instant endDate;

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
	public BasicFilter clone() {
		return (BasicFilter) super.clone();
	}

	@Override
	public void copyFrom(@Nullable PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicFilter f ) {
			setIntegrationIds(f.getIntegrationIds());
			setDatumStreamIds(f.getDatumStreamIds());
			setDatumStreamMappingIds(f.getDatumStreamMappingIds());
			setCloudControlIds(f.getCloudControlIds());
			setControlIds(f.getControlIds());
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
			if ( criteria instanceof CloudControlCriteria f ) {
				setCloudControlIds(f.getCloudControlIds());
			}
			if ( criteria instanceof ControlCriteria f ) {
				setControlIds(f.getControlIds());
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
				setClaimableJobStates(copy != null ? copy.toArray(BasicClaimableJobState[]::new) : null);
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
		result = prime * result + Arrays.hashCode(cloudControlIds);
		result = prime * result + Arrays.hashCode(controlIds);
		result = prime * result + Arrays.hashCode(indexes);
		result = prime * result + Arrays.hashCode(claimableJobStates);
		result = prime * result + Arrays.hashCode(serviceIdentifiers);
		result = prime * result + Objects.hash(startDate, endDate);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
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
				&& Arrays.equals(cloudControlIds, other.cloudControlIds)
				&& Arrays.equals(controlIds, other.controlIds) && Arrays.equals(indexes, other.indexes)
				&& Arrays.equals(claimableJobStates, other.claimableJobStates)
				&& Arrays.equals(serviceIdentifiers, other.serviceIdentifiers)
				&& Objects.equals(startDate, other.startDate) && Objects.equals(endDate, other.endDate);
	}

	@Override
	public boolean hasAnyCriteria() {
		// @formatter:off
		return     super.hasAnyCriteria()
				|| (claimableJobStates != null && claimableJobStates.length > 0)
				|| (cloudControlIds != null && cloudControlIds.length > 0)
				|| (controlIds != null && controlIds.length > 0)
				|| (datumStreamIds != null && datumStreamIds.length > 0)
				|| (datumStreamMappingIds != null && datumStreamMappingIds.length > 0)
				|| endDate != null
				|| (indexes != null && indexes.length > 0)
				|| (integrationIds != null && integrationIds.length > 0)
				|| (serviceIdentifiers != null && serviceIdentifiers.length > 0)
				|| startDate != null
				|| (taskIds != null && taskIds.length > 0)
				;
		// @formatter:on
	}

	@Override
	public final @Nullable Long getIntegrationId() {
		return CloudIntegrationFilter.super.getIntegrationId();
	}

	/**
	 * Set the integration ID.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public final void setIntegrationId(@Nullable Long integrationId) {
		setIntegrationIds(integrationId != null ? new Long[] { integrationId } : null);
	}

	@Override
	public final Long @Nullable [] getIntegrationIds() {
		return integrationIds;
	}

	/**
	 * Set the integration IDs.
	 *
	 * @param integrationIds
	 *        the integration IDs to set
	 */
	public final void setIntegrationIds(Long @Nullable [] integrationIds) {
		this.integrationIds = integrationIds;
	}

	@Override
	public final @Nullable Long getDatumStreamId() {
		return CloudDatumStreamFilter.super.getDatumStreamId();
	}

	/**
	 * Set the datum stream ID.
	 *
	 * @param datumStreamId
	 *        the datum stream ID to set
	 */
	public final void setDatumStreamId(@Nullable Long datumStreamId) {
		setDatumStreamIds(datumStreamId != null ? new Long[] { datumStreamId } : null);
	}

	@Override
	public final Long @Nullable [] getDatumStreamIds() {
		return datumStreamIds;
	}

	/**
	 * Set the datum stream IDs.
	 *
	 * @param datumStreamIds
	 *        the datum stream IDs to set
	 */
	public final void setDatumStreamIds(Long @Nullable [] datumStreamIds) {
		this.datumStreamIds = datumStreamIds;
	}

	@Override
	public final @Nullable Long getDatumStreamMappingId() {
		return CloudDatumStreamMappingFilter.super.getDatumStreamMappingId();
	}

	/**
	 * Set the datum stream mapping ID.
	 *
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID to set
	 */
	public final void setDatumStreamMappingId(@Nullable Long datumStreamMappingId) {
		setDatumStreamMappingIds(
				datumStreamMappingId != null ? new Long[] { datumStreamMappingId } : null);
	}

	@Override
	public final Long @Nullable [] getDatumStreamMappingIds() {
		return datumStreamMappingIds;
	}

	/**
	 * Set the datum stream mapping IDs.
	 *
	 * @param datumStreamMappingIds
	 *        the datum stream mapping IDs to set
	 */
	public final void setDatumStreamMappingIds(Long @Nullable [] datumStreamMappingIds) {
		this.datumStreamMappingIds = datumStreamMappingIds;
	}

	@Override
	public final @Nullable Integer getIndex() {
		return CloudDatumStreamPropertyFilter.super.getIndex();
	}

	@Override
	public final @Nullable Long getCloudControlId() {
		return CloudControlFilter.super.getCloudControlId();
	}

	/**
	 * Set the cloud control ID.
	 *
	 * @param cloudControlId
	 *        the cloud control ID to set
	 */
	public final void setCloudControlId(@Nullable Long cloudControlId) {
		setCloudControlIds(cloudControlId != null ? new Long[] { cloudControlId } : null);
	}

	@Override
	public final Long @Nullable [] getCloudControlIds() {
		return cloudControlIds;
	}

	/**
	 * Set the cloud control IDs.
	 *
	 * @param cloudControlIds
	 *        the cloud control IDs to set
	 */
	public final void setCloudControlIds(Long @Nullable [] cloudControlIds) {
		this.cloudControlIds = cloudControlIds;
	}

	@Override
	public final @Nullable String getControlId() {
		return CloudControlFilter.super.getControlId();
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the cloud control ID to set
	 */
	public final void setControlId(@Nullable String controlId) {
		setControlIds(controlId != null ? new String[] { controlId } : null);
	}

	@Override
	public final String @Nullable [] getControlIds() {
		return controlIds;
	}

	/**
	 * Set the control IDs.
	 *
	 * @param controlIds
	 *        the control IDs to set
	 */
	public final void setControlIds(String @Nullable [] controlIds) {
		this.controlIds = controlIds;
	}

	/**
	 * Set the index.
	 *
	 * @param index
	 *        the index to set
	 */
	public final void setIndex(@Nullable Integer index) {
		setIndexes(index != null ? new Integer[] { index } : null);
	}

	/**
	 * Get the indexes.
	 *
	 * @return the indexes
	 */
	@Override
	public final Integer @Nullable [] getIndexes() {
		return indexes;
	}

	/**
	 * Set the indexes.
	 *
	 * @param indexes
	 *        the indexes to set
	 */
	public final void setIndexes(Integer @Nullable [] indexes) {
		this.indexes = indexes;
	}

	@Override
	public final BasicClaimableJobState @Nullable [] getClaimableJobStates() {
		return claimableJobStates;
	}

	/**
	 * Set the claimable job states.
	 *
	 * @param claimableJobStates
	 *        the states to set
	 */
	public final void setClaimableJobStates(BasicClaimableJobState @Nullable [] claimableJobStates) {
		this.claimableJobStates = claimableJobStates;
	}

	@Override
	public final @Nullable Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDate(@Nullable Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public final @Nullable Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDate(@Nullable Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public final String @Nullable [] getServiceIdentifiers() {
		return serviceIdentifiers;
	}

	/**
	 * Set the service identifiers.
	 *
	 * @param serviceIdentifiers
	 *        the service identifiers to set
	 * @since 1.2
	 */
	public final void setServiceIdentifiers(String @Nullable [] serviceIdentifiers) {
		this.serviceIdentifiers = serviceIdentifiers;
	}

	@Override
	public final @Nullable Long getTaskId() {
		return CloudDatumStreamRakeTaskFilter.super.getTaskId();
	}

	/**
	 * Set the task ID.
	 *
	 * @param taskId
	 *        the task ID to set
	 * @since 1.4
	 */
	public final void setTaskId(@Nullable Long taskId) {
		setTaskIds(taskId != null ? new Long[] { taskId } : null);
	}

	@Override
	public final Long @Nullable [] getTaskIds() {
		return taskIds;
	}

	/**
	 * Set the task IDs.
	 *
	 * @param taskIds
	 *        the task IDs to set
	 * @since 1.4
	 */
	public final void setTaskIds(Long @Nullable [] taskIds) {
		this.taskIds = taskIds;
	}

}
