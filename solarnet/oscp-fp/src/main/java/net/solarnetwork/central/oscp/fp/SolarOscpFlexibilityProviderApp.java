/* ==================================================================
 * SolarOscpFlexibilityProviderApp.java - 10/08/2022 5:11:01 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration;

/**
 * Main entry point for the SolarOscp Flexibility Provider application.
 * 
 * @author matt
 * @version 1.0
 */
//@formatter:off
@SpringBootApplication(scanBasePackageClasses = {
		SolarNetOscpConfiguration.class,
		SolarOscpFlexibilityProviderApp.class,
})
@Import({
	SolarNetCommonConfiguration.class
})
//@formatter:on
public class SolarOscpFlexibilityProviderApp {

	/**
	 * Command-line entry point to launching server.
	 * 
	 * @param args
	 *        command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SolarOscpFlexibilityProviderApp.class, args);
	}

}
