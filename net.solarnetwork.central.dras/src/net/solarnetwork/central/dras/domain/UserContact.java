/* ==================================================================
 * UserContact.java - Jun 2, 2011 5:43:18 PM
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


/**
 * User contact information.
 * 
 * @author matt
 * @version $Revision$
 */
public class UserContact implements Cloneable, Serializable {
	
	private static final long serialVersionUID = 4053017824870846830L;

	/**
	 * The contact method type.
	 */
	public enum ContactKind {
		EMAIL, MOBILE, VOICE, PAGER, FAX,
	}
	
	private ContactKind kind;
	private String contact;
	private Integer priority;
	
	/**
	 * Default constructor.
	 */
	public UserContact() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param kind the kind
	 * @param contact the contact
	 * @param priority the priority
	 */
	public UserContact(ContactKind kind, String contact, Integer priority) {
		super();
		setKind(kind);
		setContact(contact);
		setPriority(priority);
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contact == null) ? 0 : contact.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result
				+ ((priority == null) ? 0 : priority.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof UserContact)) {
			return false;
		}
		UserContact other = (UserContact) obj;
		if (contact == null) {
			if (other.contact != null) {
				return false;
			}
		} else if (!contact.equals(other.contact)) {
			return false;
		}
		if (kind != other.kind) {
			return false;
		}
		if (priority == null) {
			if (other.priority != null) {
				return false;
			}
		} else if (!priority.equals(other.priority)) {
			return false;
		}
		return true;
	}

	public ContactKind getKind() {
		return kind;
	}
	public void setKind(ContactKind kind) {
		this.kind = kind;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

}
