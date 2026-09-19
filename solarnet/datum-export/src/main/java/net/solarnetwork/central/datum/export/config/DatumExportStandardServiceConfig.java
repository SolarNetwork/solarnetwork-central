/* ==================================================================
 * DatumExportStandardServiceConfig.java - 5/11/2021 9:43:54 AM
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

package net.solarnetwork.central.datum.export.config;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.MinimalHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.dest.ftp.FtpDatumExportDestinationService;
import net.solarnetwork.central.datum.export.dest.ftp.FtpDestinationProperties;
import net.solarnetwork.central.datum.export.dest.http.HttpDatumExportDestinationService;
import net.solarnetwork.central.datum.export.dest.http.HttpDestinationProperties;
import net.solarnetwork.central.datum.export.dest.s3.S3DatumExportDestinationService;
import net.solarnetwork.central.datum.export.dest.s3.S3DestinationProperties;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.standard.JsonDatumExportOutputFormatService;

/**
 * Datum export standard service configuration.
 *
 * @author matt
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
public class DatumExportStandardServiceConfig implements SolarNetDatumExportConfiguration {

	@Value("${app.datum.export.temporary-dir}")
	private String temporaryDir;

	@Bean
	public DatumExportOutputFormatService csvDatumExportOutputFormatService() {
		CsvDatumExportOutputFormatService service = new CsvDatumExportOutputFormatService();
		service.setTemporaryPath(temporaryDir);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(CsvDatumExportOutputFormatService.class.getName(),
				OutputConfiguration.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public DatumExportOutputFormatService jsonDatumExportOutputFormatService() {
		JsonDatumExportOutputFormatService service = new JsonDatumExportOutputFormatService();
		service.setTemporaryPath(temporaryDir);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(JsonDatumExportOutputFormatService.class.getName(),
				OutputConfiguration.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	/**
	 * A task executor specific for use with the S3 Transfer manager.
	 *
	 * @return the task executor
	 */
	@Bean
	@Qualifier(DATUM_EXPORT)
	@ConfigurationProperties(prefix = "app.datum.export.s3.executor")
	public ThreadPoolTaskExecutor s3DatumExportTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("S3-Export-");
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(10);
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}

	@Bean
	public DatumExportDestinationService s3DatumExportDestinationService(
			@Qualifier(DATUM_EXPORT) ThreadPoolTaskExecutor taskExecutor) {
		S3DatumExportDestinationService service = new S3DatumExportDestinationService(
				taskExecutor.getThreadPoolExecutor());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(S3DestinationProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public DatumExportDestinationService httpDatumExportDestinationService() {
		MinimalHttpClient client = HttpClients.createMinimal();
		HttpDatumExportDestinationService service = new HttpDatumExportDestinationService(client);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(HttpDestinationProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public DatumExportDestinationService ftpDatumExportDestinationService() {
		FtpDatumExportDestinationService service = new FtpDatumExportDestinationService();

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(FtpDestinationProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
