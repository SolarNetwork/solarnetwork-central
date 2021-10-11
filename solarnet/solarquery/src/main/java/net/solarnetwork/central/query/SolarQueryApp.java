/* ==================================================================
 * SolarQueryApp.java - 8/10/2021 9:22:28 AM
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

package net.solarnetwork.central.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import net.solarnetwork.util.ApplicationContextUtils;

/**
 * Main entry point for the SolarQuery application.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication(scanBasePackageClasses = { SolarQueryApp.class })
public class SolarQueryApp {

	private static final Logger LOG = LoggerFactory.getLogger(SolarQueryApp.class);

	/**
	 * Command-line entry point to launching server.
	 * 
	 * @param args
	 *        command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SolarQueryApp.class, args);
	}

	/**
	 * Get a command line argument processor.
	 * 
	 * @param ctx
	 *        The application context.
	 * @return The command line runner.
	 */
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			ApplicationContextUtils.traceBeanNames(ctx, LOG);
		};
	}

}
