/* ==================================================================
 * MembershipCommand.java - Jun 12, 2011 5:29:40 PM
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

package net.solarnetwork.central.dras.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.solarnetwork.central.dras.domain.Member;

/**
 * Command object for maintaining group membership.
 * 
 * @param <E> the collection element type
 * @param <T> the parent ID type
 * @author matt
 * @version $Revision$
 */
public class MembershipCommand implements Cloneable {

	public enum Mode {
		Append,
		Delete,
		Replace,
	}
	
	private Long parentId;
	private Long effectiveId;
	private List<Long> group = new ArrayList<Long>();
	private Mode mode = Mode.Replace;
	
	@Override
	public Object clone() {
		MembershipCommand other;
		try {
			other = (MembershipCommand) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should not get here
			throw new RuntimeException("Could not clone MembershipCommand");
		}
		if ( other.group != null ) {
			other.group = new ArrayList<Long>(other.group);
		}
		return other;
	}

	/**
	 * Set the {@code groups} property with a collection of {@link Member#getId()} values.
	 * 
	 * @param members the members to set the IDs with
	 */
	public void setMembers(Collection<Member> members) {
		List<Long> ids = new ArrayList<Long>(members.size());
		for ( Member m : members ) {
			ids.add(m.getId());
		}
		setGroup(ids);
	}
	
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	public Long getEffectiveId() {
		return effectiveId;
	}
	public void setEffectiveId(Long effectiveId) {
		this.effectiveId = effectiveId;
	}
	public Mode getMode() {
		return mode;
	}
	public void setMode(Mode mode) {
		this.mode = mode;
	}
	public List<Long> getGroup() {
		return group;
	}
	public void setGroup(List<Long> group) {
		this.group = group;
	}
	
}
