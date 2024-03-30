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

package net.solarnetwork.central.oscp.fp.config;

import static net.solarnetwork.central.oscp.fp.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.oscp.mqtt.OscpActionDatumPublisher;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.JsonUtils;

/**
 * Configuration for SolarFlux publishing.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt")
public class SolarFluxPublishingConfig {

	/**
	 * A module for handling SolarFlux objects.
	 */
	public static final com.fasterxml.jackson.databind.Module SOLARFLUX_MODULE;
	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		SOLARFLUX_MODULE = m;
	}

	/**
	 * A mapper for SolarFlux publishing.
	 *
	 * @return the mapper
	 */
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

	/**
	 * Publish OSCP action events as SolarFlux datum.
	 *
	 * @param mapper
	 *        the mapper to use
	 * @return the publisher
	 */
	@Bean
	@ConfigurationProperties(prefix = "app.solarflux.datum-publish")
	@Qualifier(SOLARFLUX)
	public OscpActionDatumPublisher solarFluxOscpActionDatumPublisher(
			@Qualifier(SOLARFLUX) ObjectMapper mapper) {
		OscpActionDatumPublisher processor = new OscpActionDatumPublisher(mapper);
		return processor;
	}

	/**
	 * Publish OSCP action events as SolarFlux datum, using the {@link Consumer}
	 * API.
	 *
	 * @param publisher
	 *        the publisher
	 * @return the consumer
	 */
	@Bean
	@Qualifier(SOLARFLUX)
	public Consumer<DatumPublishEvent> solarFluxOscpActionDatumPublishConsumer(
			@Qualifier(SOLARFLUX) OscpActionDatumPublisher publisher) {
		return publisher.asConsumer();
	}

}
