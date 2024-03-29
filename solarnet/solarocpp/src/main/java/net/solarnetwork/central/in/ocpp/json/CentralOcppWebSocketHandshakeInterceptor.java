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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.ocpp.domain.SystemUser;
import net.solarnetwork.ocpp.web.jakarta.json.OcppWebSocketHandshakeInterceptor;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Extension of {@link OcppWebSocketHandshakeInterceptor} for SolarNet.
 * 
 * @author matt
 * @version 1.2
 */
public class CentralOcppWebSocketHandshakeInterceptor extends OcppWebSocketHandshakeInterceptor
		implements CentralOcppUserEvents {

	private static final Logger log = LoggerFactory.getLogger(OcppWebSocketHandshakeInterceptor.class);

	/** User event kind for OCPP connection forbidden events. */
	public static final String[] CHARGE_POINT_AUTHENTICATION_FAILURE_TAGS = new String[] {
			CentralOcppWebSocketHandler.OCPP_EVENT_TAG, CentralOcppWebSocketHandler.CHARGER_EVENT_TAG,
			"forbidden" };

	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 * 
	 * @param systemUserDao
	 *        the system user DAO
	 * @param passwordEncoder
	 *        the password encoder
	 */
	public CentralOcppWebSocketHandshakeInterceptor(CentralSystemUserDao systemUserDao,
			PasswordEncoder passwordEncoder) {
		super(systemUserDao, passwordEncoder);
	}

	/**
	 * Get a credentials extractor function that extracts from the request path.
	 * 
	 * @param regex
	 *        the path regular expression: it must provide 2 groups, for the
	 *        username and password
	 * @return the function
	 */
	public static BiFunction<ServerHttpRequest, String, String[]> pathCredentialsExtractor(
			String regex) {
		Pattern p = Pattern.compile(regex);
		return (request, identifier) -> {
			String path = request.getURI().getPath();
			Matcher m = p.matcher(path);
			if ( m.matches() ) {
				return new String[] { m.group(1), m.group(2) };
			}
			log.warn("OCPP handshake request rejected for {}, path-based credentials not provided.",
					identifier);
			return null;
		};
	}

	@Override
	protected void didForbidChargerConnection(SystemUser user, String reason) {
		super.didForbidChargerConnection(user, reason);
		if ( user instanceof CentralSystemUser ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put("username", user.getUsername());
			data.put(ERROR_DATA_KEY, reason);
			generateUserEvent(((CentralSystemUser) user).getUserId(),
					CHARGE_POINT_AUTHENTICATION_FAILURE_TAGS, null, data);
		}
	}

	private void generateUserEvent(Long userId, String[] tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String ? (String) data : JsonUtils.getJSONString(data, null));
		LogEventInfo event = new LogEventInfo(tags, message, dataStr);
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
