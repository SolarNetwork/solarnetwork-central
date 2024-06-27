/* ==================================================================
 * SolarUserApp.java - 20/10/2021 3:02:24 PM
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

package net.solarnetwork.central.reg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import net.solarnetwork.central.aop.SolarNetCommonAopConfiguration;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.common.dao.config.SolarNetCommonDaoConfiguration;
import net.solarnetwork.central.datum.aop.SolarNetDatumAopConfiguration;
import net.solarnetwork.central.datum.config.SolarNetDatumConfiguration;
import net.solarnetwork.central.datum.export.config.SolarNetDatumExportConfiguration;
import net.solarnetwork.central.datum.imp.aop.SolarNetDatumImportAopConfiguration;
import net.solarnetwork.central.datum.imp.config.SolarNetDatumImportConfiguration;
import net.solarnetwork.central.din.config.SolarNetDatumInputConfiguration;
import net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration;
import net.solarnetwork.central.instructor.aop.SolarNetInstructorAopConfiguration;
import net.solarnetwork.central.instructor.config.SolarNetInstructorConfiguration;
import net.solarnetwork.central.mail.config.SolarNetCommonMailConfiguration;
import net.solarnetwork.central.security.config.SolarNetCommonSecurityConfiguration;
import net.solarnetwork.central.user.billing.aop.SolarNetUserBillingAopConfiguration;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.din.aop.SolarNetUserDatumInputAopConfiguration;
import net.solarnetwork.central.user.din.config.SolarNetUserDatumInputConfiguration;
import net.solarnetwork.central.user.dnp3.aop.SolarNetUserDnp3AopConfiguration;
import net.solarnetwork.central.user.dnp3.config.SolarNetUserDnp3Configuration;
import net.solarnetwork.central.user.event.aop.SolarNetUserEventAopConfiguration;
import net.solarnetwork.central.user.event.config.SolarNetUserEventConfiguration;
import net.solarnetwork.central.user.expire.aop.SolarNetUserExpireAopConfiguration;
import net.solarnetwork.central.user.expire.config.SolarNetUserExpireConfiguration;
import net.solarnetwork.central.user.export.aop.SolarNetUserExportAopConfiguration;
import net.solarnetwork.central.user.export.config.SolarNetUserExportConfiguration;
import net.solarnetwork.central.user.flux.aop.SolarNetUserFluxAopConfiguration;
import net.solarnetwork.central.user.flux.config.SolarNetUserFluxConfiguration;
import net.solarnetwork.central.user.inin.aop.SolarNetUserInstructionInputAopConfiguration;
import net.solarnetwork.central.user.inin.config.SolarNetUserInstructionInputConfiguration;
import net.solarnetwork.central.user.ocpp.aop.SolarNetUserOcppAopConfiguration;
import net.solarnetwork.central.user.ocpp.config.SolarNetUserOcppConfiguration;
import net.solarnetwork.central.user.oscp.aop.SolarNetUserOscpAopConfiguration;
import net.solarnetwork.central.user.oscp.config.SolarNetUserOscpConfiguration;
import net.solarnetwork.util.ApplicationContextUtils;

/**
 * Main entry point for the SolarUser application.
 *
 * @author matt
 * @version 1.1
 */
//@formatter:off
@SpringBootApplication(scanBasePackageClasses = {
		SolarNetCommonAopConfiguration.class,
		SolarNetDatumAopConfiguration.class,
		SolarNetDatumConfiguration.class,
		SolarNetDatumInputConfiguration.class,
		SolarNetDatumExportConfiguration.class,
		SolarNetDatumImportAopConfiguration.class,
		SolarNetDatumImportConfiguration.class,
		SolarNetInstructionInputConfiguration.class,
		SolarNetInstructorAopConfiguration.class,
		SolarNetInstructorConfiguration.class,
		SolarNetUserConfiguration.class,
		SolarNetUserBillingAopConfiguration.class,
		SolarNetUserDatumInputAopConfiguration.class,
		SolarNetUserDatumInputConfiguration.class,
		SolarNetUserDnp3AopConfiguration.class,
		SolarNetUserDnp3Configuration.class,
		SolarNetUserInstructionInputAopConfiguration.class,
		SolarNetUserInstructionInputConfiguration.class,
		SolarNetUserOcppAopConfiguration.class,
		SolarNetUserOcppConfiguration.class,
		SolarNetUserOscpAopConfiguration.class,
		SolarNetUserOscpConfiguration.class,
		SolarNetUserEventAopConfiguration.class,
		SolarNetUserEventConfiguration.class,
		SolarNetUserExpireAopConfiguration.class,
		SolarNetUserExpireConfiguration.class,
		SolarNetUserExportAopConfiguration.class,
		SolarNetUserExportConfiguration.class,
		SolarNetUserFluxAopConfiguration.class,
		SolarNetUserFluxConfiguration.class,
		SolarUserApp.class,
})
@Import({
		SolarNetCommonConfiguration.class,
		SolarNetCommonDaoConfiguration.class,
		SolarNetCommonMailConfiguration.class,
		SolarNetCommonSecurityConfiguration.class,
})
//@formatter:on
public class SolarUserApp {

	private static final Logger LOG = LoggerFactory.getLogger(SolarUserApp.class);

	/**
	 * Command-line entry point to launching server.
	 *
	 * @param args
	 *        command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SolarUserApp.class, args);
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
