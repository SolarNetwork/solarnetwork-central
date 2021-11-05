/* ==================================================================
 * SolarJobsApp.java - 5/11/2021 3:47:46 PM
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

package net.solarnetwork.central.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.common.dao.config.SolarNetCommonDaoConfiguration;
import net.solarnetwork.central.datum.config.SolarNetDatumConfiguration;
import net.solarnetwork.central.datum.export.config.SolarNetDatumExportConfiguration;
import net.solarnetwork.central.datum.imp.config.SolarNetDatumImportConfiguration;
import net.solarnetwork.central.instructor.config.SolarNetInstructorConfiguration;
import net.solarnetwork.central.mail.config.SolarNetCommonMailConfiguration;
import net.solarnetwork.central.security.config.SolarNetCommonSecurityConfiguration;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.event.config.SolarNetUserEventConfiguration;
import net.solarnetwork.central.user.expire.config.SolarNetUserExpireConfiguration;
import net.solarnetwork.central.user.export.config.SolarNetUserExportConfiguration;
import net.solarnetwork.util.ApplicationContextUtils;

/**
 * Main entry point for the SolarJobs application.
 * 
 * @author matt
 * @version 1.0
 */
//@formatter:off
@SpringBootApplication(scanBasePackageClasses = {
		SolarNetDatumConfiguration.class,
		SolarNetDatumExportConfiguration.class,
		SolarNetDatumImportConfiguration.class,
		SolarNetInstructorConfiguration.class,
		SolarNetUserConfiguration.class,
		SolarNetUserEventConfiguration.class,
		SolarNetUserExpireConfiguration.class,
		SolarNetUserExportConfiguration.class,
		SolarJobsApp.class,
})
@Import({
		SolarNetCommonConfiguration.class,
		SolarNetCommonDaoConfiguration.class,
		SolarNetCommonMailConfiguration.class,
		SolarNetCommonSecurityConfiguration.class,
})
//@formatter:on
public class SolarJobsApp {

	private static final Logger LOG = LoggerFactory.getLogger(SolarJobsApp.class);

	/**
	 * Command-line entry point to launching server.
	 * 
	 * @param args
	 *        command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SolarJobsApp.class, args);
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
