/* ==================================================================
 * Tag.java - 6/09/2017 5:12:16 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.domain;

/**
 * A tag associated with some object, such as an account.
 * 
 * @author matt
 * @version 1.0
 */
public class Tag {

	private String tagId;
	private String objectType;
	private String objectId;
	private String tagDefinitionId;
	private String tagDefinitionName;

	/**
	 * Get the unique ID.
	 * 
	 * @return the tagId
	 */
	public String getTagId() {
		return tagId;
	}

	/**
	 * Set the unique ID.
	 * 
	 * @param tagId
	 *        the tagId to set
	 */
	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

	/**
	 * Get the type of object the tag is associated with.
	 * 
	 * @return the type
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * Set the type of object the tag is associated with.
	 * 
	 * @param objectType
	 *        the type to set
	 */
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	/**
	 * Get the unique ID of the object the tag is associated with.
	 * 
	 * @return the object ID
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * Set the unique ID of the object the tag is associated with.
	 * 
	 * @param objectId
	 *        the object ID to set
	 */
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	/**
	 * Get the tag definition ID.
	 * 
	 * @return the tag definition ID
	 */
	public String getTagDefinitionId() {
		return tagDefinitionId;
	}

	/**
	 * Set the tag definition ID.
	 * 
	 * @param tagDefinitionId
	 *        the tag definition ID to set
	 */
	public void setTagDefinitionId(String tagDefinitionId) {
		this.tagDefinitionId = tagDefinitionId;
	}

	/**
	 * Get the tag definition name.
	 * 
	 * @return the tag definition name
	 */
	public String getTagDefinitionName() {
		return tagDefinitionName;
	}

	/**
	 * Set the tag definition name.
	 * 
	 * @param tagDefinitionName
	 *        the tag definition name to set
	 */
	public void setTagDefinitionName(String tagDefinitionName) {
		this.tagDefinitionName = tagDefinitionName;
	}

}
