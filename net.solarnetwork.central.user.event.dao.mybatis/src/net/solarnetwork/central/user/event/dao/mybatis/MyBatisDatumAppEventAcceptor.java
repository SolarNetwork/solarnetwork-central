/* ==================================================================
 * MyBatisDatumAppEventAcceptor.java - 5/06/2020 9:52:37 am
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

package net.solarnetwork.central.user.event.dao.mybatis;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.DatumAppEvent;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.event.domain.UserNodeEvent;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.central.user.event.domain.UserNodeEventTaskState;

/**
 * MyBatis implementation of {@link DatumAppEventAcceptor} that creates user
 * datum tasks out of datum events.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisDatumAppEventAcceptor extends BaseMyBatisDao
		implements UserNodeEventTaskDao, DatumAppEventAcceptor {

	@Override
	public void offerDatumEvent(DatumAppEvent event) {
		getSqlSession().insert("create-user-node-event-tasks-from-event", event);
	}

	@Override
	public UserNodeEvent claimQueuedTask(String topic) {
		return selectFirst("claim-queued-user-node-event-task", topic);
	}

	@Override
	public void taskCompleted(UserNodeEventTask task) {
		if ( task.getCompleted() == null ) {
			task.setCompleted(Instant.now());
		}
		if ( task.getStatus() == null ) {
			task.setStatus(UserNodeEventTaskState.Completed);
		}
		if ( task.getSuccess() == null ) {
			task.setSuccess(true);
		}
		getSqlSession().update("complete-user-node-event-task", task);
	}

	@Override
	public long purgeCompletedTasks(Instant olderThanDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		getSqlSession().update("purge-user-node-event-tasks", params);
		Object result = params.get("result");
		return (result instanceof Long ? ((Long) result).longValue() : 0);
	}

}
