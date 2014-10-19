/* ==================================================================
 * DatumMetadataSecurityAspect.java - Oct 3, 2014 4:21:36 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Security AOP support for {@link DatumMetadataBiz}.
 * 
 * @author matt
 * @version 1.1
 */
@Aspect
public class DatumMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * The default value for the {@link #setLocaitonMetadataAdminRoles(Set)}
	 * property, contains the single role {@code ROLE_LOC_META_ADMIN}.
	 */
	public static final Set<String> DEFAULT_LOCATION_METADATA_ADMIN_ROLES = Collections
			.singleton("ROLE_LOC_META_ADMIN");

	private Set<String> locaitonMetadataAdminRoles = DEFAULT_LOCATION_METADATA_ADMIN_ROLES;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public DatumMetadataSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.addGeneralNode*(..)) && args(nodeId,..)")
	public void addMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.storeGeneralNode*(..)) && args(nodeId,..)")
	public void storeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.removeGeneralNode*(..)) && args(nodeId,..)")
	public void removeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.findGeneralNode*(..)) && args(filter,..)")
	public void findMetadata(GeneralNodeDatumMetadataFilter filter) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.addGeneralLocation*(..)) && args(locationId,..)")
	public void addLocationMetadata(Long locationId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.storeGeneralLocation*(..)) && args(locationId,..)")
	public void storeLocationMetadata(Long locationId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.DatumMetadata*.removeGeneralLocation*(..)) && args(locationId,..)")
	public void removeLocationMetadata(Long locationId) {
	}

	/**
	 * Check access to modifying datum metadata.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("addMetadata(nodeId) || storeMetadata(nodeId) || removeMetadata(nodeId)")
	public void updateMetadataCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to reading datum metadata.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("findMetadata(filter)")
	public void readMetadataCheck(GeneralNodeDatumMetadataFilter filter) {
		requireNodeReadAccess(filter == null ? null : filter.getNodeId());
	}

	@Before("addLocationMetadata(locationId) || storeLocationMetadata(locationId) || removeLocationMetadata(locationId)")
	public void updateLocationMetadataCheck(Long locationId) {
		SecurityUtils.requireAnyRole(locaitonMetadataAdminRoles);
	}

	/**
	 * Set the set of roles required to administer location metadata. If more
	 * than one role is provided, any one role must match for the authorization
	 * to succeed.
	 * 
	 * @param locaitonMetadataAdminRoles
	 *        the set of roles
	 * @since 1.2
	 */
	public void setLocaitonMetadataAdminRoles(Set<String> locaitonMetadataAdminRoles) {
		if ( locaitonMetadataAdminRoles == null || locaitonMetadataAdminRoles.size() < 1 ) {
			throw new IllegalArgumentException(
					"The roleLocationMetadataAdmin argument must not be null or empty.");
		}
		Set<String> capitalized;
		if ( locaitonMetadataAdminRoles.size() == 1 ) {
			capitalized = Collections.singleton(locaitonMetadataAdminRoles.iterator().next()
					.toUpperCase());
		} else {
			capitalized = new HashSet<String>(locaitonMetadataAdminRoles.size());
			for ( String role : locaitonMetadataAdminRoles ) {
				capitalized.add(role.toUpperCase());
			}
		}
		this.locaitonMetadataAdminRoles = capitalized;
	}

}
