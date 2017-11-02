/* ==================================================================
 * SolarNodeImageMakerMaintenanceJob.java - 1/11/2017 11:32:30 AM
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

package net.solarnetwork.central.user.nim.jobs;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.biz.MaintenanceSubscriber;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.nim.biz.SolarNodeImageMakerBiz;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Periodic job to perform maintenance on registered NIM services.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeImageMakerMaintenanceJob extends JobSupport {

	private final OptionalServiceCollection<SolarNodeImageMakerBiz> services;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the {@link EventAdmin} to use
	 * @param services
	 *        the services to manage
	 */
	public SolarNodeImageMakerMaintenanceJob(EventAdmin eventAdmin,
			OptionalServiceCollection<SolarNodeImageMakerBiz> services) {
		super(eventAdmin);
		setJobGroup("NIM");
		setMaximumWaitMs(TimeUnit.MINUTES.toMillis(10));
		this.services = services;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		for ( SolarNodeImageMakerBiz service : services.services() ) {
			if ( service instanceof MaintenanceSubscriber ) {
				((MaintenanceSubscriber) service).performServiceMaintenance(Collections.emptyMap());
			}
		}
		return true;
	}

}
