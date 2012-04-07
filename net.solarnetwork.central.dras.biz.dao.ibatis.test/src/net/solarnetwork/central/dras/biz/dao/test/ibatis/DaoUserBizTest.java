/* ==================================================================
 * DaoUserBizTest.java - Jun 12, 2011 9:30:54 PM
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

package net.solarnetwork.central.dras.biz.dao.test.ibatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import net.solarnetwork.central.dras.biz.UserBiz;
import net.solarnetwork.central.dras.biz.dao.DaoUserBiz;
import net.solarnetwork.central.dras.dao.UserGroupDao;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.support.MembershipCommand.Mode;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test case for {@link UserBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class DaoUserBizTest extends AbstractTestSupport {

	@Autowired private UserGroupDao userGroupDao;
	@Autowired private DaoUserBiz userBiz;
	
	@Test
	public void getNoUser() {
		User user = userBiz.getUser(-8888L);
		assertNull(user);
	}
	
	@Before
	public void setupSecurity() {
		SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "unittest"));
	}
	
	@Test
	public void assignNewMembers() {
		setupTestLocation();
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		setupTestUser(-8888L, "User A");
		setupTestUser(-8887L, "User B");

		// verify no users in group
		Set<Member> members = userGroupDao.getMembers(TEST_GROUP_ID, null);
		assertNotNull(members);
		assertEquals(0, members.size());
		
		members.add(new User(-8888L));
		members.add(new User(-8887L));
		
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(TEST_GROUP_ID);
		membership.setMode(Mode.Replace);
		membership.setMembers(members);
		
		EffectiveCollection<UserGroup, ? extends Member> result 
			= userBiz.assignUserGroupMembers(membership);
		assertNotNull(result);
		assertNotNull(result.getObject());
		assertEquals(TEST_GROUP_ID, result.getObject().getId());
		assertNotNull(result.getEffective());
		assertNotNull(result.getEffective().getId());
		assertNotNull(result.getCollection());
		
		Collection<? extends Member> assigned = result.getCollection();
		assertEquals(members.size(), assigned.size());
		assertTrue(members.containsAll(assigned));
	}
	
}
