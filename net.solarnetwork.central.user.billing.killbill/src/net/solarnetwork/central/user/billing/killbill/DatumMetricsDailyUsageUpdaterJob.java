/* ==================================================================
 * DatumMetricsDailyUsageUpdaterJob.java - 21/08/2017 10:32:51 AM
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

package net.solarnetwork.central.user.billing.killbill;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to execute the {@link DatumMetricsDailyUsageUpdaterService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMetricsDailyUsageUpdaterJob extends JobSupport {

	private final DatumMetricsDailyUsageUpdaterService service;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the {@link EventAdmin} to use
	 * @param service
	 *        the service to use
	 */
	public DatumMetricsDailyUsageUpdaterJob(EventAdmin eventAdmin,
			DatumMetricsDailyUsageUpdaterService service) {
		super(eventAdmin);
		this.service = service;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		service.execute();
		return true;
	}

}
