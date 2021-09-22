/* ==================================================================
 * UserAlert.java - 15/05/2015 11:31:07 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.math.BigDecimal;
import java.util.Map;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * An alert condition definition. User alerts are designed to cover conditions
 * such as
 * 
 * <ul>
 * <li>node <em>X</em> has not posted data in <em>Y</em> hours</li>
 * <li>node <em>X</em> posted value <em>V</em> for property <em>A</em> greater
 * than <em>T</em></li>
 * <li>location <em>L</em> posted value <em>V</em> for property <em>A</em> less
 * than <em>T</em></li>
 * </ul>
 * 
 * @author matt
 * @version 2.0
 */
@JsonPropertyOrder({ "id", "created", "userId", "nodeId", "type", "status", "validTo", "options" })
public class UserAlert extends BaseEntity implements UserRelatedEntity<Long> {

	private static final long serialVersionUID = 1374111067444093568L;

	private Long userId;
	private UserAlertType type;
	private UserAlertStatus status;
	private Long nodeId;
	private DateTime validTo;
	private Map<String, Object> options;
	private String optionsJson;

	// transient
	private UserAlertSituation situation;

	/**
	 * Get the options object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore <em>null</em> values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never <em>null</em>
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getOptionsJson() {
		if ( optionsJson == null ) {
			optionsJson = JsonUtils.getJSONString(options, "{}");
		}
		return optionsJson;
	}

	/**
	 * Set the options object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created options and replace it
	 * with the values parsed from the JSON. All floating point values will be
	 * converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setOptionsJson(String json) {
		optionsJson = json;
		options = null;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public UserAlertType getType() {
		return type;
	}

	public void setType(UserAlertType type) {
		this.type = type;
	}

	public UserAlertStatus getStatus() {
		return status;
	}

	public void setStatus(UserAlertStatus status) {
		this.status = status;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public DateTime getValidTo() {
		return validTo;
	}

	public void setValidTo(DateTime validTo) {
		this.validTo = validTo;
	}

	@SuppressWarnings("unchecked")
	@JsonProperty
	public Map<String, Object> getOptions() {
		if ( options == null && optionsJson != null ) {
			options = JsonUtils.getObjectFromJSON(optionsJson, Map.class);
		}
		return options;
	}

	/**
	 * Set the options instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setOptionsJson(String)} as well.
	 * </p>
	 * 
	 * @param options
	 *        the samples instance to set
	 */
	@JsonProperty
	public void setOptions(Map<String, Object> options) {
		this.options = options;
		optionsJson = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserAlert{id=");
		builder.append(getId());
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", status=");
		builder.append(status);
		if ( nodeId != null ) {
			builder.append(", ");
			builder.append("nodeId=");
			builder.append(nodeId);
		}
		builder.append("}");
		return builder.toString();
	}

	public UserAlertSituation getSituation() {
		return situation;
	}

	public void setSituation(UserAlertSituation situation) {
		this.situation = situation;
	}

}
