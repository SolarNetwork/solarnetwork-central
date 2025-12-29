/* ==================================================================
 * UserDataConfiguration.java - 21/03/2018 2:01:46 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.DataConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * User related entity for {@link DataConfiguration}.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "id", "created", "userId", "name", "serviceIdentifier", "serviceProps" })
@JsonIgnoreProperties("enabled")
public class UserDataConfiguration extends BaseExportConfigurationEntity<UserDataConfiguration>
		implements DataConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = 866381003784859350L;

	private String filterJson;

	private transient DatumFilterCommand filter;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDataConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDataConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public UserDataConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new UserDataConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserDataConfiguration entity) {
		super.copyTo(entity);
		entity.setFilterJson(filterJson);
	}

	@Override
	public boolean isSameAs(UserDataConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return  // compare decoded JSON, as JSON key order not assumed
				Objects.equals(getFilter(), other.getFilter())
				;
		// @formatter:on
	}

	@Override
	public AggregateGeneralNodeDatumFilter getDatumFilter() {
		return getFilter();
	}

	@JsonIgnore
	public String getFilterJson() {
		if ( filterJson == null ) {
			filterJson = JsonUtils.getJSONString(filter, null);
		}
		return filterJson;
	}

	public void setFilterJson(String filterJson) {
		this.filterJson = filterJson;
		filter = null;
	}

	@JsonIgnore
	public DatumFilterCommand getFilter() {
		if ( filter == null && filterJson != null ) {
			filter = JsonUtils.getObjectFromJSON(filterJson, DatumFilterCommand.class);
		}
		return filter;
	}

	@JsonSetter("datumFilter")
	public void setFilter(DatumFilterCommand filter) {
		this.filter = filter;
		filterJson = null;
	}

}
