/* ==================================================================
 * BasicInputConfiguration.java - 7/11/2018 11:17:52 AM
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

package net.solarnetwork.central.datum.imp.domain;

import java.io.Serializable;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Basic implementation of {@link InputConfiguration}.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "name", "serviceIdentifier", "timeZoneId", "serviceProperties" })
public class BasicInputConfiguration extends BasicIdentifiableConfiguration
		implements InputConfiguration, Serializable {

	private static final long serialVersionUID = 4114494359629338909L;

	private String timeZoneId;

	/**
	 * Default constructor.
	 */
	public BasicInputConfiguration() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the configuration to copy
	 */
	public BasicInputConfiguration(InputConfiguration other) {
		super(other);
		setTimeZoneId(other.getTimeZoneId());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicInputConfiguration{");
		if ( getName() != null ) {
			builder.append("name=");
			builder.append(getName());
			builder.append(", ");
		}
		if ( getServiceIdentifier() != null ) {
			builder.append("serviceIdentifier=");
			builder.append(getServiceIdentifier());
			builder.append(", ");
		}
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
			builder.append(", ");
		}
		Map<String, Object> props = getServiceProps();
		if ( props != null ) {
			builder.append("serviceProps=");
			Map<String, Object> maskedServiceProps = StringUtils.sha256MaskedMap(props,
					CollectionUtils.sensitiveNamesToMask(props.keySet()));
			builder.append(maskedServiceProps);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
