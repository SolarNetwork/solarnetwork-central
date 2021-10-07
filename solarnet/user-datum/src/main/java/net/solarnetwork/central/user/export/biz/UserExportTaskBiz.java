/* ==================================================================
 * UserExportTaskBiz.java - 24/04/2018 10:05:40 AM
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

package net.solarnetwork.central.user.export.biz;

import java.time.Instant;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;

/**
 * Service API for operations related to user datum export tasks.
 * 
 * <p>
 * This API is meant more for internal use by export jobs.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface UserExportTaskBiz {

	/**
	 * Submit a datum export configuration for task execution.
	 * 
	 * @param config
	 *        the export configuration to create tasks for
	 * @param exportDate
	 *        the export date to use
	 * @return the created task, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code configuration} is not complete enough to create an
	 *         export task
	 */
	UserDatumExportTaskInfo submitDatumExportConfiguration(UserDatumExportConfiguration config,
			Instant exportDate);

	/**
	 * Submit an ad hoc datum export configuration for task execution.
	 * 
	 * @param config
	 *        the export configuration to create tasks for
	 * @return the created task, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code configuration} is not complete enough to create an
	 *         export task
	 * @since 1.1
	 */
	UserAdhocDatumExportTaskInfo submitAdhocDatumExportConfiguration(
			UserDatumExportConfiguration config);

}
