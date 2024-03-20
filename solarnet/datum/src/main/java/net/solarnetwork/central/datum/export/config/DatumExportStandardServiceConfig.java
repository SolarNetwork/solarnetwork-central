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

import java.util.concurrent.ExecutorService;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.MinimalHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
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
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class DatumExportStandardServiceConfig {

	@Value("${app.datum.export.temporary-dir}")
	private String temporaryDir;

	@Autowired
	private ExecutorService executorService;

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

	@Bean
	public DatumExportDestinationService s3DatumExportDestinationService() {
		S3DatumExportDestinationService service = new S3DatumExportDestinationService(executorService);

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

}
