/* ==================================================================
 * UserDatumStreamAliasSecurityAspect.java - 1/04/2026 6:23:02 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.stream.aop;

import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security enforcing AOP aspect for {@link UserDatumStreamAliasBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserDatumStreamAliasSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserDatumStreamAliasSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.*ForUser(..)) && args(userId,..)")
	public void readForUserId(Long userId) {
	}

	/**
	 * Match methods like {@code list*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.list*(..)) && args(userId,..)")
	public void listForUserId(Long userId) {
	}

	/**
	 * Match methods like {@code list*(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.save*(..)) && args(userId,pk,input)")
	public void saveAliasForUserId(Long userId, UUID id, ObjectDatumStreamAliasEntityInput input) {
	}

	@Before(value = "readForUserId(userId) || listForUserId(userId)")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "saveAliasForUserId(userId, id, input)")
	public void saveAliasAccessCheck(Long userId, UUID id, ObjectDatumStreamAliasEntityInput input) {
		requireUserWriteAccess(userId);
		if ( input == null ) {
			return;
		}
		requireNodeReadAccess(input.getOriginalObjectId());
		requireNodeReadAccess(input.getObjectId());

		if ( input.getOriginalSourceId() == null && input.getSourceId() == null ) {
			return;
		}

		final Authentication authentication = SecurityUtils.getCurrentAuthentication();
		final SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null ) {
			return;
		}

		final Object principal = (authentication != null ? authentication.getPrincipal() : null);
		final var enforcer = new SecurityPolicyEnforcer(policy, principal, input, getPathMatcher(),
				SecurityPolicyMetadataType.Node, this::requireNodeReadAccess,
				(ids) -> getNodeOwnershipDao().getDatumStreamMetadataIds(ids));

		if ( input.getOriginalSourceId() != null ) {
			enforcer.verifySourceIds(new String[] { input.getOriginalSourceId() });
		} else {
			enforcer.verifySourceIds(new String[] { input.getSourceId() });
		}
	}

}
