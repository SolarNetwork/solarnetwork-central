/* ==================================================================
 * BaseExportConfigurationEntity.java - 21/03/2018 1:46:50 PM
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
import java.math.BigDecimal;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Base class for export configuration entities.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseExportConfigurationEntity extends BaseEntity
		implements UserIdentifiableConfiguration, Serializable {

	private static final long serialVersionUID = -1417068116997904853L;

	private Long userId;
	private String name;
	private String serviceIdentifier;
	private String servicePropsJson;

	private Map<String, Object> serviceProps;

	@Override
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getServiceIdentifier() {
		return serviceIdentifier;
	}

	public void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
	}

	/**
	 * Get the service properties object as a JSON string.
	 * 
	 * @return a JSON encoded string, or {@literal null} if no service
	 *         properties available
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getServicePropsJson() {
		if ( servicePropsJson == null ) {
			servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
		}
		return servicePropsJson;
	}

	/**
	 * Set the service properties object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created service properties and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to parse as service properties
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setServicePropsJson(String json) {
		servicePropsJson = json;
		serviceProps = null;
	}

	@JsonIgnore
	public Map<String, Object> getServiceProps() {
		if ( serviceProps == null && servicePropsJson != null ) {
			serviceProps = JsonUtils.getStringMap(servicePropsJson);
		}
		return serviceProps;
	}

	/**
	 * Set the service properties to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setServicePropsJson(String)} as well.
	 * </p>
	 * 
	 * @param serviceProps
	 *        the service properties to set
	 */
	@JsonSetter("serviceProperties")
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = null;
	}

	@Override
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
