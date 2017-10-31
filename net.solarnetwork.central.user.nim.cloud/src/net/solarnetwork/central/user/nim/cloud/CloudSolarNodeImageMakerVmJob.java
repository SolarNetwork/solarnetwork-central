/* ==================================================================
 * CloudSolarNodeImageMakerVmJob.java - 1/11/2017 11:32:30 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.nim.cloud;

import java.util.concurrent.TimeUnit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Periodic job to perform virtual machine maintenance on the NIM service.
 * 
 * @author matt
 * @version 1.0
 */
public class CloudSolarNodeImageMakerVmJob extends JobSupport {

	private final CloudSolarNodeImageMakerBiz service;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the {@link EventAdmin} to use
	 * @param service
	 *        the service to use
	 */
	public CloudSolarNodeImageMakerVmJob(EventAdmin eventAdmin, CloudSolarNodeImageMakerBiz service) {
		super(eventAdmin);
		setJobGroup("NIM");
		setMaximumWaitMs(TimeUnit.MINUTES.toMillis(10));
		this.service = service;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		service.performVmMaintence();
		return true;
	}

}
