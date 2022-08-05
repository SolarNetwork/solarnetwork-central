/* ==================================================================
 * AuthenticationUserEventPublisher.java - 5/08/2022 2:57:55 pm
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

package net.solarnetwork.central.reg.service;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.codec.JsonUtils;

/**
 * Service for publishing user events from authentication events.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class AuthenticationUserEventPublisher {

	private String[] AUTH_SUCCESS_TAGS = new String[] { "security", "auth", "success" };
	private String[] AUTH_LOGOUT_TAGS = new String[] { "security", "auth", "logout" };
	private String[] AUTH_FAILURE_TAGS = new String[] { "security", "auth", "failure" };

	private final UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 * 
	 * @param userEventAppenderBiz
	 *        the appender
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AuthenticationUserEventPublisher(UserEventAppenderBiz userEventAppenderBiz) {
		super();
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
	}

	/**
	 * Process authentication events.
	 * 
	 * @param event
	 *        the event to process
	 */
	@EventListener(AbstractAuthenticationEvent.class)
	public void processAuthenticationEvent(AbstractAuthenticationEvent event) {
		String[] tags;
		String message = null;
		Map<String, Object> data = null;
		if ( event instanceof AuthenticationSuccessEvent ) {
			tags = AUTH_SUCCESS_TAGS;
		} else if ( event instanceof LogoutSuccessEvent ) {
			tags = AUTH_LOGOUT_TAGS;
		} else if ( event instanceof AbstractAuthenticationFailureEvent ) {
			AbstractAuthenticationFailureEvent fe = (AbstractAuthenticationFailureEvent) event;
			tags = AUTH_FAILURE_TAGS;
			message = fe.getException().getClass().getSimpleName();
			data = new HashMap<>(2);
			data.put("error", fe.getException().getMessage());
		} else {
			return;
		}

		Long userId = null;
		Authentication auth = event.getAuthentication();
		try {
			userId = SecurityUtils.getActorUserId(auth);
		} catch ( SecurityException e ) {
			// ignore and return
			return;
		}

		if ( data == null ) {
			data = new HashMap<>(2);
		}

		SecurityActor actor = SecurityUtils.getActor(auth);
		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			data.put("email", user.getEmail());
		} else if ( actor instanceof SecurityToken ) {
			SecurityToken tok = (SecurityToken) actor;
			data.put("token", tok.getToken());
		}
		data.put("roles",
				auth.getAuthorities().stream().map(e -> e.getAuthority()).toArray(String[]::new));

		LogEventInfo info = new LogEventInfo(tags, message, JsonUtils.getJSONString(data, null));
		userEventAppenderBiz.addEvent(userId, info);
	}

}
