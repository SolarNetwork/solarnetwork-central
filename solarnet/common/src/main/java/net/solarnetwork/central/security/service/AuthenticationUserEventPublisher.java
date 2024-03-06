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

package net.solarnetwork.central.security.service;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.EventDetailsProvider;
import net.solarnetwork.codec.JsonUtils;

/**
 * Service for publishing user events from authentication events.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthenticationUserEventPublisher {

	private static final String[] AUTH_SUCCESS_TAGS = new String[] { "security", "auth", "success" };
	private static final String[] AUTH_LOGOUT_TAGS = new String[] { "security", "auth", "logout" };
	private static final String[] AUTH_FAILURE_TAGS = new String[] { "security", "auth", "failure" };

	private final UserEventAppenderBiz userEventAppenderBiz;
	private boolean failureOnly;

	private final String[] authSuccessTags;
	private final String[] authLogoutTags;
	private final String[] authFailureTags;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *        the top-level event context, such as the application name, to use
	 *        in the generated event tags
	 * @param userEventAppenderBiz
	 *        the appender
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AuthenticationUserEventPublisher(String context, UserEventAppenderBiz userEventAppenderBiz) {
		super();
		requireNonNullArgument(context, "context");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");

		this.authSuccessTags = setupTags(context, AUTH_SUCCESS_TAGS);
		this.authLogoutTags = setupTags(context, AUTH_LOGOUT_TAGS);
		this.authFailureTags = setupTags(context, AUTH_FAILURE_TAGS);
	}

	private static final String[] setupTags(String context, String[] src) {
		String[] dest = new String[src.length + 1];
		dest[0] = context;
		System.arraycopy(src, 0, dest, 1, src.length);
		return dest;
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
			if ( failureOnly ) {
				return;
			}
			tags = authSuccessTags;
		} else if ( event instanceof LogoutSuccessEvent ) {
			if ( failureOnly ) {
				return;
			}
			tags = authLogoutTags;
		} else if ( event instanceof AbstractAuthenticationFailureEvent ) {
			AbstractAuthenticationFailureEvent fe = (AbstractAuthenticationFailureEvent) event;
			tags = authFailureTags;
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
			data = new HashMap<>(4);
		}

		if ( auth instanceof EventDetailsProvider p ) {
			data.putAll(p.eventDetails());
		} else if ( auth.getDetails() instanceof EventDetailsProvider p ) {
			data.putAll(p.eventDetails());
		}

		data.put("prinicpal", auth.getPrincipal());
		if ( auth.getDetails() instanceof WebAuthenticationDetails w ) {
			data.put("remoteAddress", w.getRemoteAddress());
		}

		try {
			SecurityActor actor = SecurityUtils.getActor(auth);
			if ( actor instanceof SecurityUser ) {
				SecurityUser user = (SecurityUser) actor;
				data.put("email", user.getEmail());
			} else if ( actor instanceof SecurityToken ) {
				SecurityToken tok = (SecurityToken) actor;
				data.put("token", tok.getToken());
			}
		} catch ( SecurityException e ) {
			// ignore and continue
		}

		if ( auth.getAuthorities() != null && !auth.getAuthorities().isEmpty() ) {
			data.put("roles",
					auth.getAuthorities().stream().map(e -> e.getAuthority()).toArray(String[]::new));
		}

		LogEventInfo info = new LogEventInfo(tags, message, JsonUtils.getJSONString(data, null));
		userEventAppenderBiz.addEvent(userId, info);
	}

	/**
	 * Get the publish failure events only setting.
	 * 
	 * @return {@literal true} to publish failure events only
	 */
	public boolean isFailureOnly() {
		return failureOnly;
	}

	/**
	 * Set the publish failure events only setting
	 * 
	 * @param failureOnly
	 *        {@literal true} to publish failure events only
	 */
	public void setFailureOnly(boolean failureOnly) {
		this.failureOnly = failureOnly;
	}

}
