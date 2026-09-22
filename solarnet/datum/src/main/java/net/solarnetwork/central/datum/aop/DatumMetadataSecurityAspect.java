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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security AOP support for {@link DatumMetadataBiz}.
 *
 * @author matt
 * @version 2.5
 */
@Aspect
@Component
public class DatumMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * The default value for the {@link #setLocationMetadataAdminRoles(Set)}
	 * property, contains the single role {@code ROLE_LOC_META_ADMIN}.
	 */
	public static final Set<String> DEFAULT_LOCATION_METADATA_ADMIN_ROLES = Collections
			.singleton("ROLE_LOC_META_ADMIN");

	private Set<String> locationMetadataAdminRoles = DEFAULT_LOCATION_METADATA_ADMIN_ROLES;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 */
	public DatumMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.addGeneralNode*(..)) && args(nodeId,..)")
	public void addMetadata(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.storeGeneralNode*(..)) && args(nodeId,..)")
	public void storeMetadata(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.removeGeneralNode*(..)) && args(nodeId,..)")
	public void removeMetadata(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.findGeneralNode*(..)) && args(filter,..)")
	public void findMetadata(GeneralNodeDatumMetadataFilter filter) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.getGeneralNodeDatumMetadataFilteredSources(..)) && args(nodeIds,..)")
	public void getMetadataFilteredSources(Long[] nodeIds) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.addGeneralLocation*(..)) && args(locationId,..)")
	public void addLocationMetadata(Long locationId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.storeGeneralLocation*(..)) && args(locationId,..)")
	public void storeLocationMetadata(Long locationId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.removeGeneralLocation*(..)) && args(locationId,..)")
	public void removeLocationMetadata(Long locationId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.findDatumStreamMetadata*(..)) && args(filter,..)")
	public void findDatumStreamMetadata(ObjectStreamCriteria filter) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.findLocationRequests(..)) && args(userId,..)")
	public void findLocationRequests(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.getLocationRequest(..)) && args(userId,..)")
	public void getLocationRequest(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.removeLocationRequest(..)) && args(userId,..)")
	public void removeLocationRequest(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.submitLocationRequest(..)) && args(userId,..)")
	public void submitLocationRequest(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.DatumMetadataBiz.updateLocationRequest(..)) && args(userId,..)")
	public void updateLocationRequest(Long userId) {
	}

	/**
	 * Check access to modifying datum metadata.
	 *
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before(value = "addMetadata(nodeId) || storeMetadata(nodeId) || removeMetadata(nodeId)",
			argNames = "nodeId")
	public void updateMetadataCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to reading datum metadata.
	 *
	 * <p>
	 * Read access is required for every node in the filter, and the filter is
	 * restricted to the nodes and sources of the active security policy.
	 * </p>
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to verify
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "findMetadata(filter)", argNames = "pjp,filter")
	public Object readMetadataAccessCheck(ProceedingJoinPoint pjp,
			GeneralNodeDatumMetadataFilter filter) throws Throwable {
		final Long[] nodeIds = (filter != null ? filter.getNodeIds() : null);
		if ( nodeIds == null || nodeIds.length < 1 ) {
			log.warn("Access DENIED to unspecified nodes");
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		for ( Long nodeId : nodeIds ) {
			requireNodeReadAccess(nodeId);
		}
		final GeneralNodeDatumMetadataFilter f = policyEnforcerCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}
		final @Nullable Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting the
	 * available sources for node datum metadata.
	 *
	 * <p>
	 * First the node ID is verified. Then, for all returned source ID values,
	 * if the active policy has no source ID restrictions return all values,
	 * otherwise remove any value not included in the policy.
	 * </p>
	 *
	 * @param pjp
	 *        The join point.
	 * @param nodeIds
	 *        The node IDs.
	 * @return The set of NodeSourcePK results.
	 * @throws Throwable
	 *         if any error occurs
	 * @since 1.2
	 */
	@Around(value = "getMetadataFilteredSources(nodeIds)", argNames = "pjp,nodeIds")
	public Object filteredMetadataSourcesAccessCheck(ProceedingJoinPoint pjp, Long[] nodeIds)
			throws Throwable {
		// verify node IDs
		if ( nodeIds != null ) {
			for ( Long nodeId : nodeIds ) {
				requireNodeReadAccess(nodeId);
			}
		}

		// verify source IDs in result
		@SuppressWarnings("unchecked")
		Set<NodeSourcePK> result = (Set<NodeSourcePK>) pjp.proceed();
		if ( result == null || result.isEmpty() ) {
			return result;
		}
		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null ) {
			return result;
		}
		Set<String> allowedSourceIds = policy.getSourceIds();
		if ( allowedSourceIds == null || allowedSourceIds.isEmpty() ) {
			return result;
		}
		Authentication authentication = SecurityUtils.getCurrentAuthentication();
		Object principal = (authentication != null ? authentication.getPrincipal() : null);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, principal, null,
				getPathMatcher());
		try {
			List<String> inputSourceIds = new ArrayList<>(result.size());
			for ( NodeSourcePK pk : result ) {
				inputSourceIds.add(pk.getSourceId());
			}
			String[] resultSourceIds = nonnull(
					enforcer.verifySourceIds(inputSourceIds.toArray(String[]::new)), "Input source IDs");
			Set<String> allowedSourceIdSet = new HashSet<>(Arrays.asList(resultSourceIds));
			Set<NodeSourcePK> restricted = new LinkedHashSet<>(resultSourceIds.length);
			for ( NodeSourcePK oneResult : result ) {
				if ( allowedSourceIdSet.contains(oneResult.getSourceId()) ) {
					restricted.add(oneResult);
				}
			}
			result = restricted;
		} catch ( AuthorizationException e ) {
			// ignore, and just  map to empty set
			result = Collections.emptySet();
		}
		return result;
	}

	@Before(value = "addLocationMetadata(locationId) || storeLocationMetadata(locationId) || removeLocationMetadata(locationId)",
			argNames = "locationId")
	public void updateLocationMetadataCheck(Long locationId) {
		SecurityUtils.requireAnyRole(locationMetadataAdminRoles);
	}

	/**
	 * Check access to reading datum metadata.
	 *
	 * <p>
	 * Location stream searches do not require any access. Node stream
	 * searches, including those within a location, require read access to
	 * every node and user in the filter, and at least one of either.
	 * </p>
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to verify
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 * @since 1.3
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "findDatumStreamMetadata(filter)", argNames = "pjp,filter")
	public Object findDatumStreamMetadataAccessCheck(ProceedingJoinPoint pjp,
			ObjectStreamCriteria filter) throws Throwable {
		if ( filter == null || (filter.getUserId() == null && filter.getNodeId() == null
				&& filter.getLocationId() == null) ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		if ( filter.effectiveObjectKind() == ObjectDatumKind.Location ) {
			// location searches do not require any check
			return pjp.proceed();
		}
		if ( filter.getUserId() == null && filter.getNodeId() == null ) {
			log.warn("Access DENIED to node streams without node or user criteria");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		Long[] ids = filter.getNodeIds();
		if ( ids != null ) {
			for ( Long nodeId : ids ) {
				requireNodeReadAccess(nodeId);
			}
		}
		ids = filter.getUserIds();
		if ( ids != null ) {
			for ( Long userId : ids ) {
				requireUserReadAccess(userId);
			}
		}
		final ObjectStreamCriteria f = policyEnforcerCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}
		final @Nullable Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Check access to location requests.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Before(value = "findLocationRequests(userId) || getLocationRequest(userId)", argNames = "userId")
	public void viewLocationRequestsCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	/**
	 * Check access to location requests.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Before(value = "submitLocationRequest(userId) || updateLocationRequest(userId) || removeLocationRequest(userId)",
			argNames = "userId")
	public void modifyLocationRequestsCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	/**
	 * Set the set of roles required to administer location metadata. If more
	 * than one role is provided, any one role must match for the authorization
	 * to succeed.
	 *
	 * @param locationMetadataAdminRoles
	 *        the set of roles
	 * @since 1.2
	 */
	public void setLocationMetadataAdminRoles(Set<String> locationMetadataAdminRoles) {
		if ( locationMetadataAdminRoles == null || locationMetadataAdminRoles.isEmpty() ) {
			throw new IllegalArgumentException(
					"The roleLocationMetadataAdmin argument must not be null or empty.");
		}
		Set<String> capitalized;
		if ( locationMetadataAdminRoles.size() == 1 ) {
			capitalized = Collections
					.singleton(locationMetadataAdminRoles.iterator().next().toUpperCase(Locale.ENGLISH));
		} else {
			capitalized = new HashSet<>(locationMetadataAdminRoles.size());
			for ( String role : locationMetadataAdminRoles ) {
				capitalized.add(role.toUpperCase(Locale.ENGLISH));
			}
		}
		this.locationMetadataAdminRoles = capitalized;
	}

}
