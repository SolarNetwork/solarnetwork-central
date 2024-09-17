/* ==================================================================
 * HttpIntegrationConfig.java - 17/09/2024 12:04:37 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package org.springframework.web.multipart.commons.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * HTTP integration test configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class HttpIntegrationTestsConfig {

	@Bean
	public CommonsMultipartResolver multipartResolver(
			@Value("${spring.servlet.multipart.max-file-size:10MB}") DataSize maxFileSize,
			@Value("${spring.servlet.multipart.max-request-size:10MB}") DataSize maxRequestSize,
			@Value("${spring.servlet.multipart.file-size-threshold:1MB}") DataSize fileSizeThreshold) {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(maxRequestSize.toBytes());
		multipartResolver.setMaxUploadSizePerFile(maxFileSize.toBytes());
		multipartResolver.setMaxInMemorySize((int) fileSizeThreshold.toBytes());
		return multipartResolver;
	}

}
