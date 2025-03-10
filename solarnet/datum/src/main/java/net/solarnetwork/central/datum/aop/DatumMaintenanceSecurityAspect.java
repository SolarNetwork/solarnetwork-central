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
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security AOP support for {@link DatumMaintenanceBiz}.
 *
 * @author matt
 * @version 2.0
 * @since 1.7
 */
@Aspect
@Component
public class DatumMaintenanceSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership to use
	 */
	public DatumMaintenanceSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMaintenanceBiz.mark*(..)) && args(filter,..)")
	public void markStale(GeneralNodeDatumFilter filter) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMaintenanceBiz.find*(..)) && args(filter,..)")
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
	@Before(value = "markStale(filter) || findStale(filter)", argNames = "filter")
	public void staleFilterCheck(GeneralNodeDatumFilter filter) {
		if ( filter != null ) {
			Long[] nodeIds = filter.getNodeIds();
			if ( nodeIds == null || nodeIds.length < 1 ) {
				log.warn("Access DENIED to unspecified nodes");
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
			for ( Long nodeId : filter.getNodeIds() ) {
				requireNodeWriteAccess(nodeId);
			}
		}
	}

}
