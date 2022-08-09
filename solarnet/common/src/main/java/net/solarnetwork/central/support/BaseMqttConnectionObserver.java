/* ==================================================================
 * BaseMqttConnectionObserver.java - 8/08/2022 9:12:14 am
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
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * Base implementation of {@link MqttConnectionObserver} to help with connection
 * observer-based MQTT services.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseMqttConnectionObserver extends BasicIdentifiable
		implements MqttConnectionObserver {

	/** The {@code connectTimeoutSeconds} property default value. */
	public static final int DEFAULT_PUBLISH_TIMEOUT_SECONDS = 10;

	/** The {@code subscribeTimeoutSeconds} property default value. */
	public static final int DEFAULT_SUBSCRIBE_TIMEOUT_SECONDS = 10;

	/** The {@code transientErrorTries} property default value. */
	public static final int DEFAULT_TRANSIENT_ERROR_TRIES = 3;

	/** The {@code publishQos} property default value. */
	public static final MqttQos DEFAULT_PUBLISH_QOS = MqttQos.AtMostOnce;

	/** The {@code subscribeQos} property default value. */
	public static final MqttQos DEFAULT_SUBSCRIBE_QOS = MqttQos.AtLeastOnce;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final AtomicReference<MqttConnection> mqttConnection = new AtomicReference<>();

	private boolean retained;
	private MqttQos publishQos = DEFAULT_PUBLISH_QOS;
	private MqttQos subscribeQos = DEFAULT_SUBSCRIBE_QOS;
	private int publishTimeoutSeconds = DEFAULT_PUBLISH_TIMEOUT_SECONDS;
	private int subscribeTimeoutSeconds = DEFAULT_SUBSCRIBE_TIMEOUT_SECONDS;
	private int transientErrorTries = DEFAULT_TRANSIENT_ERROR_TRIES;
	private MqttStats mqttStats;

	/**
	 * Callback when the MQTT connection has been established.
	 * 
	 * <p>
	 * Extending classes that override this can call this implementation to
	 * store the connection in the {@code mqttConnection} reference.
	 * </p>
	 * 
	 * {@inheritDoc}h
	 */
	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		log.info("MQTT connection established for {}", this);
		mqttConnection.set(connection);
	}

	/**
	 * Callback when the MQTT connection has been lost.
	 * 
	 * <p>
	 * Extending classes that override this can call this implementation to
	 * clear the connection from the {@code mqttConnection} reference.
	 * </p>
	 * 
	 * {@inheritDoc}h
	 */
	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		log.info("MQTT connection lost for {}", this);
		mqttConnection.compareAndSet(connection, null);
	}

	/**
	 * Test if the MQTT connection is established.
	 * 
	 * @return {@literal true} if the MQTT connection is established
	 */
	public boolean isConnected() {
		MqttConnection conn = mqttConnection.get();
		return (conn != null && conn.isEstablished());
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(getClass().getSimpleName());
		buf.append('{');

		String s = getDisplayName();
		if ( s != null && !s.isEmpty() ) {
			buf.append(s);
		} else {
			s = getUid();
			if ( s != null && !s.isEmpty() ) {
				buf.append(s);
			} else {
				buf.append(Integer.toHexString(System.identityHashCode(this)));
			}
		}

		buf.append('}');
		return buf.toString();
	}

	/**
	 * Get the number of times to try operations that support retry, when
	 * transient exceptions occur.
	 * 
	 * @return the number of attempts to try operations that support retry;
	 *         defaults to {@link #DEFAULT_TRANSIENT_ERROR_TRIES}
	 */
	public int getTransientErrorTries() {
		return transientErrorTries;
	}

	/**
	 * Set the number of times to try operations that support retry, when
	 * transient exceptions occur.
	 * 
	 * @param transientErrorTries
	 *        the number of times to attempt operations that support retry; must
	 *        be greater than {@literal 0}
	 */
	public void setTransientErrorTries(int transientErrorTries) {
		if ( transientErrorTries < 1 ) {
			transientErrorTries = 1;
		}
		this.transientErrorTries = transientErrorTries;
	}

	/**
	 * Get the statistics tracker.
	 * 
	 * @return the statistics tracker
	 */
	public MqttStats getMqttStats() {
		return mqttStats;
	}

	/**
	 * Set the statistics tracker.
	 * 
	 * @param mqttStats
	 *        the statistics tracker to set
	 */
	public void setMqttStats(MqttStats mqttStats) {
		this.mqttStats = mqttStats;
	}

	/**
	 * Get the subscribe QoS.
	 * 
	 * @return the QoS; defaults to {@link #DEFAULT_SUBSCRIBE_QOS}
	 */
	public MqttQos getSubscribeQos() {
		return subscribeQos;
	}

	/**
	 * Set the subscribe QoS.
	 * 
	 * @param subscribeQos
	 *        the subscribe QoS to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setSubscribeQos(MqttQos subscribeQos) {
		this.subscribeQos = requireNonNullArgument(subscribeQos, "subscribeQos");
	}

	/**
	 * Set the subscribe QoS as a level value.
	 * 
	 * @param level
	 *        the level to set
	 * @throws IllegalArgumentException
	 *         if the level is not supported
	 */
	public void setSubscribeQosLevel(int level) {
		setSubscribeQos(MqttQos.valueOf(level));
	}

	/**
	 * Get the subscribe timeout seconds.
	 * 
	 * @return the timeout seconds; defaults to
	 *         {@link #DEFAULT_SUBSCRIBE_TIMEOUT_SECONDS}
	 */
	public int getSubscribeTimeoutSeconds() {
		return subscribeTimeoutSeconds;
	}

	/**
	 * Set the subscribe timeout seconds.
	 * 
	 * @param subscribeTimeoutSeconds
	 *        the timeout seconds to set
	 */
	public void setSubscribeTimeoutSeconds(int subscribeTimeoutSeconds) {
		this.subscribeTimeoutSeconds = subscribeTimeoutSeconds;
	}

	/**
	 * Get the retained flag.
	 * 
	 * @return the retained; defaults to {@literal false}
	 */
	public boolean isRetained() {
		return retained;
	}

	/**
	 * Set the retained flag
	 * 
	 * @param retained
	 *        the retained to set
	 */
	public void setRetained(boolean retained) {
		this.retained = retained;
	}

	/**
	 * Get the publish QoS.
	 * 
	 * @return the QoS; defaults to {@link #DEFAULT_PUBLISH_QOS}
	 */
	public MqttQos getPublishQos() {
		return publishQos;
	}

	/**
	 * Set the publish QoS.
	 * 
	 * @param publishQos
	 *        the QoS to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setPublishQos(MqttQos publishQos) {
		this.publishQos = requireNonNullArgument(publishQos, "publishQos");
	}

	/**
	 * Set the publish QoS as a level value.
	 * 
	 * @param level
	 *        the QoS level to set
	 * @throws IllegalArgumentException
	 *         if the level is not supported
	 */
	public void setPublishQosLevel(int level) {
		setPublishQos(MqttQos.valueOf(level));
	}

	/**
	 * Get a publish timeout, in seconds.
	 * 
	 * @return the timeout seconds; defaults to
	 *         {@link #DEFAULT_PUBLISH_TIMEOUT_SECONDS}
	 */
	public int getPublishTimeoutSeconds() {
		return publishTimeoutSeconds;
	}

	/**
	 * Set a publish timeout, in seconds.
	 * 
	 * @param publishTimeoutSeconds
	 *        the timeout to set
	 */
	public void setPublishTimeoutSeconds(int publishTimeoutSeconds) {
		this.publishTimeoutSeconds = publishTimeoutSeconds;
	}

}
