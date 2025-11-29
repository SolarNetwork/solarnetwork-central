/* ==================================================================
 * SolarQueueMqttConnectionConfig.java - 8/08/2022 5:28:27 am
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

package net.solarnetwork.central.jobs.config;

import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.support.ObservableMqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.util.StatTracker;

/**
 * SolarQueue MQTT configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt")
public class SolarQueueMqttConnectionConfig {

	/** Qualifier for SolarQueue MQTT. */
	public static final String SOLARQUEUE = "solarqueue";

	@Autowired
	private MqttConnectionFactory connectionFactory;

	/**
	 * Statistics for SolarQueue processing.
	 *
	 * @return the statistics
	 */
	@Qualifier(SOLARQUEUE)
	@Bean
	public StatTracker solarQueueMqttStats() {
		return new StatTracker("SolarQueue", null,
				LoggerFactory.getLogger("net.solarnetwork.central.mqtt.stats.SolarQueue"), 500);
	}

	/**
	 * MQTT connection to SolarQueue.
	 *
	 * @param stats
	 *        the statistics to use
	 * @param mqttConnectionObservers
	 *        the connection observers
	 * @return the connection
	 */
	@Qualifier(SOLARQUEUE)
	@ConfigurationProperties(prefix = "app.solarqueue.connection")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public ObservableMqttConnection solarQueueMqttConnection(@Qualifier(SOLARQUEUE) StatTracker stats,
			@Qualifier(SOLARQUEUE) List<MqttConnectionObserver> mqttConnectionObservers) {
		return new ObservableMqttConnection(connectionFactory, stats, "SolarQueue MQTT",
				mqttConnectionObservers);
	}

}
