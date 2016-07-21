/* ==================================================================
 * RegistrationSecurityAspect.java - 21/07/2016 7:51:38 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link RegistrationBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class RegistrationSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public RegistrationSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*RegistrationBiz.renewNodeCertificate(..)) && args(userNode, ..)")
	public void renewNodeCertificate(UserNode userNode) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*RegistrationBiz.getPendingNodeCertificateRenewal(..)) && args(userNode, ..)")
	public void getPendingNodeCertificateRenewal(UserNode userNode) {
	}

	@Before("renewNodeCertificate(userNode) || getPendingNodeCertificateRenewal(userNode)")
	public void processNodeCertificateCheck(UserNode userNode) {
		requireNodeWriteAccess(userNode.getNode().getId());
	}

}
