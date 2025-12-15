/* ==================================================================
 * RegistrationSecurityAspect.java - 15/12/2025 12:12:30 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Security enforcing AOP aspect for {@link RegistrationBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class RegistrationSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public RegistrationSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match creating a node manually.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.createNodeManually(..))")
	public void createNodeManually() {
	}

	/**
	 * Match renewing a node certificate.
	 * 
	 * @param userNode
	 *        the node to renew the certificate for
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.renewNodeCertificate(..)) && args(userNode,..)")
	public void renewCertificate(UserNode userNode) {
	}

	/**
	 * Enforce an unrestricted policy.
	 */
	@Before(value = "createNodeManually()")
	public void requireUnrestrictedPolicyAccessCheck() {
		if ( !SecurityUtils.policyIsUnrestricted(getActiveSecurityPolicy()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, SecurityUtils.currentTokenId());
		}
	}

	/**
	 * Enforce an unrestricted policy.
	 */
	@Before(value = "renewCertificate(userNode)")
	public void requireUserNodeWriteAccess(UserNode userNode) {
		final Long nodeId = userNode != null && userNode.getNode() != null ? userNode.getNode().getId()
				: null;
		if ( nodeId != null ) {
			requireNodeWriteAccess(nodeId);
		}
	}

}
