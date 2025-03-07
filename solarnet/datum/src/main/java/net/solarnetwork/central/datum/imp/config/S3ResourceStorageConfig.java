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

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.common.s3.S3Client;
import net.solarnetwork.common.s3.S3ResourceStorageService;
import net.solarnetwork.common.s3.sdk2.Sdk2S3Client;
import net.solarnetwork.service.ResourceStorageService;

/**
 * S3 resource storage configuration for datum import.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("datum-import-s3-resource-storage")
public class S3ResourceStorageConfig implements SolarNetDatumImportConfiguration {

	@Autowired
	public Executor executor;

	@ConfigurationProperties(prefix = "app.datum.import.s3-storage.executor")
	@Qualifier(DATUM_IMPORT)
	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskExecutor datumImportS3Executor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-S3-Import-");
		executor.setCorePoolSize(2);
		return executor;
	}

	@ConfigurationProperties(prefix = "app.datum.import.s3-storage.s3-client")
	@Qualifier(DATUM_IMPORT)
	@Bean
	public S3Client datumImportS3Client(@Qualifier(DATUM_IMPORT) ThreadPoolTaskExecutor executor) {
		return new Sdk2S3Client(executor.getThreadPoolExecutor(), "Datum-Import");
	}

	@ConfigurationProperties(prefix = "app.datum.import.s3-storage.service")
	@Qualifier(DATUM_IMPORT)
	@Bean(initMethod = "startup")
	public ResourceStorageService datumImportResourceStorageService(
			@Qualifier(DATUM_IMPORT) S3Client s3Client) {
		S3ResourceStorageService service = new S3ResourceStorageService(executor);
		service.setUid("Datum-Import");
		service.setS3Client(s3Client);
		return service;
	}

}
