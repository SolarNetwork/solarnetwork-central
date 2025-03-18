/* ==================================================================
 * DatumExportConfig.java - 18/03/2025 7:11:58 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.datum.export.config.SolarNetDatumExportConfiguration;
import net.solarnetwork.central.security.PrefixedTextEncryptor;

/**
 * Datum export general configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumExportConfig implements SolarNetDatumExportConfiguration {

	@Bean
	@Qualifier(DATUM_EXPORT)
	public PrefixedTextEncryptor datumExportTextEncryptor(
			@Value("${app.datum.export.encryptor.password}") String password,
			@Value("${app.datum.export.encryptor.salt-hex}") String salt) {
		return PrefixedTextEncryptor.aesTextEncryptor(password, salt);
	}

}
