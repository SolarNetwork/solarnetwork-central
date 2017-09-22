/* ==================================================================
 * TagDefinition.java - 6/09/2017 4:33:19 PM
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

import java.util.Set;

/**
 * Killbill tag definition.
 * 
 * @author matt
 * @version 1.0
 */
public class TagDefinition {

	private String id;
	private String name;
	private String description;
	private Boolean isControlTag;
	private Set<String> applicableObjectTypes;

	/**
	 * Default constructor.
	 */
	public TagDefinition() {
		super();
	}

	/**
	 * Construct with ID and name.
	 * 
	 * @param id
	 *        the ID
	 * @param name
	 *        the name
	 */
	public TagDefinition(String id, String name) {
		super();
		setId(id);
		setName(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof TagDefinition) ) {
			return false;
		}
		TagDefinition other = (TagDefinition) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		return true;
	}

	/**
	 * Get the unique ID.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the unique ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        the description to set
	 */
	public void setEescription(String description) {
		this.description = description;
	}

	/**
	 * Get the control flag.
	 * 
	 * @return the flag
	 */
	public Boolean getIsControlTag() {
		return isControlTag;
	}

	/**
	 * Set the control flag.
	 * 
	 * @param isControlTag
	 *        the flag to set
	 */
	public void setIsControlTag(Boolean isControlTag) {
		this.isControlTag = isControlTag;
	}

	/**
	 * Get the set of object types this tag can be applied to.
	 * 
	 * @return the the object types
	 */
	public Set<String> getApplicableObjectTypes() {
		return applicableObjectTypes;
	}

	/**
	 * Set the object types this tag can be applied to.
	 * 
	 * @param applicableObjectTypes
	 *        the object types to set
	 */
	public void setApplicableObjectTypes(Set<String> applicableObjectTypes) {
		this.applicableObjectTypes = applicableObjectTypes;
	}

}
