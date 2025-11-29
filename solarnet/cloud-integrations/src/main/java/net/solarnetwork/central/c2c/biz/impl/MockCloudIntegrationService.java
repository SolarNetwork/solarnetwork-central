/* ==================================================================
 * MockCloudIntegrationService.java - 12/11/2025 8:41:37 am
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

package net.solarnetwork.central.c2c.biz.impl;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;

/**
 * Mock implementation of {@link CloudIntegrationService} for development and
 * testing.
 *
 * @author matt
 * @version 1.0
 */
public class MockCloudIntegrationService extends BaseCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.mock";

	/**
	 * Constructor.
	 *
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param controlServices
	 *        the control services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MockCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			Collection<CloudControlService> controlServices, UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor) {
		super(SERVICE_IDENTIFIER, "Mock Integration Service", datumStreamServices, controlServices,
				userEventAppenderBiz, encryptor, List.of(), Map.of());
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		return Result.success();
	}

}
