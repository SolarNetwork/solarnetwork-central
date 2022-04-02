/* ==================================================================
 * DatumImportStandardServiceConfig.java - 5/11/2021 3:20:06 PM
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.standard.BasicCsvDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.standard.CsvDatumImportInputProperties;
import net.solarnetwork.central.datum.imp.standard.SimpleCsvDatumImportInputFormatService;

/**
 * Configuration for datum import standard services.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
public class DatumImportStandardServiceConfig {

	@Bean
	public DatumImportInputFormatService basicCsvDatumImportInputFormatService() {
		BasicCsvDatumImportInputFormatService service = new BasicCsvDatumImportInputFormatService();

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(BasicCsvDatumImportInputFormatService.class.getName(),
				CsvDatumImportInputProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public DatumImportInputFormatService simpleCsvDatumImportInputFormatService() {
		SimpleCsvDatumImportInputFormatService service = new SimpleCsvDatumImportInputFormatService();

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SimpleCsvDatumImportInputFormatService.class.getName(),
				CsvDatumImportInputProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
