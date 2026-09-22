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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.domain.EntityConstants;
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
 * @version 1.1
 */
@Aspect
@Component
public class UserDatumStreamAliasSecurityAspect extends AuthorizationSupport {

	private final ObjectDatumStreamAliasEntityDao aliasDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param aliasDao
	 *        the alias DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.1
	 */
	public UserDatumStreamAliasSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			ObjectDatumStreamAliasEntityDao aliasDao) {
		super(nodeOwnershipDao);
		this.aliasDao = requireNonNullArgument(aliasDao, "aliasDao");
	}

	/**
	 * Match methods like {@code *ForUser(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.*ForUser(..))
			&& args(userId,..)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void readForUserId(Long userId) {
	}

	/**
	 * Match methods like {@code list*(userId, filter)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.list*(..))
			&& args(userId,filter)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void listForUserId(Long userId, ObjectDatumStreamAliasFilter filter) {
	}

	/**
	 * Match methods like {@code save*(userId, pk, input))}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.save*(..))
			&& args(userId,id,input)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void saveAliasForUserId(Long userId, UUID id, ObjectDatumStreamAliasEntityInput input) {
	}

	/**
	 * Match methods like {@code delete*(userId, filter))}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.delete*(..))
			&& args(userId,filter)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void deleteAliasForUserId(Long userId, ObjectDatumStreamAliasFilter filter) {
	}

	@Before(value = "readForUserId(userId)")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	/**
	 * Check access to save an alias.
	 *
	 * <p>
	 * Write access is required for the user, for the original and alias nodes
	 * of the input, and for the original node of any existing alias with the
	 * given ID, which is the node that determines the owner of the alias.
	 * </p>
	 *
	 * @param userId
	 *        the user ID
	 * @param id
	 *        the alias ID
	 * @param input
	 *        the alias input
	 */
	@Before(value = "saveAliasForUserId(userId, id, input)")
	public void saveAliasAccessCheck(Long userId, UUID id, ObjectDatumStreamAliasEntityInput input) {
		requireUserWriteAccess(userId);
		if ( EntityConstants.isAssigned(id) ) {
			final ObjectDatumStreamAliasEntity existing = aliasDao.get(id);
			if ( existing != null ) {
				requireNodeWriteAccess(existing.getOriginalObjectId());
			}
		}
		if ( input == null ) {
			return;
		}
		requireNodeWriteAccess(input.getOriginalObjectId());
		requireNodeWriteAccess(input.getObjectId());

		if ( input.getOriginalSourceId() == null && input.getSourceId() == null ) {
			return;
		}

		final SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null || ((policy.getNodeIds() == null || policy.getNodeIds().isEmpty())
				&& (policy.getSourceIds() == null || policy.getSourceIds().isEmpty())) ) {
			return;
		}

		final Authentication authentication = SecurityUtils.getCurrentAuthentication();
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

	@SuppressWarnings("ReferenceEquality")
	private Object enforceFilterPolicy(ProceedingJoinPoint pjp, Long userId,
			ObjectDatumStreamAliasFilter filter) throws Throwable {
		var filterToUse = policyEnforcerCheck(filter);
		if ( filterToUse == filter ) {
			return pjp.proceed();
		}
		return pjp.proceed(new Object[] { userId, filterToUse });
	}

	@Around(value = "listForUserId(userId,filter)", argNames = "pjp,userId,filter")
	public Object listAliasAccessCheck(ProceedingJoinPoint pjp, Long userId,
			ObjectDatumStreamAliasFilter filter) throws Throwable {
		requireUserReadAccess(userId);
		return enforceFilterPolicy(pjp, userId, filter);
	}

	@Around(value = "deleteAliasForUserId(userId,filter)", argNames = "pjp,userId,filter")
	public Object deleteAliasAccessCheck(ProceedingJoinPoint pjp, Long userId,
			ObjectDatumStreamAliasFilter filter) throws Throwable {
		requireUserWriteAccess(userId);
		return enforceFilterPolicy(pjp, userId, filter);
	}

}
