/* ==================================================================
 * UserOscpSecurityAspect.java - 15/08/2022 10:38:13 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.oscp.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;

/**
 * Security enforcing AOP aspect for {@link UserOscpBiz}.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserOscpSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserOscpSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.*ForUser*(..)) && args(userId,..)")
	public void readForUser(Long userId) {
	}

	/**
	 * Match methods like {@code create*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.create*(..)) && args(userId,..)")
	public void createUserRelatedEntity(Long userId) {
	}

	/**
	 * Match methods like {@code update*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.update*(..)) && args(userId,..)")
	public void updateUserRelatedEntity(Long userId) {
	}

	/**
	 * Match methods like {@code create*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 * @param input
	 *        the asset configuration input
	 */
	@Pointcut(
			value = "execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.createAsset(..)) && args(userId,input)",
			argNames = "userId,input")
	public void createAssetConfiguration(Long userId, AssetConfigurationInput input) {
	}

	/**
	 * Match methods like {@code update*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 * @param assetId
	 *        the asset ID
	 * @param input
	 *        the asset configuration input
	 */
	@Pointcut(
			value = "execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.updateAsset(..)) && args(userId,assetId,input)",
			argNames = "userId,assetId,input")
	public void updateAssetConfiguration(Long userId, Long assetId, AssetConfigurationInput input) {
	}

	/**
	 * Match methods like {@code delete*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.oscp.biz.UserOscpBiz.delete*(..)) && args(userId,..)")
	public void deleteUserRelatedEntity(Long userId) {
	}

	@Before(value = "readForUser(userId)", argNames = "userId")
	public void userReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "createUserRelatedEntity(userId) || updateUserRelatedEntity(userId) || deleteUserRelatedEntity(userId)",
			argNames = "userId")
	public void userWriteAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	@Before(value = "createAssetConfiguration(userId,input)", argNames = "userId,input")
	public void createAssetNodeIdCheck(Long userId, AssetConfigurationInput input) {
		if ( input != null && input.getNodeId() != null ) {
			requireNodeReadAccess(input.getNodeId());
		}
	}

	@Before(value = "updateAssetConfiguration(userId,assetId,input)", argNames = "userId,assetId,input")
	public void updateAssetNodeIdCheck(Long userId, Long assetId, AssetConfigurationInput input) {
		if ( input != null && input.getNodeId() != null ) {
			requireNodeReadAccess(input.getNodeId());
		}
	}

}
