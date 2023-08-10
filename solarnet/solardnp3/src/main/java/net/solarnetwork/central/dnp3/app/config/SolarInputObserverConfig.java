/* ==================================================================
 * SolarInputObserverConfig.java - 10/08/2023 2:13:14 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.config;

import static net.solarnetwork.central.dnp3.app.config.ObjectStreamMetadataCacheConfig.DATUM_METADATA_CACHE;
import static net.solarnetwork.central.dnp3.app.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.mqtt.SolarInputDatumObserver;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Configuration for the SolarInput observer service.
 * 
 * @author matt
 * @version 1.0
 */
@Profile("mqtt")
@Configuration(proxyBeanMethods = false)
public class SolarInputObserverConfig {

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private SolarNodeOwnershipDao solarNodeOwnershipDao;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired
	@Qualifier(DATUM_METADATA_CACHE)
	private Cache<DatumId, ObjectDatumStreamMetadata> datumMetadataCache;

	/**
	 * The SolarInput observer service.
	 * 
	 * @return the service
	 */
	@Bean
	@Qualifier(SOLARQUEUE)
	public SolarInputDatumObserver solarInputDatumObserver() {
		ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper(new CBORFactory());
		SolarInputDatumObserver service = new SolarInputDatumObserver(taskExecutor, objectMapper,
				solarNodeOwnershipDao, datumStreamMetadataDao);
		service.setMetadataCache(datumMetadataCache);
		return service;
	}

}
