/* ==================================================================
 * OscpUtils.java - 7/10/2022 7:27:42 am
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

package net.solarnetwork.central.oscp.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Utilities for OSCP.
 * 
 * @author matt
 * @version 1.1
 */
public final class OscpUtils {

	private OscpUtils() {
		// not available
	}

	/**
	 * Get a JSON schema validator for OSCP 2.0.
	 * 
	 * @return the validator
	 */
	public static JsonSchemaFactory oscpSchemaFactory_v20() {
		try {
			AbsoluteIri baseIri = AbsoluteIri.of("http://www.openchargealliance.org/schemas/oscp/2.0/");
			Map<AbsoluteIri, Resource> uriMappings = new HashMap<>();
			PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(
					OscpUtils.class.getClassLoader());
			for ( Resource r : resourceResolver.getResources("classpath:schema/json/oscp/v20/*.json") ) {
				uriMappings.put(baseIri.resolve(r.getFilename()), r);
			}

			return JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(VersionFlag.V7))
					.schemaLoaders((l) -> {
						l.add((iri) -> {
							var r = uriMappings.get(iri);
							if ( r == null ) {
								return null;
							}
							return () -> r.getInputStream();
						});
					}).build();
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Error loading OSCP 2.0 JSON schema resources from classpath:schema/json/oscp/v20/*.json: "
							+ e.getMessage(),
					e);
		}
	}

}
