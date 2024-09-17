/* ==================================================================
 * BaseHttpIntegrationTest.java - 17/09/2024 11:52:50 am
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base test class for HTTP server integration.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootApplication
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = HttpIntegrationTestsConfig.class)
public abstract class BaseHttpIntegrationTests {

	@Autowired
	protected ServletWebServerApplicationContext server;

	@LocalServerPort
	protected int port;

	/**
	 * Get a URI builder to the local HTTP server.
	 *
	 * @param path
	 *        the path
	 * @return the builder
	 */
	protected UriComponentsBuilder serverUri(String path) {
		return UriComponentsBuilder.fromHttpUrl("http://localhost:%d%s".formatted(port, path));
	}

}
