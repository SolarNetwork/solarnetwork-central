/* ==================================================================
 * DaoBizSupport.java - Jun 11, 2011 10:25:12 PM
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

package net.solarnetwork.central.dras.biz.dao;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.support.SimpleUserFilter;

import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Support for DAO  Biz implementations.
 * 
 * @author matt
 * @version $Revision$
 */
public class DaoBizSupport {

	protected EffectiveDao effectiveDao;
	protected LocationDao locationDao;
	protected UserDao userDao;
	
	/**
	 * Get the ID of the acting user.
	 * 
	 * @return ID, or <em>null</em> if not available
	 */
	protected Long getCurrentUserId() {
		String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUniqueId(currentUserName);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		if ( results.getReturnedResultCount() < 1 ) {
			return null;
		}
		return results.getResults().iterator().next().getId();
	}
	
	/**
	 * Create a new Effective entity.
	 * 
	 * @param effective the effective date
	 * @return the Effective
	 */
	protected Effective createEffective(DateTime effectiveDate) {
		Effective eff = new Effective();
		eff.setCreator(getCurrentUserId());
		eff.setEffectiveDate(effectiveDate);
		Long id = effectiveDao.store(eff);
		return effectiveDao.get(id);
	}

	/**
	 * API used in {@link DaoBizSupport#maintainGroupMembership(MembershipCommand, MembershipMaintenance)}.
	 * 
	 * @param <T> the group (parent) object type
	 */
	protected interface MembershipMaintenance<T extends Entity<Long>, E extends Member> {
		
		/**
		 * Get the DAO for accessing the group (parent) object.
		 * @return DAO
		 */
		GenericDao<T, Long> getDao();
		
		/**
		 * Create a new member object.
		 * 
		 * @param memberId the member ID
		 * @return the member
		 */
		E createMember(Long memberId);
		
		/**
		 * Get the set of members for a given group (parent) object.
		 * 
		 * @param parentId the parent ID
		 * @param eff the effective
		 * @return the set of members (never <em>null</em>)
		 */
		Set<E> getMembers(Long parentId, Effective eff);
		
		/**
		 * Completely assign the members of a given group (parent) object.
		 * 
		 * @param parentId the parent ID
		 * @param newMembers the new group membership IDs
		 * @param eff the effective
		 */
		void assignMembers(Long parentId, Set<Long> newMembers, Effective eff);
		
	}
	
	/**
	 * Helper method for maintaining the membership relationship in a group.
	 * 
	 * @param membership the command
	 * @param access API for updating the group membership
	 * @return the EffectiveCollection representing the new membership status
	 */
	protected <T extends Entity<Long>, E extends Member> 
	EffectiveCollection<T, E> maintainGroupMembership(
			MembershipCommand membership,  MembershipMaintenance<T, E> access) {
		Effective eff;
		boolean newEffective = false;
		if ( membership.getEffectiveId() != null ) {
			eff = effectiveDao.get(membership.getEffectiveId());
		} else {
			eff = createEffective(null);
			newEffective = true;
		}
		Set<E> members = access.getMembers(membership.getParentId(), eff);
		Set<E> newMembers = new HashSet<E>(members.size()
				+membership.getGroup().size());
		Set<E> actionMembers = new LinkedHashSet<E>(membership.getGroup().size());
		for ( Long memberId : membership.getGroup() ) {
			actionMembers.add(access.createMember(memberId));
		}
		
		switch ( membership.getMode() ) {
		case Append:
			newMembers.addAll(members);
			newMembers.addAll(actionMembers);
			break;
			
		case Delete:
			newMembers.addAll(members);
			newMembers.removeAll(actionMembers);
			break;
			
		case Replace:
			newMembers.addAll(actionMembers);
		
		}
		
		if ( newMembers.equals(members) ) {
			// no change, clean up and return
			if ( newEffective ) {
				effectiveDao.delete(eff);
				eff = null; // TODO: or get the actual Effective from userGroupDao?
			}
		} else {
			Set<Long> ids = new HashSet<Long>(newMembers.size());
			for ( Member m : newMembers ) {
				ids.add(m.getId());
			}
			access.assignMembers(membership.getParentId(), ids, eff);
		}
		
		T userGroup = access.getDao().get(membership.getParentId());
		EffectiveCollection<T, E> result 
				= new EffectiveCollection<T, E>(eff, userGroup, newMembers);
		return result;
	}

}
