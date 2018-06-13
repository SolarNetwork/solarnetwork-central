/* ==================================================================
 * MyBatisNodeInstructionDao.java - Nov 12, 2014 6:33:35 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.dao.mybatis;

import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.util.JsonUtils;

/**
 * MyBatis implementation of {@link NodeInstructionDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisNodeInstructionDao
		extends BaseMyBatisFilterableDao<NodeInstruction, EntityMatch, InstructionFilter, Long>
		implements NodeInstructionDao {

	/** Query name used by {@link #purgeCompletedInstructions(DateTime)}. */
	public static final String UPDATE_PURGE_COMPLETED_INSTRUCTIONS = "delete-NodeInstruction-completed";

	/**
	 * Query name used by
	 * {@link #updateNodeInstructionState(Long, Long, InstructionState, Map)}.
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_SET_STATE = "update-NodeInstruction-state";

	/**
	 * Query name used by
	 * {@link #compareAndStoreInstruction(Long, InstructionState, Instruction)}.
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_COMPARE_STATE = "update-NodeInstruction-compare-state";

	/**
	 * Query name used by
	 * {@link #updateStaleInstructionStates(InstructionState, DateTime, InstructionState)
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_STALE_STATE = "update-NodeInstruction-stale-state";

	/**
	 * Default constructor.
	 */
	public MyBatisNodeInstructionDao() {
		super(NodeInstruction.class, Long.class, EntityMatch.class);
	}

	@Override
	protected Long handleInsert(NodeInstruction datum) {
		Long result = super.handleInsert(datum);
		handleRelation(result, datum.getParameters(), InstructionParameter.class, null);
		return result;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long purgeCompletedInstructions(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_COMPLETED_INSTRUCTIONS, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean compareAndUpdateInstructionState(Long instructionId, Long nodeId,
			InstructionState expectedState, InstructionState state, Map<String, ?> resultParameters) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("id", instructionId);
		params.put("nodeId", nodeId);
		params.put("expectedState", expectedState);
		params.put("state", state);
		params.put("resultParametersJson",
				resultParameters != null ? JsonUtils.getJSONString(resultParameters, null) : null);
		int count = getSqlSession().update(UPDATE_COMPARE_STATE, params);
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean updateNodeInstructionState(Long instructionId, Long nodeId, InstructionState state,
			Map<String, ?> resultParameters) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("id", instructionId);
		params.put("nodeId", nodeId);
		params.put("state", state);
		params.put("resultParametersJson",
				resultParameters != null ? JsonUtils.getJSONString(resultParameters, null) : null);
		int count = getSqlSession().update(UPDATE_SET_STATE, params);
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long updateStaleInstructionsState(InstructionState currentState, DateTime olderThanDate,
			InstructionState desiredState) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		params.put("expectedState", currentState);
		params.put("state", desiredState);
		return getSqlSession().update(UPDATE_STALE_STATE, params);
	}

}
