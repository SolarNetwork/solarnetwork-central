/* ==================================================================
 * NoopUserNodeInstructionTaskDao.java - 1/12/2025 10:41:58 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * A fake {@link UserNodeInstructionTaskDao} to help with expression
 * simulations.
 * 
 * @author matt
 * @version 1.0
 */
public class NoopUserNodeInstructionTaskDao implements UserNodeInstructionTaskDao {

	/** A static shared instance. */
	public static final UserNodeInstructionTaskDao INSTANCE = new NoopUserNodeInstructionTaskDao();

	/**
	 * Constructor.
	 */
	public NoopUserNodeInstructionTaskDao() {
		super();
	}

	@Override
	public boolean updateTaskState(UserLongCompositePK id, BasicClaimableJobState desiredState,
			BasicClaimableJobState... expectedStates) {
		return true;
	}

	@Override
	public boolean updateTask(UserNodeInstructionTaskEntity info,
			BasicClaimableJobState... expectedStates) {
		return true;
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserNodeInstructionTaskEntity claimQueuedTask() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(UserNodeInstructionTaskFilter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> findFiltered(
			UserNodeInstructionTaskFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserLongCompositePK save(UserNodeInstructionTaskEntity entity) {
		return entity.getId();
	}

	@Override
	public Class<? extends UserNodeInstructionTaskEntity> getObjectType() {
		return UserNodeInstructionTaskEntity.class;
	}

	@Override
	public Collection<UserNodeInstructionTaskEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserNodeInstructionTaskEntity get(UserLongCompositePK id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(UserNodeInstructionTaskEntity entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<UserNodeInstructionTaskEntity> findAll(Long keyComponent1,
			List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserLongCompositePK create(Long keyComponent1, UserNodeInstructionTaskEntity entity) {
		throw new UnsupportedOperationException();
	}

}
