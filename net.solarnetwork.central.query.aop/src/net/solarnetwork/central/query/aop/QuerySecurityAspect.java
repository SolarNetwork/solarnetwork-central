/* ==================================================================
 * QuerySecurityAspect.java - Dec 18, 2012 4:32:34 PM
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

package net.solarnetwork.central.query.aop;

import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security enforcing AOP aspect for {@link QueryBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class QuerySecurityAspect {

	private final UserNodeDao userNodeDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public QuerySecurityAspect(UserNodeDao userNodeDao) {
		super();
		this.userNodeDao = userNodeDao;
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getReportableInterval(..)) && args(nodeId,..)")
	public void nodeReportableInterval(Long nodeId) {
	}

	/**
	 * Allow the current user (or current node) access to node data.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("nodeReportableInterval(nodeId)")
	public void userNodeAccessCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			log.warn("Access DENIED to node {}; not found", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, nodeId);
		}
		if ( !userNode.isRequiresAuthorization() ) {
			return;
		}

		final SecurityActor actor = SecurityUtils.getCurrentActor();
		if ( actor == null ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			if ( !nodeId.equals(node.getNodeId()) ) {
				log.warn("Access DENIED to node {} for node {}; wrong node", nodeId, node.getNodeId());
				throw new AuthorizationException(node.getNodeId().toString(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			if ( !user.getUserId().equals(userNode.getUser().getId()) ) {
				log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, user.getEmail());
				throw new AuthorizationException(user.getEmail(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			if ( UserAuthTokenType.User.toString().equals(token.getTokenType()) ) {
				// user token, so user ID must match node user's ID
				if ( token.getUserId().equals(userNode.getUser().getId()) ) {
					log.warn("Access DENIED to node {} for token {}; wrong user", nodeId,
							token.getToken());
					throw new AuthorizationException(token.getToken(),
							AuthorizationException.Reason.ACCESS_DENIED);
				}
				return;
			}
			if ( UserAuthTokenType.ReadNodeData.toString().equals(token.getTokenType()) ) {
				// data token, so token must include the requested node ID
				if ( token.getTokenIds() == null || !token.getTokenIds().contains(nodeId) ) {
					log.warn("Access DENIED to node {} for token {}; node not included", nodeId,
							token.getToken());
					throw new AuthorizationException(token.getToken(),
							AuthorizationException.Reason.ACCESS_DENIED);
				}
				return;
			}
		}

		log.warn("Access DENIED to node {} for actor {}", nodeId, actor);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
	}

}
