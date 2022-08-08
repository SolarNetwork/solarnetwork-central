/* ==================================================================
 * ObservableMqttConnection.java - 7/08/2022 7:49:47 pm
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Basic MQTT connection service with a configurable list of observers.
 * 
 * @author matt
 * @version 1.0
 */
public class ObservableMqttConnection extends BaseMqttConnectionService
		implements ServiceLifecycleObserver, MqttConnectionObserver {

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnet-ocpp";

	private final String name;
	private final List<MqttConnectionObserver> connectionObservers;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the MQTT connection factory
	 * @param mqttStats
	 *        the MQTT stats
	 * @param name
	 *        the display name
	 * @param connectionObservers
	 *        the connection observers
	 */
	public ObservableMqttConnection(MqttConnectionFactory connectionFactory, MqttStats mqttStats,
			String name, List<MqttConnectionObserver> connectionObservers) {
		super(connectionFactory, mqttStats);
		this.name = requireNonNullArgument(name, "name");
		this.connectionObservers = requireNonNullArgument(connectionObservers, "connectionObservers");
		getMqttConfig().setUsername(DEFAULT_MQTT_USERNAME);
		try {
			getMqttConfig().setServerUri(new URI(DEFAULT_MQTT_HOST));
		} catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getPingTestName() {
		return name;
	}

	@Override
	public String getPingTestId() {
		return getClass().getName() + "-" + name.replace(" ", "-");
	}

	@Override
	public void serviceDidStartup() {
		init();
	}

	@Override
	public void serviceDidShutdown() {
		shutdown();
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers ) {
				try {
					o.onMqttServerConnectionEstablished(connection, reconnected);
				} catch ( Throwable t ) {
					// naughty!
					Throwable root = t;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.error("Unhandled error in MQTT connection {} established observer {}: {}",
							getMqttConfig().getServerUri(), o, root.getMessage(), root);
				}
			}
		}
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers ) {
				try {
					o.onMqttServerConnectionLost(connection, willReconnect, cause);
				} catch ( Throwable t ) {
					// naughty!
					Throwable root = t;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.error("Unhandled error in MQTT connection {} lost observer {}: {}",
							getMqttConfig().getServerUri(), o, root.getMessage(), root);
				}
			}
		}
	}

	/**
	 * Get the connection observers.
	 * 
	 * @return the connection observers
	 */
	public List<MqttConnectionObserver> getConnectionObservers() {
		return connectionObservers;
	}

}
