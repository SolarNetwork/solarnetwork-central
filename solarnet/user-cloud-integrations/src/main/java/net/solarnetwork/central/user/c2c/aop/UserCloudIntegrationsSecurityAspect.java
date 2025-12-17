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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamIdRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRelated;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.NodeIdRelated;
import net.solarnetwork.central.domain.ObjectDatumIdRelated;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;

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
	 * Match replace methods given a configuration.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.replace*(..)) && args(userKey,..)")
	public void replaceEntityForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match replace datum stream rake tasks configuration.
	 *
	 * @param datumStreamId
	 *        the datum stream ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.replaceDatumStreamRakeTasks(..)) && args(datumStreamId,..)")
	public void replaceDatumStreamRakeTasksForDatumStreamId(UserLongCompositePK datumStreamId) {
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
	 * Match delete methods given an entity key and entity class.
	 *
	 * @param userKey
	 *        the user key
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

	private void requireDatumStreamWriteAccess(UserLongCompositePK datumStreamId) {
		CloudDatumStreamConfiguration conf = datumStreamDao.get(datumStreamId);
		if ( conf != null && conf.hasNodeId() ) {
			requireNodeWriteAccess(conf.nodeId());
		}
	}

	@Before(value = "readForUserKey(userKey)", argNames = "userKey")
	public void userKeyReadAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "readForUserId(userId)", argNames = "userId")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "listForUserId(userId)", argNames = "userId")
	public void userIdListAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "listForUserKey(userKey)", argNames = "userKey")
	public void userKeyListAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "replaceEntityForUserKey(userKey)", argNames = "userKey")
	public void replaceEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
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
				&& userKey instanceof UserLongCompositePK pk ) {
			requireDatumStreamWriteAccess(pk);
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
		if ( entityClass != null && CloudDatumStreamIdRelated.class.isAssignableFrom(entityClass)
				&& userKey instanceof UserLongCompositePK pk ) {
			requireDatumStreamWriteAccess(pk);
		}
	}

	@Before(value = "deleteEntityForUserId(userId)", argNames = "userId")
	public void userIdDeleteAccessCheck(Long userId) {
		requireUserWriteAccess(userId);

		// also these are global settings so require an unrestricted token
		requireUnrestrictedSecurityPolicy();
	}

	@Before("replaceDatumStreamRakeTasksForDatumStreamId(datumStreamId)")
	public void datumStreamIdAccessCheck(UserLongCompositePK datumStreamId) {
		requireDatumStreamWriteAccess(datumStreamId);
	}

}
