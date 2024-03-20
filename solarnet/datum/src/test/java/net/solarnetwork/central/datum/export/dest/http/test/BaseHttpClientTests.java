/* ==================================================================
 * BaseHttpClientTests.java - 20/03/2024 10:10:59 am
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

package net.solarnetwork.central.datum.export.dest.http.test;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for HTTP client tests with an embedded HTTP server.
 *
 * @author matt
 * @version 1.0
 */
public class BaseHttpClientTests {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * HTTP server operations.
	 *
	 * @param <V>
	 *        the return type
	 */
	@FunctionalInterface
	public static interface ServerOps<V> {

		/**
		 * Perform the HTTP server operations.
		 *
		 * @param server
		 *        the server
		 * @param port
		 *        the server listening port
		 * @param baseUrl
		 *        the server base URL
		 * @return the result
		 * @throws Exception
		 *         if any error occurs
		 */
		V go(Server server, int port, String baseUrl) throws Exception;

	}

	/**
	 * Perform HTTP server operations with a handler.
	 *
	 * @param <V>
	 *        the return type
	 * @param handler
	 *        the handler
	 * @param ops
	 *        the operations to perform
	 * @return the operations result
	 * @throws Exception
	 *         if any error occurs
	 */
	protected <V> V doWithHttpServer(Handler handler, ServerOps<V> ops) throws Exception {
		final Server server = new Server(0);
		server.setHandler(handler);
		server.start();

		int serverPort = 0;
		Connector c = server.getConnectors()[0];
		if ( c instanceof ServerConnector sc ) {
			serverPort = sc.getLocalPort();
		}

		String serverBaseUrl = "http://localhost:" + serverPort;
		try {
			return ops.go(server, serverPort, serverBaseUrl);
		} finally {
			server.stop();
		}
	}

}
