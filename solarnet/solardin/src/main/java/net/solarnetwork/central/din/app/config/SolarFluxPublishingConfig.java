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

package net.solarnetwork.central.din.app.config;

import static net.solarnetwork.central.din.app.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher;
import net.solarnetwork.central.datum.flux.dao.FluxPublishSettingsDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.codec.jackson.JsonDateUtils;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * Configuration for SolarFlux publishing.
 *
 * @author matt
 * @version 2.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt")
public class SolarFluxPublishingConfig {

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private FluxPublishSettingsDao fluxPublishSettingsDao;

	/**
	 * A module for handling SolarFlux objects.
	 *
	 * @since 1.1
	 */
	public static final JacksonModule SOLARFLUX_MODULE;
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
	public CBORMapper solarFluxObjectMapper() {
		return CborUtils.CBOR_OBJECT_MAPPER.rebuild()
				.addModules(JsonDateUtils.JAVA_TIMESTAMP_MODULE, SOLARFLUX_MODULE)
				.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS).build();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarflux.datum-publish")
	@Qualifier(SOLARFLUX)
	public SolarFluxDatumPublisher solarFluxDatumPublisher(@Qualifier(SOLARFLUX) ObjectMapper mapper) {
		SolarFluxDatumPublisher processor = new SolarFluxDatumPublisher(nodeOwnershipDao,
				fluxPublishSettingsDao, mapper);
		return processor;
	}

}
