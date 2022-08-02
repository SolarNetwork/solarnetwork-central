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
import java.util.Map;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UuidGenerator;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.support.RandomUuidGenerator;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.ocpp.domain.SystemUser;
import net.solarnetwork.ocpp.web.json.OcppWebSocketHandshakeInterceptor;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Extension of {@link OcppWebSocketHandshakeInterceptor} for SolarNet.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralOcppWebSocketHandshakeInterceptor extends OcppWebSocketHandshakeInterceptor {

	/** A user event kind for OCPP connection forbidden events. */
	public static final String CHARGE_POINT_AUTHENTICATION_FAILURE_KIND = "OCPP/Charger/Forbidden";

	private UserEventAppenderBiz userEventAppenderBiz;
	private UuidGenerator uuidGenerator = RandomUuidGenerator.INSTANCE;

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

	@Override
	protected void didForbidChargerConnection(SystemUser user, String reason) {
		super.didForbidChargerConnection(user, reason);
		if ( user instanceof CentralSystemUser ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put("username", user.getUsername());
			data.put("error", reason);
			generateUserEvent(((CentralSystemUser) user).getUserId(),
					CHARGE_POINT_AUTHENTICATION_FAILURE_KIND, "Charge point forbidden", data);
		}
	}

	private void generateUserEvent(Long userId, String kind, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String ? (String) data : JsonUtils.getJSONString(data, null));
		UserEvent event = new UserEvent(userId, Instant.now(), uuidGenerator.generate(), kind, message,
				dataStr);
		biz.add(event);
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

	/**
	 * Get the UUID generator.
	 * 
	 * @return the generator, never {@literal null}
	 */
	public UuidGenerator getUuidGenerator() {
		return uuidGenerator;
	}

	/**
	 * Set the UUID generator.
	 * 
	 * @param uuidGenerator
	 *        the generator to set; if {@literal null} then
	 *        {@link RandomUuidGenerator} will be used
	 */
	public void setUuidGenerator(UuidGenerator uuidGenerator) {
		this.uuidGenerator = (uuidGenerator != null ? uuidGenerator : RandomUuidGenerator.INSTANCE);
	}

}
