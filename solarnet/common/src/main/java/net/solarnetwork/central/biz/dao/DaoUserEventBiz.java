/* ==================================================================
 * DaoUserEventBiz.java - 6/08/2022 10:08:54 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import net.solarnetwork.central.biz.UserEventBiz;
import net.solarnetwork.central.common.dao.UserEventDao;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.FilteredResultsProcessor;

/**
 * DAO implementation of {@link UserEventBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserEventBiz implements UserEventBiz {

	private final UserEventDao userEventDao;

	/**
	 * Constructor.
	 * 
	 * @param userEventDao
	 *        the user event DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserEventBiz(UserEventDao userEventDao) {
		super();
		this.userEventDao = requireNonNullArgument(userEventDao, "userEventDao");
	}

	@Override
	public void findFilteredUserEvents(UserEventFilter filter,
			FilteredResultsProcessor<UserEvent> processor) throws IOException {
		userEventDao.findFilteredStream(filter, processor);
	}

}
