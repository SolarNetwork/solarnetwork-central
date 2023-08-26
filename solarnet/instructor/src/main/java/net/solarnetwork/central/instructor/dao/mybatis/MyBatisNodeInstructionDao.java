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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * MyBatis implementation of {@link NodeInstructionDao}.
 * 
 * @author matt
 * @version 1.8
 */
public class MyBatisNodeInstructionDao
		extends BaseMyBatisFilterableDao<NodeInstruction, EntityMatch, InstructionFilter, Long>
		implements NodeInstructionDao {

	/** Query name used by {@link #purgeCompletedInstructions(Instant)}. */
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
	 * {@link #compareAndUpdateInstructionState(Long, Long, InstructionState, InstructionState, Map)}.
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_COMPARE_STATE = "update-NodeInstruction-compare-state";

	/**
	 * Query name used by
	 * {@link #updateStaleInstructionsState(InstructionState, Instant, InstructionState)}.
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_STALE_STATE = "update-NodeInstruction-stale-state";

	/**
	 * Query name used by {@link #purgeIncompletInstructions(Instant)}.
	 * 
	 * @since 1.4
	 */
	public static final String UPDATE_PURGE_INCOMPLETE_INSTRUCTIONS = "delete-NodeInstruction-incomplete";

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
	public long purgeCompletedInstructions(Instant olderThanDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_COMPLETED_INSTRUCTIONS, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long purgeIncompleteInstructions(Instant olderThanDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_INCOMPLETE_INSTRUCTIONS, params);
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
		params.put("statusDate", Timestamp.from(Instant.now()));
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
		params.put("statusDate", Timestamp.from(Instant.now()));
		int count = getSqlSession().update(UPDATE_SET_STATE, params);
		return (count > 0);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long updateStaleInstructionsState(InstructionState currentState, Instant olderThanDate,
			InstructionState desiredState) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		params.put("expectedState", currentState);
		params.put("state", desiredState);
		params.put("statusDate", Timestamp.from(Instant.now()));
		return getSqlSession().update(UPDATE_STALE_STATE, params);
	}

	@Override
	protected Long executeFilterCountQuery(final String countQueryName, InstructionFilter filter,
			final Map<String, ?> sqlProps) {
		// count not supported, so don't bother trying
		return null;
	}

	@Override
	public void findFilteredStream(InstructionFilter filter,
			FilteredResultsProcessor<NodeInstruction> processor) throws IOException {
		requireNonNullArgument(filter, "filter");
		requireNonNullArgument(processor, "processor");
		processor.start(null, null, null, Collections.emptyMap()); // TODO: support count total results/offset/max
		try {
			getSqlSession().select("findall-NodeInstruction-EntityMatch",
					singletonMap(FILTER_PROPERTY, filter), new ResultHandler<NodeInstruction>() {

						@Override
						public void handleResult(
								ResultContext<? extends NodeInstruction> resultContext) {
							NodeInstruction instr = resultContext.getResultObject();
							try {
								processor.handleResultItem(instr);
							} catch ( IOException e ) {
								resultContext.stop();
								throw new RuntimeException(e);
							}
						}
					});
		} catch ( RuntimeException e ) {
			if ( e.getCause() instanceof IOException ) {
				throw (IOException) e.getCause();
			}
			throw e;
		}
	}

}
