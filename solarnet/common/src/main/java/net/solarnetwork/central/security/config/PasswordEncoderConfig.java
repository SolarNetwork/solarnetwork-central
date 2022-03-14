/* ==================================================================
 * PasswordEncoderConfig.java - 21/10/2021 9:40:09 AM
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

package net.solarnetwork.central.security.config;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.central.security.DelegatingPasswordEncoder;

/**
 * Password encoder configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

	@SuppressWarnings("deprecation")
	@Bean
	public DelegatingPasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new LinkedHashMap<>(2);
		encoders.put("$2a$", new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A, 12,
				new SecureRandom()));
		encoders.put("{SHA}", new net.solarnetwork.central.security.LegacyPasswordEncoder());
		return new DelegatingPasswordEncoder(encoders);
	}

}
