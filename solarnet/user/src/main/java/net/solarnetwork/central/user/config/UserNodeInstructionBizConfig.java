/* ==================================================================
 * UserNodeInstructionBizConfig.java - 16/11/2025 5:42:23 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Validator;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionBiz;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;

/**
 * Configuration for the user instruction services.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserNodeInstructionBizConfig {

	@Autowired
	private UserNodeInstructionService instructionService;

	@Autowired
	private UserNodeInstructionTaskDao instructionTaskDao;

	@Autowired
	private Validator validator;

	@Bean
	public DaoUserNodeInstructionBiz userInstructionBiz() {
		var biz = new DaoUserNodeInstructionBiz(instructionService, instructionTaskDao);

		biz.setValidator(validator);

		return biz;
	}

}
