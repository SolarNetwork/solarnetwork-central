/* ==================================================================
 * SolarOcppWebSocketWarmUpTask.java - 4/07/2024 12:02:28 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import jakarta.websocket.server.ServerEndpointConfig;
import net.solarnetwork.central.biz.AppWarmUpTask;

/**
 * Component to "warm up" the WebSocket layer.
 * 
 * @author matt
 * @version 1.0
 */
@Component
@Profile(AppWarmUpTask.WARMUP)
public class SolarOcppWebSocketWarmUpTask implements AppWarmUpTask {

	private static final Logger log = LoggerFactory.getLogger(SolarOcppDaoWarmUpTask.class);

	/**
	 * Constructor.
	 */
	public SolarOcppWebSocketWarmUpTask() {
		super();
	}

	@Override
	public void warmUp() throws Exception {
		log.info("Performing WebSocket warm-up tasks...");

		log.debug("Getting container default configurator...");
		new ServerEndpointConfig.Configurator().getContainerDefaultConfigurator();

		log.info("WebSocket warm-up tasks complete.");
	}

}
