/* ==================================================================
 * UserCloudIntegrationsSecurityAspect.java - 30/09/2024 11:14:37 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.c2c.aop;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamIdRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingIdRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRelated;
import net.solarnetwork.central.common.dao.NodeCriteria;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.NodeIdRelated;
import net.solarnetwork.central.domain.ObjectDatumIdRelated;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security enforcing AOP aspect for {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.1
 */
@Aspect
@Component
@Profile(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsSecurityAspect extends AuthorizationSupport {

	private final CloudDatumStreamConfigurationDao datumStreamDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 */
	public UserCloudIntegrationsSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			CloudDatumStreamConfigurationDao datumStreamDao) {
		super(nodeOwnershipDao);
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
	}

	/**
	 * Match read methods given a user-related identifier.
	 *
	 * @param userKey
	 *        the user related identifier
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.*ForId(..)) && args(userKey,..)")
	public void readForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match read methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.*ForUser(..)) && args(userId,..)")
	public void readForUserId(Long userId) {
	}

	/**
	 * Match list methods given a user-related identifier.
	 *
	 * @param userKey
	 *        the user related identifier
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.list*(..)) && args(userKey,..)")
	public void listForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match list methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.list*(..)) && args(userId,..)")
	public void listForUserId(Long userId) {
	}

	/**
	 * Match read methods given a user-related identifier.
	 *
	 * @param userId
	 *        the user ID
	 * @param filter
	 *        the search filter
	 * @param entityClass
	 *        the entity class
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.list*(..)) && args(userId,filter,entityClass)")
	public void listForUserIdAndFilterAndClass(Long userId, CloudIntegrationsFilter filter,
			Class<?> entityClass) {
	}

	/**
	 * Match replace methods given a configuration.
	 *
	 * @param userKey
	 *        the user key
	 * @param inputs
	 *        the list of entity inputs to save
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.replace*(..)) && args(userKey,inputs)")
	public void replaceEntityForUserKey(UserIdRelated userKey, List<?> inputs) {
	}

	/**
	 * Match update methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.update*(..)) && args(userKey,..)")
	public void updateEntityForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match save methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.save*(..)) && args(userKey,entity,..)")
	public void saveEntityForUserKey(UserIdRelated userKey, Object entity) {
	}

	/**
	 * Match save methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.save*(..)) && args(userId,..)")
	public void saveEntityForUserId(Long userId) {
	}

	/**
	 * Match delete methods given an entity key.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.delete*(..)) && args(userKey)")
	public void deleteEntityForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match read methods given a user-related identifier.
	 *
	 * @param userKey
	 *        the user related identifier
	 * @param entityClass
	 *        the entity class
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.*ForId(..)) && args(userKey,entityClass)")
	public void readEntityForUserKeyAndClass(UserIdRelated userKey, Class<?> entityClass) {
	}

	/**
	 * Match delete methods given an entity key and entity class.
	 *
	 * @param userKey
	 *        the user key
	 * @param entityClass
	 *        the entity class
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.delete*(..)) && args(userKey,entityClass)")
	public void deleteEntityForUserKeyAndClass(UserIdRelated userKey, Class<?> entityClass) {
	}

	/**
	 * Match delete methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.delete*(..)) && args(userId,..)")
	public void deleteEntityForUserId(Long userId) {
	}

	/**
	 * Require read access to a node ID configured on a datum stream.
	 *
	 * @param datumStreamId
	 *        the datum stream to verify
	 * @throws AuthorizationException
	 *         if access is denied
	 */
	public void requireDatumStreamReadAccess(UserLongCompositePK datumStreamId) {
		requireDatumStreamAccess(datumStreamId, this::requireNodeReadAccess);
	}

	/**
	 * Require write access to a node ID configured on a datum stream.
	 *
	 * @param datumStreamId
	 *        the datum stream to verify
	 * @throws AuthorizationException
	 *         if access is denied
	 */
	public void requireDatumStreamWriteAccess(UserLongCompositePK datumStreamId) {
		requireDatumStreamAccess(datumStreamId, this::requireNodeWriteAccess);
	}

	private void requireDatumStreamAccess(final UserLongCompositePK datumStreamId,
			final Consumer<Long> nodeIdValidator) {
		if ( datumStreamId == null ) {
			return;
		}
		final CloudDatumStreamConfiguration conf = datumStreamDao.get(datumStreamId);
		if ( conf != null && conf.hasNodeId() ) {
			nodeIdValidator.accept(conf.nodeId());
		}
	}

	/**
	 * Require read access to all node IDs configured on all datum streams
	 * referencing a datum stream mapping.
	 *
	 * @param datumStreamMappingId
	 *        the datum stream mapping to verify
	 * @throws AuthorizationException
	 *         if access is denied
	 */
	public void requireDatumStreamMappingReadAccess(UserLongCompositePK datumStreamMappingId) {
		requireDatumStreamMappingAccess(datumStreamMappingId, this::requireNodeReadAccess);
	}

	/**
	 * Require write access to all node IDs configured on all datum streams
	 * referencing a datum stream mapping.
	 *
	 * @param datumStreamMappingId
	 *        the datum stream mapping to verify
	 * @throws AuthorizationException
	 *         if access is denied
	 */
	public void requireDatumStreamMappingWriteAccess(UserLongCompositePK datumStreamMappingId) {
		requireDatumStreamMappingAccess(datumStreamMappingId, this::requireNodeWriteAccess);
	}

	private void requireDatumStreamMappingAccess(final UserLongCompositePK datumStreamMappingId,
			final Consumer<Long> nodeIdValidator) {
		if ( datumStreamMappingId == null ) {
			return;
		}
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(datumStreamMappingId.getUserId());
		filter.setDatumStreamMappingId(datumStreamMappingId.getEntityId());

		final FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = datumStreamDao
				.findFiltered(filter);
		for ( CloudDatumStreamConfiguration conf : results ) {
			if ( conf != null && conf.hasNodeId() ) {
				nodeIdValidator.accept(conf.nodeId());
			}
		}
	}

	@Before(value = "readForUserKey(userKey)", argNames = "userKey")
	public void readForUserKeyAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "readEntityForUserKeyAndClass(userKey,entityClass)",
			argNames = "userKey,entityClass")
	public void readEntityForUserKeyAndClassAccessCheck(UserIdRelated userKey, Class<?> entityClass) {
		// requireUserReadAccess already handled by userKeyReadAccessCheck() above
		if ( entityClass == null ) {
			return;
		}
		if ( CloudDatumStreamIdRelated.class.isAssignableFrom(entityClass)
				&& userKey instanceof UserLongCompositePK datumStreamId ) {
			requireDatumStreamReadAccess(datumStreamId);
		} else if ( CloudDatumStreamMappingIdRelated.class.isAssignableFrom(entityClass)
				&& userKey instanceof UserLongCompositePK mappingId ) {
			requireDatumStreamMappingReadAccess(mappingId);
		}
	}

	@Before(value = "readForUserId(userId)", argNames = "userId")
	public void readForUserIdAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "listForUserId(userId)", argNames = "userId")
	public void listForUserIdAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "listForUserKey(userKey)", argNames = "userKey")
	public void listForUserKeyAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Around(value = "listForUserIdAndFilterAndClass(userId,filter,entityClass)",
			argNames = "pjp,userId,filter,entityClass")
	public Object listForUserIdAndFilterAndClassAccessCheck(ProceedingJoinPoint pjp, Long userId,
			CloudIntegrationsFilter filter, Class<?> entityClass) throws Throwable {
		final SecurityPolicy policy = getActiveSecurityPolicy();
		final Object[] args = pjp.getArgs();
		if ( !SecurityUtils.policyIsUnrestricted(policy) ) {
			// enforce policy on filter
			args[1] = enforceSecurityPolicyOnFilter(policy, filter, entityClass);
		}
		return pjp.proceed(args);
	}

	private CloudIntegrationsFilter enforceSecurityPolicyOnFilter(final SecurityPolicy policy,
			final CloudIntegrationsFilter filter, final Class<?> entityClass) {
		if ( policy == null || entityClass == null || policy.getNodeIds() == null
				|| policy.getNodeIds().isEmpty() ) {
			// no node IDs to enforce
			return filter;
		}

		final Set<Long> policyNodeIds = policy.getNodeIds();
		final Set<Long> filterNodeIds = Set.of(
				filter instanceof NodeCriteria c && c.hasNodeCriteria() ? c.getNodeIds() : new Long[0]);

		// create this if we need to modify the filter by populating node IDs
		BasicFilter replacementFilter = null;

		if ( filterNodeIds.isEmpty() ) {
			replacementFilter = new BasicFilter(filter);
			replacementFilter.setNodeIds(policyNodeIds.toArray(Long[]::new));
		} else {
			Set<Long> restrictedNodeIds = new LinkedHashSet<>(filterNodeIds);
			for ( Iterator<Long> itr = restrictedNodeIds.iterator(); itr.hasNext(); ) {
				if ( !policyNodeIds.contains(itr.next()) ) {
					itr.remove();
				}
			}
			if ( restrictedNodeIds.size() < filterNodeIds.size() ) {
				replacementFilter = new BasicFilter(filter);
				replacementFilter.setNodeIds(restrictedNodeIds.toArray(Long[]::new));
			}
		}

		return (replacementFilter != null ? replacementFilter : filter);
	}

	@Before(value = "replaceEntityForUserKey(userKey,inputs)", argNames = "userKey,inputs")
	public void replaceEntityAccessCheck(UserIdRelated userKey, List<?> inputs) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
		if ( inputs == null || inputs.isEmpty() ) {
			return;
		}
		Object input = inputs.getFirst();
		if ( input == null ) {
			return;
		}
		if ( input instanceof CloudDatumStreamRelated
				&& userKey instanceof UserLongCompositePK datumStreamId ) {
			requireDatumStreamWriteAccess(datumStreamId);
		} else if ( input instanceof CloudDatumStreamMappingRelated
				&& userKey instanceof UserLongCompositePK datumStreamMappingId ) {
			requireDatumStreamMappingWriteAccess(datumStreamMappingId);
		}
	}

	@Before(value = "saveEntityForUserKey(userKey, entity)", argNames = "userKey,entity")
	public void saveEntityAccessCheck(UserIdRelated userKey, Object entity) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
		if ( entity instanceof NodeIdRelated id && id.getNodeId() != null ) {
			requireNodeWriteAccess(id.getNodeId());
		} else if ( entity instanceof ObjectDatumIdRelated id && id.hasNodeId() ) {
			requireNodeWriteAccess(id.nodeId());
		} else if ( entity instanceof CloudDatumStreamIdRelated id && id.hasDatumStreamId() ) {
			requireDatumStreamWriteAccess(
					new UserLongCompositePK(userKey.getUserId(), id.getDatumStreamId()));
		} else if ( entity instanceof CloudDatumStreamRelated
				&& userKey instanceof UserLongCompositePK datumStreamId ) {
			requireDatumStreamWriteAccess(datumStreamId);
		} else if ( entity instanceof CloudDatumStreamMappingRelated
				&& userKey instanceof UserLongCompositePK mappingId ) {
			requireDatumStreamMappingWriteAccess(mappingId);
		}
	}

	@Before(value = "saveEntityForUserId(userId)", argNames = "userId")
	public void saveEntityForUserAccessCheck(Long userId) {
		requireUserWriteAccess(userId);

		// also these are global settings so require an unrestricted token
		requireUnrestrictedSecurityPolicy();
	}

	@Before(value = "updateEntityForUserKey(userKey)", argNames = "userKey")
	public void updateEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "deleteEntityForUserKey(userKey)", argNames = "userKey")
	public void deleteEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "deleteEntityForUserKeyAndClass(userKey,entityClass)",
			argNames = "userKey,entityClass")
	public void deleteEntityForUserKeyAndClassAccessCheck(UserIdRelated userKey, Class<?> entityClass) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
		if ( entityClass == null ) {
			return;
		}
		if ( CloudDatumStreamIdRelated.class.isAssignableFrom(entityClass)
				&& userKey instanceof UserLongCompositePK datumStreamId ) {
			requireDatumStreamWriteAccess(datumStreamId);
		} else if ( CloudDatumStreamMappingIdRelated.class.isAssignableFrom(entityClass)
				&& userKey instanceof UserLongCompositePK mappingId ) {
			requireDatumStreamMappingWriteAccess(mappingId);
		}
	}

	@Before(value = "deleteEntityForUserId(userId)", argNames = "userId")
	public void deleteFoUserIdAccessCheck(Long userId) {
		requireUserWriteAccess(userId);

		// also these are global settings so require an unrestricted token
		requireUnrestrictedSecurityPolicy();
	}

}
