/* ==================================================================
 * UserAlertSituation.java - 15/05/2015 12:00:27 pm
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
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A triggered alert condition.
 * 
 * @author matt
 * @version 2.0
 */
public class UserAlertSituation extends BaseEntity {

	private static final long serialVersionUID = -6858352050110675205L;

	private UserAlert alert;
	private UserAlertSituationStatus status;
	private Instant notified;
	private Map<String, Object> info;
	private String infoJson;

	public UserAlert getAlert() {
		return alert;
	}

	public void setAlert(UserAlert alert) {
		this.alert = alert;
	}

	public UserAlertSituationStatus getStatus() {
		return status;
	}

	public void setStatus(UserAlertSituationStatus status) {
		this.status = status;
	}

	public Instant getNotified() {
		return notified;
	}

	public void setNotified(Instant notified) {
		this.notified = notified;
	}

	/**
	 * Get the info object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore <em>null</em> values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never <em>null</em>
	 * @since 1.1
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getInfoJson() {
		if ( infoJson == null ) {
			infoJson = JsonUtils.getJSONString(info, "{}");
		}
		return infoJson;
	}

	/**
	 * Set the info object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created info and replace it with
	 * the values parsed from the JSON. All floating point values will be
	 * converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 * @since 1.1
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setInfoJson(String json) {
		infoJson = json;
		info = null;
	}

	/**
	 * Get the info object.
	 * 
	 * @return the info object
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	@JsonProperty
	public Map<String, Object> getInfo() {
		if ( info == null && infoJson != null ) {
			info = JsonUtils.getObjectFromJSON(infoJson, Map.class);
		}
		return info;
	}

	/**
	 * Set the info instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setInfoJson(String)} as well.
	 * </p>
	 * 
	 * @param info
	 *        the info to set
	 * @since 1.1
	 */
	@JsonProperty
	public void setInfo(Map<String, Object> info) {
		this.info = info;
		infoJson = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserAlertSituation{id=");
		builder.append(getId());
		builder.append(", created=");
		builder.append(getCreated());
		builder.append(", alert=");
		builder.append(alert);
		builder.append(", status=");
		builder.append(status);
		builder.append(", notified=");
		builder.append(notified);
		builder.append("}");
		return builder.toString();
	}

}
