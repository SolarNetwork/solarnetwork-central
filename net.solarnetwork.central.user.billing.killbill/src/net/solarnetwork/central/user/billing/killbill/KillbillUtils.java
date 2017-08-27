/* ==================================================================
 * KillbillUtils.java - 25/08/2017 2:07:42 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.util.BigDecimalStringSerializer;

/**
 * Global Killbill helpers.
 * 
 * @author matt
 * @version 1.0
 */
public final class KillbillUtils {

	/** The default payment method data to add to new accounts. */
	public static final Map<String, Object> EXTERNAL_PAMENT_METHOD_DATA = externalPaymentMethodData();

	private static Map<String, Object> externalPaymentMethodData() {
		Map<String, Object> map = new HashMap<>();
		map.put("pluginName", "__EXTERNAL_PAYMENT__");
		map.put("pluginInfo", Collections.emptyMap());
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Get an {@link ObjectMapper} instance configured with standard support for
	 * Killbill interaction.
	 * 
	 * @return an ObjectMapper
	 */
	public static final ObjectMapper defaultObjectMapper() {
		return Jackson2ObjectMapperBuilder.json()
				.serializerByType(BigDecimal.class, BigDecimalStringSerializer.INSTANCE)
				.serializationInclusion(Include.NON_NULL).build();
	}

}
