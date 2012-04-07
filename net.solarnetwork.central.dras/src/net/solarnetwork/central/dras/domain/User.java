/* ==================================================================
 * User.java - Jun 2, 2011 5:40:07 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * A system user account.
 * 
 * @author matt
 * @version $Revision$
 */
public class User extends BaseEntity implements Cloneable, Serializable, Member, Match {

	private static final long serialVersionUID = 5990626944355176113L;

	/**
	 * Default constructor.
	 */
	public User() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * 
	 * @param id the ID
	 */
	public User(Long id) {
		super();
		setId(id);
	}
	
	@NotNull
	private String username;
	
	private String password;
	
	@NotNull
	private String displayName;
	
	private String[] address;
	private String vendor;
	private Boolean enabled;
	private List<UserContact> contactInfo = new ArrayList<UserContact>(2);
	private List<String> roleNames;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String[] getAddress() {
		return address;
	}
	public void setAddress(String[] address) {
		this.address = address;
	}
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	public List<UserContact> getContactInfo() {
		return contactInfo;
	}
	public void setContactInfo(List<UserContact> contactInfo) {
		this.contactInfo = contactInfo;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public List<String> getRoleNames() {
		return roleNames;
	}
	public void setRoleNames(List<String> roleNames) {
		this.roleNames = roleNames;
	}
	
}
