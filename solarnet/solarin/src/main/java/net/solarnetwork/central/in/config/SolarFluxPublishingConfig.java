/* ==================================================================
 * SolarFluxPublishingConfig.java - 10/11/2021 9:22:14 PM
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

package net.solarnetwork.central.in.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;

/**
 * Configuration for SolarFlux publishing.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
@Profile("mqtt")
public class SolarFluxPublishingConfig {

	/** A qualifier for SolarFlux. */
	public static final String SOLARFLUX = "solarflux";

	/**
	 * A module for handling SolarFlux objects.
	 * 
	 * @since 1.1
	 */
	public static final com.fasterxml.jackson.databind.Module SOLARFLUX_MODULE;
	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		SOLARFLUX_MODULE = m;
	}

	@Autowired
	private MqttConnectionFactory mqttConnectionFactory;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Bean
	@Qualifier(SOLARFLUX)
	public ObjectMapper solarFluxObjectMapper() {
		ObjectMapper mapper = JsonUtils
				.createObjectMapper(new CBORFactory(), JsonUtils.JAVA_TIMESTAMP_MODULE)
				.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
		mapper.registerModule(SOLARFLUX_MODULE);
		return mapper;
	}

	@ConfigurationProperties(prefix = "app.datum.solarflux-publish")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Qualifier(SOLARFLUX)
	public SolarFluxDatumPublisher solarFluxDatumPublisher(
			@Qualifier(SOLARFLUX) List<MqttConnectionObserver> solarFluxConnectionObservers) {
		SolarFluxDatumPublisher processor = new SolarFluxDatumPublisher(mqttConnectionFactory,
				nodeOwnershipDao, solarFluxObjectMapper(), solarFluxConnectionObservers);
		return processor;
	}

}
