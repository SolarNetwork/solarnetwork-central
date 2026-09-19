/* ==================================================================
 * UserAuthTokenSecurityAspect.java - 14/12/2025 2:25:35 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.aop;

import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter;
import net.solarnetwork.central.user.dao.UserAuthTokenFilter;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.dao.FilterResults;

/**
 * Security enforcing AOP aspect for auth token methods of {@link UserBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserAuthTokenSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserAuthTokenSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match creating a new auth token.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.generateUserAuthToken(..))")
	public void generateUserAuthToken() {
	}

	/**
	 * Match listing all tokens.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getAllUserAuthTokens(..))")
	public void getAllUserAuthTokens() {
	}

	/**
	 * Match listing all tokens.
	 * 
	 * @param userId
	 *        the user ID
	 * @param filter
	 *        the search filter
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.listUserAuthTokensForUser(..)) && args(userId,filter)")
	public void findUserAuthTokensForUser(Long userId, UserAuthTokenFilter filter) {
	}

	/**
	 * Match token delete.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.deleteUserAuthToken(..))")
	public void deleteUserAuthToken() {
	}

	/**
	 * Match token update.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.updateUserAuthToken*(..))")
	public void updateUserAuthToken() {
	}

	/**
	 * Disallow creating auth tokens if the active user has a security policy.
	 */
	@Before("generateUserAuthToken() || deleteUserAuthToken() || updateUserAuthToken()")
	public void generateUserAuthTokenAccessCheck() {
		requireUnrestrictedSecurityPolicy();
	}

	/**
	 * Restrict access to token list if actor has restricted security policy.
	 * 
	 * @param pjp
	 *        the join point
	 * @return the tokens
	 * @throws Throwable
	 *         if any error occurs
	 */
	@Around("getAllUserAuthTokens()")
	public List<UserAuthToken> getAllUserAuthTokensAccessCheck(ProceedingJoinPoint pjp)
			throws Throwable {
		if ( !SecurityUtils.policyIsUnrestricted(getActiveSecurityPolicy()) ) {
			// not allowed to list all; instead return just current token
			if ( pjp.getTarget() instanceof UserBiz userBiz ) {
				BasicUserAuthTokenFilter filter = new BasicUserAuthTokenFilter();
				filter.setIdentifier(SecurityUtils.currentTokenId());
				FilterResults<UserAuthToken, String> results = userBiz
						.listUserAuthTokensForUser(SecurityUtils.getCurrentActorUserId(), filter);
				if ( results.getReturnedResultCount() > 0 ) {
					assert results.getReturnedResultCount() == 1 : "exactly one result expected";
					return List.of(results.iterator().next());
				}
			}
			return List.of();
		}
		@SuppressWarnings("unchecked")
		List<UserAuthToken> result = (List<UserAuthToken>) pjp.proceed();
		return result;
	}

	/**
	 * Restrict access to token list if actor has restricted security policy.
	 * 
	 * @param pjp
	 *        the join point
	 * @param userId
	 *        the user ID
	 * @param filter
	 *        the filter
	 * @return the tokens
	 * @throws Throwable
	 *         if any error occurs
	 */
	@Around(value = "findUserAuthTokensForUser(userId,filter)", argNames = "pjp,userId,filter")
	public FilterResults<UserAuthToken, String> findUserAuthTokensAccessCheck(ProceedingJoinPoint pjp,
			Long userId, UserAuthTokenFilter filter) throws Throwable {
		Object[] args = pjp.getArgs();
		if ( !SecurityUtils.policyIsUnrestricted(getActiveSecurityPolicy()) ) {
			// not allowed to list all; instead return just current token
			BasicUserAuthTokenFilter restrictedFilter = new BasicUserAuthTokenFilter(filter);
			restrictedFilter.setIdentifier(SecurityUtils.currentTokenId());
			args[1] = restrictedFilter;
		}
		@SuppressWarnings("unchecked")
		FilterResults<UserAuthToken, String> result = (FilterResults<UserAuthToken, String>) pjp
				.proceed(args);
		return result;
	}

}
