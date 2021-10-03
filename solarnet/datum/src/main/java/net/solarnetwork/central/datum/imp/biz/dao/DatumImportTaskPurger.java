/* ==================================================================
 * DatumImportTaskPurger.java - 11/11/2018 8:44:55 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.biz.dao;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * A {@link Runnable} for removing tasks that have completed and expired after a
 * specific amount of time.
 * 
 * <p>
 * This class maintains a weak reference to the map passed to the constructor,
 * and if the map goes out of scope then {@link #run()} will throw a
 * {@link RuntimeException}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportTaskPurger implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(DatumImportTaskPurger.class);

	private final long completedTaskMinimumCacheTime;
	private final WeakReference<ConcurrentMap<UserUuidPK, DatumImportStatus>> taskMapRef;

	/**
	 * Constructor.
	 * 
	 * @param completedTaskMinimumCacheTime
	 *        the minimum time to keep in the task map after the task completes,
	 *        in milliseconds
	 * @param taskMap
	 *        the map of tasks to maintain and purge expired tasks from
	 */
	public DatumImportTaskPurger(long completedTaskMinimumCacheTime,
			ConcurrentMap<UserUuidPK, DatumImportStatus> taskMap) {
		super();
		this.completedTaskMinimumCacheTime = completedTaskMinimumCacheTime;
		this.taskMapRef = new WeakReference<ConcurrentMap<UserUuidPK, DatumImportStatus>>(taskMap);
	}

	@Override
	public void run() {
		ConcurrentMap<UserUuidPK, DatumImportStatus> taskMap = taskMapRef.get();
		if ( taskMap == null ) {
			throw new RuntimeException("Task map no longer available; exiting.");
		}
		for ( Iterator<DatumImportStatus> itr = taskMap.values().iterator(); itr.hasNext(); ) {
			DatumImportStatus status = itr.next();
			long completeDate = status.getCompletionDate();
			if ( status.isDone()
					&& completeDate + completedTaskMinimumCacheTime < System.currentTimeMillis() ) {
				log.info("Purging status for completed import task {}: {}", status.getJobId(),
						status.getJobState());
				itr.remove();
			}
		}
	}

}
