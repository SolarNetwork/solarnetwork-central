/* ==================================================================
 * JsonConfig.java - 9/6/2023 12:13:20 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.mqttconntest.config;

import static net.solarnetwork.central.mqttconntest.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.mqttconntest.impl.MqttConnectionDebugger;

/**
 * Configuration for the MQTT Connection Debugger.
 * 
 * @author matt
 * @version 1.0
 */
@Profile("mqtt & !no-solarflux")
@Configuration(proxyBeanMethods = false)
public class SolarFluxMqttConnectionDebuggerConfig {

	@Value("${app.solarflux.debugger.topic:instr/OCPP_v16}")
	private String mqttDebuggerTopic = "instr/OCPP_v16";

	@Bean
	@Qualifier(SOLARFLUX)
	public MqttConnectionDebugger solarFluxMqttConnectionDebugger(Executor executor) {
		MqttConnectionDebugger debugger = new MqttConnectionDebugger(mqttDebuggerTopic, executor);
		return debugger;
	}

}
