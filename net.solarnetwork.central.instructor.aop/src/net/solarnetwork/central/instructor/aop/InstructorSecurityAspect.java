/* ==================================================================
 * InstructorSecurityAspect.java - Nov 27, 2012 8:57:43 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.aop;

import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Security aspect for {@link InstructorBiz}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class InstructorSecurityAspect {

	@Autowired
	private UserNodeDao userNodeDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.instructor.biz.InstructorBiz.getActiveInstructionsForNode(Long))")
	public void instructionsForNode() {
	}

	/**
	 * Allow the current user access to a report if they are a member of the
	 * report team, including guests.
	 * 
	 * @param reportId
	 *        the ID of the report to verify
	 */
	@Before("((instructionsForNode()) && args(nodeId))")
	public void reportTeamMemberAccessCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		final SecurityUser actor = SecurityUtils.getCurrentUser();
		if ( actor == null ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(null, AuthorizationException.Reason.ACCESS_DENIED);
		}
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			log.warn("Access DENIED to node {} for user {}; not found", nodeId, actor.getEmail());
			throw new AuthorizationException(actor.getEmail(),
					AuthorizationException.Reason.ACCESS_DENIED);
		}
		if ( !actor.getUserId().equals(userNode.getUser().getId()) ) {
			log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, actor.getEmail());
			throw new AuthorizationException(actor.getEmail(),
					AuthorizationException.Reason.ACCESS_DENIED);
		}
	}

	public void setUserNodeDao(UserNodeDao userNodeDao) {
		this.userNodeDao = userNodeDao;
	}

}
