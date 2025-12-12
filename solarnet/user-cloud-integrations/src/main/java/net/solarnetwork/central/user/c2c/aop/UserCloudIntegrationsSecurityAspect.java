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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security enforcing AOP aspect for {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.1
 */
@Aspect
@Component
public class UserCloudIntegrationsSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserCloudIntegrationsSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
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
	 * Match delete methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz.delete*(..)) && args(userKey,..)")
	public void deleteEntityForUserKey(UserIdRelated userKey) {
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
		if ( entity instanceof CloudDatumStreamConfigurationInput ds
				&& ObjectDatumKind.Node.equals(ds.getKind()) && ds.getObjectId() != null ) {
			requireNodeWriteAccess(ds.getObjectId());
		}
	}

	@Before(value = "saveEntityForUserId(userId)", argNames = "userId")
	public void saveEntityForUserAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	@Before(value = "updateEntityForUserKey(userKey)", argNames = "userKey")
	public void updateEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "deleteEntityForUserKey(userKey)", argNames = "userKey")
	public void deleteEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "deleteEntityForUserId(userId)", argNames = "userId")
	public void userIdDeleteAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

}
