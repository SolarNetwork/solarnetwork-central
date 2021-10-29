/* ==================================================================
 * InvoiceGenerationTaskCreatorJob.java - 21/07/2020 12:18:24 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.jobs;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.billing.snf.domain.AccountTaskType;

/**
 * Job to create {@link AccountTaskType#GenerateInvoice} task entities.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceGenerationTaskCreatorJob extends JobSupport {

	private final InvoiceGenerationTaskCreator creator;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the event admin
	 * @param creator
	 *        the creator
	 */
	public InvoiceGenerationTaskCreatorJob(EventAdmin eventAdmin, InvoiceGenerationTaskCreator creator) {
		super(eventAdmin);
		this.creator = creator;
		setJobGroup("Billing");
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		creator.createTasks();
		return true;
	}

}
