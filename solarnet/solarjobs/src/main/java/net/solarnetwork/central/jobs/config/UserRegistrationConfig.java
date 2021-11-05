/* ==================================================================
 * UserConfig.java - 21/10/2021 10:00:06 AM
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.user.config.RegistrationBizConfig.USER_REGISTRATION;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import net.solarnetwork.central.user.biz.dao.UserValidator;

/**
 * Configuration for user registration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserRegistrationConfig {

	@Qualifier(USER_REGISTRATION)
	@Bean
	public Validator userValidator() {
		return new UserValidator();
	}

}
