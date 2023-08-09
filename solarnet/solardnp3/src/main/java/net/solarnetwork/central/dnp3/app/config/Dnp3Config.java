/* ==================================================================
 * Dnp3Config.java - 9/08/2023 4:22:12 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.impl.DNP3ManagerFactory;
import net.solarnetwork.dnp3.util.Slf4jLogHandler;

/**
 * DNP3 configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class Dnp3Config {

	/** The default concurrency. */
	public static final int DEFAULT_CONCURRENCY = 2;

	@Value("${app.dnp3.concurrency:#{null}}")
	private Integer concurrency;

	@Bean(destroyMethod = "shutdown")
	public DNP3Manager dnp3Manager() {
		final int c = (concurrency != null ? concurrency.intValue() : DEFAULT_CONCURRENCY);
		return DNP3ManagerFactory.createManager(c, new Slf4jLogHandler());
	}

}
