/* ==================================================================
 * SolarNodeMetadataSecurityAspect.java - 11/11/2016 1:37:56 PM
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

package net.solarnetwork.central.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security AOP support for {@link SolarNodeMetadataBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class SolarNodeMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public SolarNodeMetadataSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.biz.SolarNodeMetadata*.addSolarNode*(..)) && args(nodeId,..)")
	public void addMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.biz.SolarNodeMetadata*.storeSolarNode*(..)) && args(nodeId,..)")
	public void storeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.biz.SolarNodeMetadata*.removeSolarNode*(..)) && args(nodeId)")
	public void removeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.biz.SolarNodeMetadata*.findSolarNode*(..)) && args(filter,..)")
	public void findMetadata(SolarNodeMetadataFilter filter) {
	}

	/**
	 * Check access to modifying node metadata.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("addMetadata(nodeId) || storeMetadata(nodeId) || removeMetadata(nodeId)")
	public void updateMetadataCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to reading node metadata.
	 * 
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to verify
	 */
	@Around("findMetadata(filter)")
	public Object readMetadataCheck(ProceedingJoinPoint pjp, SolarNodeMetadataFilter filter)
			throws Throwable {
		Long[] nodeIds = (filter == null ? null : filter.getNodeIds());
		if ( nodeIds == null ) {
			log.warn("Access DENIED to node metadata without node ID filter");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long nodeId : nodeIds ) {
			requireNodeReadAccess(nodeId);
		}

		// node ID passes, execute query and then filter based on security policy if necessary
		Object result = pjp.proceed();

		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null || policy.getNodeMetadataPaths() == null
				|| policy.getNodeMetadataPaths().isEmpty() ) {
			return result;
		}

		return policyEnforcerCheck(result, SecurityPolicyMetadataType.Node);
	}

}
