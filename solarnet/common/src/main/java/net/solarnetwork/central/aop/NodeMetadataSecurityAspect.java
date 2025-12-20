/* ==================================================================
 * NodeMetadataSecurityAspect.java - 16/12/2025 7:03:41 am
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

package net.solarnetwork.central.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.dao.FilterResults;

/**
 * Security AOP support for {@link SolarNodeMetadataBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class NodeMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 */
	public NodeMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match add/store/remove metadata methods.
	 * 
	 * @param nodeId
	 *        the node ID to modify metadata on
	 */
	@Pointcut("execution(* net.solarnetwork.central.biz.SolarNodeMetadataBiz.*(..)) && args(nodeId,..)")
	public void modifyNodeMetadata(Long nodeId) {
	}

	/**
	 * Match add/store/remove metadata methods.
	 * 
	 * @param filter
	 *        the search criteria
	 */
	@Pointcut("execution(* net.solarnetwork.central.biz.SolarNodeMetadataBiz.find*(..)) && args(filter,..)")
	public void findNodeMetadata(SolarNodeMetadataFilter filter) {
	}

	/**
	 * Check access to modify node metadata.
	 *
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "modifyNodeMetadata(nodeId)", argNames = "nodeId")
	public void modifyNodeMetadataAccessCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to find node metadata.
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to check
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 */
	@Around(value = "findNodeMetadata(filter)")
	public FilterResults<SolarNodeMetadataFilterMatch, Long> findNodeMetadataAccessCheck(
			ProceedingJoinPoint pjp, SolarNodeMetadataFilter filter) throws Throwable {
		if ( filter == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		final Object[] args = pjp.getArgs();

		SolarNodeMetadataFilter f = policyEnforcerCheck(filter);
		if ( f != filter ) {
			args[0] = f;
		}

		if ( filter.getNodeId() == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		@SuppressWarnings("unchecked")
		var result = (FilterResults<SolarNodeMetadataFilterMatch, Long>) pjp.proceed(args);
		return result;
	}

}
