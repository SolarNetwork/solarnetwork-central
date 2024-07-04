/* ==================================================================
 * AppWarmUpManager.java - 3/07/2024 4:55:06 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Component to "warm up" the application, primarily to get lazily-loaded
 * infrastructure loaded so the application is faster to respond to requests.
 * 
 * @author matt
 * @version 1.0
 */
public class AppWarmUpManager implements ServiceLifecycleObserver {

	private static final Logger log = LoggerFactory.getLogger(AppWarmUpManager.class);

	private final Iterable<AppWarmUpTask> warmUpTasks;

	/**
	 * Constructor.
	 * 
	 * @param warmUpTasks
	 *        the warm-up tasks to manage
	 */
	public AppWarmUpManager(Iterable<AppWarmUpTask> warmUpTasks) {
		super();
		this.warmUpTasks = warmUpTasks;
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	@Override
	public void serviceDidStartup() {
		if ( warmUpTasks == null ) {
			return;
		}
		log.info("Performing app warm-up tasks...");
		for ( AppWarmUpTask task : warmUpTasks ) {
			try {
				log.debug("Executing app warm-up task [{}]...", task);
				task.warmUp();
			} catch ( Exception e ) {
				log.error("App warm-up task [{}] threw exception: {}", task, e.getMessage(), e);
			}
		}
		log.info("App warm-up tasks complete.");
	}

}
