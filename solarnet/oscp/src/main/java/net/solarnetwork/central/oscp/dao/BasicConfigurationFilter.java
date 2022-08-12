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
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link ConfigurationFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicConfigurationFilter extends BasicCoreCriteria implements ConfigurationFilter {

	private Long[] configurationIds;

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
			setConfigurationIds(c.getConfigurationIds());
		} else {
			if ( criteria instanceof ConfigurationCriteria c ) {
				setConfigurationIds(c.getConfigurationIds());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(configurationIds);
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
		return Arrays.equals(configurationIds, other.configurationIds);
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

}
