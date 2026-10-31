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
import java.util.List;
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
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security enforcing AOP aspect for {@link UserDatumStreamAliasBiz}.
 *
 * @author matt
 * @version 1.2
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

	/**
	 * Match methods that read alias grants.
	 *
	 * @param userId
	 *        the user ID
	 * @since 1.2
	 */
	@Pointcut("""
			(execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.aliasGrantForUser(..))
				|| execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.listAliasGrants(..))
				|| execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.listReceivedAliasGrants(..)))
			&& args(userId,..)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void readAliasGrantForUserId(Long userId) {
	}

	/**
	 * Match methods that change existing alias grants.
	 *
	 * @param userId
	 *        the user ID
	 * @since 1.2
	 */
	@Pointcut("""
			(execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.updateAliasGrantEnabled(..))
				|| execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.deleteAliasGrant(..)))
			&& args(userId,..)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void changeAliasGrantForUserId(Long userId) {
	}

	/**
	 * Match the method that saves alias grants.
	 *
	 * @param userId
	 *        the user ID
	 * @param input
	 *        the grant input
	 * @since 1.2
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.saveAliasGrant(..))
			&& args(userId,*,input)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void saveAliasGrantForUserId(Long userId, ObjectDatumStreamAliasGrantEntityInput input) {
	}

	/**
	 * Match the method that lists streams available to alias.
	 *
	 * @param userId
	 *        the user ID
	 * @param aliasNodeId
	 *        the alias node ID
	 * @since 1.2
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.listAvailableAliasStreams(..))
			&& args(userId,aliasNodeId,*)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void listAvailableAliasStreamsForUserId(Long userId, Long aliasNodeId) {
	}

	/**
	 * Match the method that saves aliases in bulk.
	 *
	 * @param userId
	 *        the user ID
	 * @param inputs
	 *        the alias inputs
	 * @since 1.2
	 */
	@Pointcut("""
			execution(* net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz.saveAliases(..))
			&& args(userId,inputs)
			&& @target(net.solarnetwork.central.domain.Securable)
			""")
	public void saveAliasesForUserId(Long userId, List<ObjectDatumStreamAliasEntityInput> inputs) {
	}

	@Before(value = "readForUserId(userId)")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	/**
	 * Check access to save an alias.
	 *
	 * <p>
	 * Write access is required for the user, for the alias node of the input,
	 * and for the alias node of any existing alias with the given ID, which is
	 * the node that determines the owner of the alias. The original node must
	 * have the same owner as the alias node, and write access to it is
	 * required, or else the original stream must be available to the alias
	 * node, which is verified before the stream is touched so the outcome
	 * reveals nothing about streams that are not available.
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
				requireNodeWriteAccess(existing.getObjectId());
			}
		}
		if ( input == null ) {
			return;
		}
		requireNodeWriteAccess(input.getObjectId());
		requireOriginalNodeAccess(input);

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

	private void requireOriginalNodeAccess(ObjectDatumStreamAliasEntityInput input) {
		final Long originalNodeId = input.getOriginalObjectId();
		final Long aliasNodeId = input.getObjectId();
		final String originalSourceId = input.getOriginalSourceId();
		final SolarNodeOwnership original = (originalNodeId != null
				? getNodeOwnershipDao().ownershipForNodeId(originalNodeId)
				: null);
		final SolarNodeOwnership alias = (aliasNodeId != null
				? getNodeOwnershipDao().ownershipForNodeId(aliasNodeId)
				: null);
		if ( original == null || alias == null || original.getUserId().equals(alias.getUserId()) ) {
			requireNodeWriteAccess(originalNodeId);
			return;
		}

		// a policy limits an actor to its own account, so cannot be applied to another account
		requireUnrestrictedSecurityPolicy();
		if ( originalSourceId == null
				|| !aliasDao.aliasAllowed(original.getNodeId(), originalSourceId, alias.getNodeId()) ) {
			log.warn("Access DENIED to node {} source {} for alias node {}; not granted", originalNodeId,
					originalSourceId, aliasNodeId);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED,
					originalNodeId);
		}
	}

	/**
	 * Check access to save aliases in bulk.
	 *
	 * <p>
	 * Each input is held to the same checks as saving a new alias. The number
	 * of inputs is verified first, as every check can query the database.
	 * </p>
	 *
	 * @param userId
	 *        the user ID
	 * @param inputs
	 *        the alias inputs
	 * @see #saveAliasAccessCheck(Long, UUID, ObjectDatumStreamAliasEntityInput)
	 * @since 1.2
	 */
	@Before(value = "saveAliasesForUserId(userId, inputs)")
	public void saveAliasesAccessCheck(Long userId, List<ObjectDatumStreamAliasEntityInput> inputs) {
		requireUserWriteAccess(userId);
		if ( inputs == null ) {
			return;
		}
		if ( inputs.size() > UserDatumStreamAliasBiz.MAX_BULK_ALIASES ) {
			throw new IllegalArgumentException("No more than %d aliases can be saved at once."
					.formatted(UserDatumStreamAliasBiz.MAX_BULK_ALIASES));
		}
		for ( ObjectDatumStreamAliasEntityInput input : inputs ) {
			saveAliasAccessCheck(userId, EntityConstants.UNASSIGNED_UUID_ID, input);
		}
	}

	/**
	 * Check access to list the streams available to alias.
	 *
	 * <p>
	 * This has the same requirements as saving an alias of another account's
	 * stream on the given node.
	 * </p>
	 *
	 * @param userId
	 *        the user ID
	 * @param aliasNodeId
	 *        the alias node ID
	 * @since 1.2
	 */
	@Before(value = "listAvailableAliasStreamsForUserId(userId, aliasNodeId)")
	public void listAvailableAliasStreamsAccessCheck(Long userId, Long aliasNodeId) {
		readAliasGrantAccessCheck(userId);
		requireNodeWriteAccess(aliasNodeId);
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

	/**
	 * Check access to read alias grants.
	 *
	 * <p>
	 * A grant shares access to an entire account, so an actor restricted by a
	 * security policy cannot manage grants.
	 * </p>
	 *
	 * @param userId
	 *        the user ID
	 * @since 1.2
	 */
	@Before(value = "readAliasGrantForUserId(userId)")
	public void readAliasGrantAccessCheck(Long userId) {
		requireUserReadAccess(userId);
		requireUnrestrictedSecurityPolicy();
	}

	/**
	 * Check access to change existing alias grants.
	 *
	 * @param userId
	 *        the user ID
	 * @see #readAliasGrantAccessCheck(Long)
	 * @since 1.2
	 */
	@Before(value = "changeAliasGrantForUserId(userId)")
	public void changeAliasGrantAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
		requireUnrestrictedSecurityPolicy();
	}

	/**
	 * Check access to save an alias grant.
	 *
	 * <p>
	 * A grant only ever applies to nodes its owner owns at the time of a query,
	 * so verifying the nodes here only serves to report mistakes.
	 * </p>
	 *
	 * @param userId
	 *        the user ID
	 * @param input
	 *        the grant input
	 * @see #readAliasGrantAccessCheck(Long)
	 * @since 1.2
	 */
	@Before(value = "saveAliasGrantForUserId(userId, input)")
	public void saveAliasGrantAccessCheck(Long userId, ObjectDatumStreamAliasGrantEntityInput input) {
		changeAliasGrantAccessCheck(userId);
		if ( input == null || input.getNodeIds() == null ) {
			return;
		}
		for ( Long nodeId : input.getNodeIds() ) {
			requireNodeWriteAccess(nodeId);
		}
	}

}
