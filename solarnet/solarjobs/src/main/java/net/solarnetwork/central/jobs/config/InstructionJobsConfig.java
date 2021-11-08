/* ==================================================================
 * InstructionJobsConfig.java - 8/11/2021 3:29:20 PM
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.jobs.NodeInstructionCleaner;
import net.solarnetwork.central.instructor.jobs.StaleNodeStateUpdater;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * Instructor jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class InstructionJobsConfig {

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@ConfigurationProperties(prefix = "app.job.datum.instr.cleaner")
	@Bean
	public ManagedJob completedNodeInstructionCleaner() {
		NodeInstructionCleaner job = new NodeInstructionCleaner(nodeInstructionDao);
		job.setId("NodeInstructionCleaner");
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.datum.instr.stale-queuing")
	@Bean
	public ManagedJob queuingStaleNodeStateUpdator() {
		StaleNodeStateUpdater job = new StaleNodeStateUpdater(nodeInstructionDao);
		job.setId("StaleNodeStateUpdater-Queuing");
		return job;
	}

}
