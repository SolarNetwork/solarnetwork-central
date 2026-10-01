/* ==================================================================
 * OcppHandshakeConfig.java - 1/10/2026 1:12:36 pm
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

package net.solarnetwork.central.in.ocpp.config;

import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OCPP WebSocket handshake processing.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OcppHandshakeConfig {

	/** A qualifier for OCPP WebSocket handshake components. */
	public static final String OCPP_HANDSHAKE = "ocpp-handshake";

	/** The default maximum number of concurrent handshakes. */
	public static final int DEFAULT_MAX_CONCURRENT = 2;

	/**
	 * The semaphore that limits concurrent OCPP WebSocket handshakes, shared by
	 * all OCPP handshake interceptors.
	 *
	 * @param maxConcurrent
	 *        the maximum number of concurrent handshakes
	 * @return the semaphore
	 */
	@Qualifier(OCPP_HANDSHAKE)
	@Bean
	public Semaphore ocppHandshakeSemaphore(
			@Value("${app.ocpp.handshake.max-concurrent:" + DEFAULT_MAX_CONCURRENT
					+ "}") int maxConcurrent) {
		return new Semaphore(Math.max(1, maxConcurrent), true);
	}

}
