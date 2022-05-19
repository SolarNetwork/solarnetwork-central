/* ==================================================================
 * LocationRequest.java - 19/05/2022 1:37:42 pm
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

package net.solarnetwork.central.domain;

import java.time.Instant;
import net.solarnetwork.dao.BasicLongEntity;

/**
 * Entity for location requests.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class LocationRequest extends BasicLongEntity {

	private static final long serialVersionUID = 7700770585486402946L;

	private Instant modified;
	private Long userId;
	private LocationRequestStatus status;
	private Long locationId;
	private String message;
	private String jsonData;

	/**
	 * Default constructor.
	 */
	public LocationRequest() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public LocationRequest(Long id, Instant created) {
		super(id, created);
	}

	/**
	 * Create a copy with a given ID.
	 * 
	 * @param id
	 *        the ID to use
	 * @return the new copy
	 */
	public LocationRequest withId(Long id) {
		LocationRequest req = new LocationRequest(id, getCreated());
		req.setModified(getModified());
		req.setUserId(getUserId());
		req.setStatus(getStatus());
		req.setLocationId(req.getLocationId());
		req.setMessage(getMessage());
		req.setJsonData(getJsonData());
		return req;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LocationRequest{");
		if ( getId() != null ) {
			builder.append("id=");
			builder.append(getId());
			builder.append(", ");
		}
		if ( userId != null ) {
			builder.append("userId=");
			builder.append(userId);
			builder.append(", ");
		}
		if ( status != null ) {
			builder.append("status=");
			builder.append(status);
			builder.append(", ");
		}
		if ( locationId != null ) {
			builder.append("locationId=");
			builder.append(locationId);
			builder.append(", ");
		}
		if ( message != null ) {
			builder.append("message=");
			builder.append(message);
			builder.append(", ");
		}
		if ( jsonData != null ) {
			builder.append("jsonData=");
			builder.append(jsonData);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modified
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 * 
	 * @param modified
	 *        the modified to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 * 
	 * @param userId
	 *        the userId to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the status.
	 * 
	 * @return the status
	 */
	public LocationRequestStatus getStatus() {
		return status;
	}

	/**
	 * Set the status.
	 * 
	 * @param status
	 *        the status to set
	 */
	public void setStatus(LocationRequestStatus status) {
		this.status = status;
	}

	/**
	 * Get the location ID.
	 * 
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the location ID.
	 * 
	 * @param locationId
	 *        the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * Get the message.
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message.
	 * 
	 * @param message
	 *        the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Set the JSON data.
	 * 
	 * @return the JSON data
	 */
	public String getJsonData() {
		return jsonData;
	}

	/**
	 * Get the JSON data.
	 * 
	 * @param jsonData
	 *        the jsonData to set
	 */
	public void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

}
