/* ==================================================================
 * S3ResourceStorageConfig.java - 5/11/2021 2:40:18 PM
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

package net.solarnetwork.central.datum.imp.config;

import static net.solarnetwork.central.datum.imp.config.SolarNetDatumImportConfiguration.DATUM_IMPORT;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.common.s3.S3ResourceStorageService;
import net.solarnetwork.service.ResourceStorageService;

/**
 * S3 resource storage configuration for datum import.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("datum-import-s3-resource-storage")
public class S3ResourceStorageConfig {

	@Autowired
	public Executor executor;

	@ConfigurationProperties(prefix = "app.datum.import.s3-storage")
	@Qualifier(DATUM_IMPORT)
	@Bean
	public ResourceStorageService datumImportResourceStorageService() {
		return new S3ResourceStorageService("Datum-Import", executor);
	}

}
