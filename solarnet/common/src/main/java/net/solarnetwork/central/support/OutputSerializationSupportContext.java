/* ==================================================================
 * SerializationSupportContext.java - 18/11/2022 12:22:58 pm
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

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.PropertySerializerRegistrar;

/**
 * Supporting services for output serialization.
 * 
 * @param jsonObjectMapper
 *        the ObjectMapper to use for JSON
 * @param cborObjectMapper
 *        the ObjectMapper to ues for CBOR
 * @param jsonSerializer
 *        the serializer to use for JSON/CBOR output
 * @param registrar
 *        a property serializer registrar for non-JSON/CBOR output
 * @author matt
 * @version 1.0
 */
public record OutputSerializationSupportContext<T> (ObjectMapper jsonObjectMapper,
		ObjectMapper cborObjectMapper, JsonSerializer<T> jsonSerializer,
		PropertySerializerRegistrar registrar) {

}
