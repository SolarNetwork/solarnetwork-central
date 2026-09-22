/* ==================================================================
 * UserExpireSecurityAspect.java - 9/07/2018 11:01:02 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.expire.aop;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security enforcing AOP aspect for {@link UserExpireBiz}
 *
 * @author matt
 * @version 2.2
 */
@Aspect
@Component
public class UserExpireSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserExpireSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.*Biz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz.delete*(..)) && args(config,..)")
	public void deleteConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz.*ForConfiguration(..)) && args(config,..)")
	public void actionForConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz.*(..)) && args(filter,..)")
	public void actionForDatumFilter(GeneralNodeDatumFilter filter) {

	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz.deleteDatum(..)) && args(userId,..)")
	public void deleteDatum(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz.deleteDatum(..)) && args(userId,ids,..)")
	public void deleteDatumIds(Long userId, Set<ObjectDatumId> ids) {
	}

	@Before(value = "actionForUser(userId)", argNames = "userId")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "deleteDatum(userId)", argNames = "userId")
	public void writeActionForUserCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	@Before(value = "saveConfiguration(config) || deleteConfiguration(config) || actionForConfiguration(config)",
			argNames = "config")
	public void saveConfigurationCheck(UserRelatedEntity<?> config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);
	}

	/**
	 * Check access to a datum filter, and restrict it to the active security
	 * policy.
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to check
	 * @return the result
	 * @throws Throwable
	 *         if any error occurs
	 * @since 2.2
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "actionForDatumFilter(filter)", argNames = "pjp,filter")
	public Object datumFilterAccessCheck(ProceedingJoinPoint pjp, GeneralNodeDatumFilter filter)
			throws Throwable {
		final GeneralNodeDatumFilter f = datumFilterCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}
		final @Nullable Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Check access to a datum filter.
	 *
	 * @param filter
	 *        the filter to check
	 * @return the filter to use, restricted to the nodes and sources of the
	 *         active security policy
	 */
	public GeneralNodeDatumFilter datumFilterCheck(GeneralNodeDatumFilter filter) {
		final Long userId = (filter != null ? filter.getUserId() : null);
		requireUserWriteAccess(userId);

		final Long[] nodeIds = (filter != null ? filter.getNodeIds() : null);
		if ( nodeIds != null ) {
			for ( Long nodeId : nodeIds ) {
				requireNodeWriteAccess(nodeId);
			}
		}
		return nonnull(policyEnforcerCheck(filter, true), "Restricted filter");
	}

	/**
	 * Restrict the datum IDs to delete to the active security policy.
	 *
	 * <p>
	 * IDs the policy does not allow are removed from the set to delete. If the
	 * policy allows none of them, access is denied.
	 * </p>
	 *
	 * @param pjp
	 *        the join point
	 * @param userId
	 *        the user ID
	 * @param ids
	 *        the datum IDs to delete
	 * @return the deleted datum IDs
	 * @throws Throwable
	 *         if any error occurs
	 * @since 2.2
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "deleteDatumIds(userId, ids)", argNames = "pjp,userId,ids")
	public Object deleteDatumIdsAccessCheck(ProceedingJoinPoint pjp, Long userId,
			Set<ObjectDatumId> ids) throws Throwable {
		final Set<ObjectDatumId> allowed = policyRestrictedDatumIds(ids);
		if ( allowed == ids ) {
			return pjp.proceed();
		}
		final @Nullable Object[] args = pjp.getArgs();
		args[1] = allowed;
		return pjp.proceed(args);
	}

	/**
	 * Restrict a set of datum IDs to those allowed by the active security
	 * policy.
	 *
	 * @param ids
	 *        the datum IDs to restrict
	 * @return the allowed datum IDs, or {@code ids} if the policy does not
	 *         restrict nodes or sources
	 * @throws AuthorizationException
	 *         if the policy allows none of the IDs
	 */
	private Set<ObjectDatumId> policyRestrictedDatumIds(Set<ObjectDatumId> ids) {
		final SecurityPolicy policy = getActiveSecurityPolicy();
		if ( ids == null || ids.isEmpty() || policy == null
				|| (isEmpty(policy.getNodeIds()) && isEmpty(policy.getSourceIds())) ) {
			return ids;
		}
		final Authentication authentication = SecurityUtils.getCurrentAuthentication();
		final Object principal = (authentication != null ? authentication.getPrincipal() : null);
		final var enforcer = new SecurityPolicyEnforcer(policy, principal, null, getPathMatcher(), null,
				null, getNodeOwnershipDao()::getDatumStreamMetadataIds);

		// verify the IDs as a whole: the enforcer denies access if none are allowed
		final Set<Long> okNodeIds = verified(enforcer::verifyNodeIds,
				ids.stream().filter(id -> ObjectDatumKind.Node == id.getKind())
						.map(ObjectDatumId::getObjectId).toArray(Long[]::new));
		final Set<String> okSourceIds = verified(enforcer::verifySourceIds,
				ids.stream().map(ObjectDatumId::getSourceId).toArray(String[]::new));
		final Set<UUID> okStreamIds = verified(enforcer::verifyStreamIds,
				ids.stream().filter(id -> id.getObjectId() == null).map(ObjectDatumId::getStreamId)
						.toArray(UUID[]::new));

		final Set<ObjectDatumId> result = new LinkedHashSet<>(ids.size());
		for ( ObjectDatumId id : ids ) {
			final String sourceId = id.getSourceId();
			if ( sourceId != null && !okSourceIds.contains(sourceId) ) {
				continue;
			}
			final Long objectId = id.getObjectId();
			if ( objectId != null ) {
				if ( ObjectDatumKind.Node == id.getKind() && !okNodeIds.contains(objectId) ) {
					continue;
				}
			} else if ( !okStreamIds.contains(id.getStreamId()) ) {
				continue;
			}
			result.add(id);
		}
		return result;
	}

	private static boolean isEmpty(@Nullable Set<?> set) {
		return (set == null || set.isEmpty());
	}

	/**
	 * API for verifying an array of IDs against a security policy.
	 *
	 * @param <T>
	 *        the ID type
	 */
	@FunctionalInterface
	private interface IdVerifier<T> {

		/**
		 * Verify IDs.
		 *
		 * @param ids
		 *        the IDs to verify
		 * @return the allowed IDs
		 */
		T @Nullable [] verify(T @Nullable [] ids);
	}

	private static <T> Set<T> verified(IdVerifier<T> verifier, T[] values) {
		final T[] nonNull = Arrays.stream(values).filter(Objects::nonNull)
				.toArray(len -> Arrays.copyOf(values, len));
		if ( nonNull.length < 1 ) {
			return Collections.emptySet();
		}
		final T[] result = verifier.verify(nonNull);
		return (result != null ? Set.of(result) : Collections.emptySet());
	}

}
