/* ==================================================================
 * CentralOcppWebSocketHandshakeInterceptor.java - 2/08/2022 7:02:49 pm
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

package net.solarnetwork.central.in.ocpp.json;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.ocpp.domain.ChargePointAuthorizationDetails;
import net.solarnetwork.ocpp.domain.SystemUser;
import net.solarnetwork.ocpp.web.jakarta.json.OcppWebSocketHandshakeInterceptor;
import net.solarnetwork.service.PasswordEncoder;
import net.solarnetwork.util.ObjectUtils;

/**
 * Extension of {@link OcppWebSocketHandshakeInterceptor} for SolarNet.
 *
 * @author matt
 * @version 1.6
 */
public class CentralOcppWebSocketHandshakeInterceptor extends OcppWebSocketHandshakeInterceptor
		implements CentralOcppUserEvents {

	private static final Logger log = LoggerFactory
			.getLogger(CentralOcppWebSocketHandshakeInterceptor.class);

	/** User event kind for OCPP connection forbidden events. */
	public static final List<String> CHARGE_POINT_AUTHENTICATION_FAILURE_TAGS = List.of(
			CentralOcppWebSocketHandler.OCPP_EVENT_TAG, CentralOcppWebSocketHandler.CHARGER_EVENT_TAG,
			"forbidden");

	private final UserSettingsDao userSettingsDao;
	private final Pattern pathCredentialsRegex;
	private final Pattern pathHidRegex;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 *
	 * @param systemUserDao
	 *        the system user DAO
	 * @param passwordEncoder
	 *        the password encoder
	 * @param userSettingsDao
	 *        the user settings DAO
	 */
	public CentralOcppWebSocketHandshakeInterceptor(CentralSystemUserDao systemUserDao,
			PasswordEncoder passwordEncoder, UserSettingsDao userSettingsDao) {
		this(systemUserDao, passwordEncoder, userSettingsDao, null);
	}

	/**
	 * Constructor.
	 *
	 * @param systemUserDao
	 *        the system user DAO
	 * @param passwordEncoder
	 *        the password encoder
	 * @param userSettingsDao
	 *        the user settings DAO
	 * @param pathCredentialsRegex
	 *        an optional regular expression to extract path credentials from
	 *        request URLs; the expression must return two groups: the username
	 *        and the password
	 * @since 1.3
	 */
	public CentralOcppWebSocketHandshakeInterceptor(CentralSystemUserDao systemUserDao,
			PasswordEncoder passwordEncoder, UserSettingsDao userSettingsDao,
			Pattern pathCredentialsRegex) {
		this(systemUserDao, passwordEncoder, userSettingsDao, pathCredentialsRegex, null);
	}

	/**
	 * Constructor.
	 *
	 * @param systemUserDao
	 *        the system user DAO
	 * @param passwordEncoder
	 *        the password encoder
	 * @param userSettingsDao
	 *        the user settings DAO
	 * @param pathCredentialsRegex
	 *        an optional regular expression to extract path credentials from
	 *        request URLs; the expression must return two groups: the username
	 *        and the password
	 * @param pathHidRegex
	 *        an optional regular expression to extract an OCPP user settings
	 *        {@code hid} value from request URLs; the epxression must return
	 *        one group: the hid value
	 * @since 1.4
	 */
	public CentralOcppWebSocketHandshakeInterceptor(CentralSystemUserDao systemUserDao,
			PasswordEncoder passwordEncoder, UserSettingsDao userSettingsDao,
			Pattern pathCredentialsRegex, Pattern pathHidRegex) {
		super(systemUserDao, passwordEncoder);
		this.userSettingsDao = ObjectUtils.requireNonNullArgument(userSettingsDao, "userSettingsDao");
		this.pathCredentialsRegex = pathCredentialsRegex;
		if ( pathCredentialsRegex != null ) {
			setClientCredentialsExtractor(this::extractPathCredentials);
		}
		this.pathHidRegex = pathHidRegex;
	}

	private ChargePointAuthorizationDetails extractPathCredentials(final ServerHttpRequest request,
			final String identifier) {
		String path = request.getURI().getPath();
		Matcher m = pathCredentialsRegex.matcher(path);
		if ( m.matches() ) {
			return new SystemUser(Instant.now(), m.group(1), m.group(2));
		}
		log.warn("OCPP handshake request rejected for {}, path-based credentials not provided.",
				identifier);
		didForbidChargerConnection(request, identifier, null,
				String.format("Path-based credentials not provided in URL [%s]", path));
		return null;
	}

	@Override
	protected void didForbidChargerConnection(ServerHttpRequest request, String identifier,
			ChargePointAuthorizationDetails user, String reason) {
		super.didForbidChargerConnection(request, identifier, user, reason);

		Long userId = null;

		if ( user instanceof UserIdRelated u ) {
			userId = u.getUserId();
		} else if ( pathHidRegex != null ) {
			Matcher m = pathHidRegex.matcher(request.getURI().getPath());
			if ( m.matches() ) {
				String hid = m.group(1);
				if ( !hid.isEmpty() ) {
					UserSettings settings = userSettingsDao.getForHid(hid);
					if ( settings != null ) {
						userId = settings.getUserId();
					}
				}
			}
		}

		if ( userId != null ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			if ( user != null && user.getUsername() != null ) {
				data.put("username", user.getUsername());
			}
			if ( identifier != null ) {
				data.put(CHARGE_POINT_DATA_KEY, identifier);
			}
			data.put(ERROR_DATA_KEY, reason);
			generateUserEvent(userId, CHARGE_POINT_AUTHENTICATION_FAILURE_TAGS, null, data);
		}
	}

	private void generateUserEvent(Long userId, List<String> tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String s ? s : JsonUtils.getJSONString(data, null));
		LogEventInfo event = LogEventInfo.event(tags, message, dataStr);
		biz.addEvent(userId, event);
	}

	/**
	 * Get the user event appender service.
	 *
	 * @return the service
	 */
	public UserEventAppenderBiz getUserEventAppenderBiz() {
		return userEventAppenderBiz;
	}

	/**
	 * Set the user event appender service.
	 *
	 * @param userEventAppenderBiz
	 *        the service to set
	 */
	public void setUserEventAppenderBiz(UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

}
