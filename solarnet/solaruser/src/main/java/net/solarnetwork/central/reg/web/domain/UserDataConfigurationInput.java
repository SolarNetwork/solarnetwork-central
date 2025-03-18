/* ==================================================================
 * UserDataConfigurationInput.java - 17/03/2025 4:40:13 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.domain;

import java.time.Instant;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;

/**
 * Input DTO for {@link UserDataConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UserDataConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<UserDataConfiguration, UserLongCompositePK> {

	private Long id;

	private DatumFilterCommand datumFilter;

	/**
	 * Constructor.
	 */
	public UserDataConfigurationInput() {
		super();
	}

	@Override
	public UserDataConfiguration toEntity(UserLongCompositePK id, Instant date) {
		UserDataConfiguration entity = new UserDataConfiguration(id, date);
		populateConfiguration(entity);
		return entity;
	}

	@Override
	protected void populateConfiguration(UserDataConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setFilter(datumFilter);
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get the datum filter.
	 *
	 * @return the filter
	 */
	public DatumFilterCommand getDatumFilter() {
		return datumFilter;
	}

	/**
	 * Set the datum filter.
	 *
	 * @param datumFilter
	 *        the filter to set
	 */
	public void setDatumFilter(DatumFilterCommand datumFilter) {
		this.datumFilter = datumFilter;
	}

}
