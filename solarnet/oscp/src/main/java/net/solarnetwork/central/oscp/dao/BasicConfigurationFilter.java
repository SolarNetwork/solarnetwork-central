/* ==================================================================
 * BasicConfigurationFilter.java - 11/08/2022 11:25:48 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao;

import java.util.Arrays;
import java.util.Objects;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link ConfigurationFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicConfigurationFilter extends BasicCoreCriteria
		implements AssetFilter, CapacityGroupFilter {

	private boolean lockResults;
	private boolean skipLockedResults;
	private Long[] configurationIds;
	private Long[] groupIds;
	private Long[] providerIds;
	private Long[] optimizerIds;
	private String[] identifiers;

	/**
	 * Create a filter for one or more user IDs.
	 * 
	 * @param userIds
	 *        the user IDs to create the filter for
	 * @return the new filter instance
	 */
	public static BasicConfigurationFilter filterForUsers(Long... userIds) {
		BasicConfigurationFilter f = new BasicConfigurationFilter();
		f.setUserIds(userIds);
		return f;
	}

	/**
	 * Constructor.
	 */
	public BasicConfigurationFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicConfigurationFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicConfigurationFilter c ) {
			setLockResults(c.isLockResults());
			setSkipLockedResults(c.isSkipLockedResults());
			setConfigurationIds(c.getConfigurationIds());
			setGroupIds(c.getGroupIds());
			setProviderIds(c.getProviderIds());
			setOptimizerIds(c.getOptimizerIds());
			setIdentifiers(getIdentifiers());
		} else {
			if ( criteria instanceof LockingCriteria c ) {
				setLockResults(c.isLockResults());
				setSkipLockedResults(c.isSkipLockedResults());
			}
			if ( criteria instanceof ConfigurationCriteria c ) {
				setConfigurationIds(c.getConfigurationIds());
			}
			if ( criteria instanceof GroupCriteria c ) {
				setGroupIds(c.getGroupIds());
			}
			if ( criteria instanceof ProviderCriteria c ) {
				setProviderIds(c.getProviderIds());
			}
			if ( criteria instanceof OptimizerCriteria c ) {
				setOptimizerIds(getLocationIds());
			}
			if ( criteria instanceof IdentifierCriteria c ) {
				setIdentifiers(c.getIdentifiers());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(lockResults);
		result = prime * result + Objects.hash(skipLockedResults);
		result = prime * result + Arrays.hashCode(configurationIds);
		result = prime * result + Arrays.hashCode(groupIds);
		result = prime * result + Arrays.hashCode(providerIds);
		result = prime * result + Arrays.hashCode(optimizerIds);
		result = prime * result + Arrays.hashCode(identifiers);
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
		if ( !(obj instanceof BasicConfigurationFilter) ) {
			return false;
		}
		BasicConfigurationFilter other = (BasicConfigurationFilter) obj;
		return lockResults == other.lockResults && skipLockedResults == other.skipLockedResults
				&& Arrays.equals(configurationIds, other.configurationIds)
				&& Arrays.equals(groupIds, other.groupIds)
				&& Arrays.equals(providerIds, other.providerIds)
				&& Arrays.equals(optimizerIds, other.optimizerIds)
				&& Arrays.equals(identifiers, other.identifiers);
	}

	@Override
	public BasicConfigurationFilter clone() {
		return (BasicConfigurationFilter) super.clone();
	}

	@Override
	public Long[] getConfigurationIds() {
		return configurationIds;
	}

	/**
	 * Set the configuration IDs.
	 * 
	 * @param configurationIds
	 *        the IDs of the configurations to find
	 */
	public void setConfigurationIds(Long[] configurationIds) {
		this.configurationIds = configurationIds;
	}

	/**
	 * Set a single configuration ID.
	 * 
	 * @param configurationId
	 *        the ID of the configuration to set
	 */
	public void setConfigurationId(Long configurationId) {
		setConfigurationIds(configurationId != null ? new Long[] { configurationId } : null);
	}

	@Override
	public Long[] getGroupIds() {
		return groupIds;
	}

	/**
	 * Set the group IDs.
	 * 
	 * @param groupIds
	 *        the group IDs to set
	 */
	public void setGroupIds(Long[] groupIds) {
		this.groupIds = groupIds;
	}

	/**
	 * Set a single group ID.
	 * 
	 * @param groupId
	 *        the ID of the group to set
	 */
	public void setGroupId(Long groupId) {
		setGroupIds(groupId != null ? new Long[] { groupId } : null);
	}

	@Override
	public Long[] getProviderIds() {
		return providerIds;
	}

	/**
	 * Set the provider IDs.
	 * 
	 * @param providerIds
	 *        the provider IDs to set
	 */
	public void setProviderIds(Long[] providerIds) {
		this.providerIds = providerIds;
	}

	/**
	 * Set a single provider ID.
	 * 
	 * @param providerId
	 *        the ID of the provider to set
	 */
	public void setProviderId(Long providerId) {
		setProviderIds(providerId != null ? new Long[] { providerId } : null);
	}

	@Override
	public Long[] getOptimizerIds() {
		return optimizerIds;
	}

	/**
	 * Set the optimizer IDs.
	 * 
	 * @param optimizerIds
	 *        the optimizer IDs to set
	 */
	public void setOptimizerIds(Long[] optimizerIds) {
		this.optimizerIds = optimizerIds;
	}

	/**
	 * Set a single optimizer ID.
	 * 
	 * @param optimizerId
	 *        the ID of the optimizer to set
	 */
	public void setOptimizerId(Long optimizerId) {
		setOptimizerIds(optimizerId != null ? new Long[] { optimizerId } : null);
	}

	@Override
	public String[] getIdentifiers() {
		return identifiers;
	}

	/**
	 * Set the provider IDs.
	 * 
	 * @param identifiers
	 *        the identifiers to set
	 */
	public void setIdentifiers(String[] identifiers) {
		this.identifiers = identifiers;
	}

	/**
	 * Set a single identifier.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		setIdentifiers(identifier != null ? new String[] { identifier } : null);
	}

	@Override
	public boolean isLockResults() {
		return lockResults;
	}

	/**
	 * Set the lock results flag.
	 * 
	 * @param lockResults
	 *        {@literal true} to request locked results
	 */
	public void setLockResults(boolean lockResults) {
		this.lockResults = lockResults;
	}

	@Override
	public boolean isSkipLockedResults() {
		return skipLockedResults;
	}

	/**
	 * Set the "skip locked results" flag.
	 * 
	 * @param skipLockedResults
	 *        {@literal true} to skip locked results
	 */
	public void setSkipLockedResults(boolean skipLockedResults) {
		this.skipLockedResults = skipLockedResults;
	}

}
