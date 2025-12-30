/* ==================================================================
 * JsonConfig.java - 5/10/2021 9:13:20 PM
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

package net.solarnetwork.central.query.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * JSON configuration.
 * 
 * @author matt
 * @version 2.0
 */
@Configuration(proxyBeanMethods = false)
public class JsonConfig {

	/** A qualifier for CBOR handling. */
	public static final String CBOR_MAPPER = "cbor";

	/**
	 * Get the primary {@link JsonMapper} to use for JSON processing.
	 *
	 * @return the mapper
	 */
	@Primary
	@Bean
	public JsonMapper jsonMapper() {
		return DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER;
	}

	/**
	 * Get the primary {@link CBORMapper} to use for CBOR processing.
	 *
	 * @return the mapper
	 */
	@Bean
	@Qualifier(CBOR_MAPPER)
	public CBORMapper cborObjectMapper() {
		return CborUtils.CBOR_OBJECT_MAPPER.rebuild()
				.addModules(JsonUtils.DATUM_MODULE, DatumJsonUtils.DATUM_MODULE).build();
	}

}
