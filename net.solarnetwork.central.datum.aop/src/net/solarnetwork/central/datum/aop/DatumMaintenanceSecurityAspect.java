/* ==================================================================
 * DatumMaintenanceSecurityAspect.java - 10/04/2019 11:28:24 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security AOP support for {@link DatumMaintenanceBiz}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.7
 */
@Aspect
public class DatumMaintenanceSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 * @param nodeDatumDao
	 *        the datum DAO to use
	 */
	public DatumMaintenanceSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMaintenanceBiz.mark*(..)) && args(filter,..)")
	public void markStale(GeneralNodeDatumFilter filter) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMaintenanceBiz.find*(..)) && args(filter,..)")
	public void findStale(GeneralNodeDatumFilter filter) {
	}

	/**
	 * Check access to marking datum aggregates stale.
	 * 
	 * <p>
	 * This verifies that the actor has write access to the node IDs in the
	 * filter.
	 * </p>
	 * 
	 * @param filter
	 *        the filter to verify
	 */
	@Before("markStale(filter) || findStale(filter)")
	public void staleFilterCheck(GeneralNodeDatumFilter filter) {
		if ( filter != null ) {
			Long[] nodeIds = filter.getNodeIds();
			if ( nodeIds == null || nodeIds.length < 1 ) {
				log.warn("Access DENIED to unspecified nodes");
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
			if ( nodeIds != null ) {
				for ( Long nodeId : filter.getNodeIds() ) {
					requireNodeWriteAccess(nodeId);
				}
			}
		}
	}

}
