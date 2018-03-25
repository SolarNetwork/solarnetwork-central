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

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.export.DataConfiguration;
import net.solarnetwork.util.JsonUtils;

/**
 * User related entity for {@link DataConfiguration}.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDataConfiguration extends BaseExportConfigurationEntity
		implements DataConfiguration, UserIdentifiableConfiguration, Serializable {

	private static final long serialVersionUID = 866381003784859350L;

	private String filterJson;
	private DatumFilterCommand filter;

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
