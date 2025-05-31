/* ==================================================================
 * MyBatisNodeInstructionDaoTests.java - Nov 12, 2014 6:41:59 AM
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

package net.solarnetwork.central.instructor.dao.mybatis.test;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.instructor.dao.mybatis.MyBatisNodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link MyBatisNodeInstructionDao} class.
 * 
 * @author matt
 * @version 2.6
 */
public class MyBatisNodeInstructionDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisNodeInstructionDao dao;

	private NodeInstruction lastDatum;
	private List<NodeInstruction> addedInstructions;

	@BeforeEach
	public void setUp() throws Exception {
		dao = new MyBatisNodeInstructionDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastDatum = null;
		addedInstructions = new ArrayList<NodeInstruction>(4);
		setupTestNode();
	}

	private NodeInstruction storeNewInstruction(Long nodeId) {
		return storeNewInstruction(nodeId, Instant.now());
	}

	private NodeInstruction storeNewInstruction(Long nodeId, Instant date) {
		return storeNewInstruction(nodeId, date, null);
	}

	private NodeInstruction storeNewInstruction(Long nodeId, Instant date,
			Map<String, Object> resultParams) {
		return storeNewInstruction(nodeId, date, null, null);
	}

	private NodeInstruction storeNewInstruction(Long nodeId, Instant date,
			Map<String, Object> resultParams, Consumer<NodeInstruction> callback) {

		NodeInstruction instr = new NodeInstruction();
		instr.setCreated(Instant.now());
		instr.getInstruction().setStatusDate(date);
		instr.getInstruction().setInstructionDate(date);
		instr.setNodeId(nodeId);
		instr.getInstruction().setState(InstructionState.Queued);
		instr.getInstruction().setTopic("Test Topic");
		instr.getInstruction().setResultParameters(resultParams);

		instr.getInstruction().addParameter("Test param 1", "Test value 1");
		instr.getInstruction().addParameter("Test param 2", "Test value 2");

		if ( callback != null ) {
			callback.accept(instr);
		}

		Long id = dao.save(instr);
		then(id).as("ID returned").isNotNull();
		instr.setId(id);
		addedInstructions.add(instr);
		return instr;
	}

	@Test
	public void storeNew() {
		lastDatum = storeNewInstruction(TEST_NODE_ID);
	}

	private void validate(NodeInstruction srcEntity, NodeInstruction entity) {
		final Instruction src = srcEntity.getInstruction();
		// @formatter:off
		then(entity)
			.as("NodeInstruction should exist")
			.isNotNull()
			.satisfies(instr -> {
				then(instr.getCreated())
					.as("Creation date is non-null")
					.isNotNull()
					;
			})
			.as("Node ID copied")
			.returns(srcEntity.getNodeId(), from(NodeInstruction::getNodeId))
			.extracting(NodeInstruction::getInstruction)
			.as("Topic copied")
			.returns(src.getTopic(), from(Instruction::getTopic))
			.as("Instruction date copied")
			.returns(src.getInstructionDate(), from(Instruction::getInstructionDate))
			.as("State copied")
			.returns(src.getState(), from(Instruction::getState))
			.as("Status date copied")
			.returns(src.getStatusDate(), from(Instruction::getStatusDate))
			.as("Instruction parameters copied")
			.returns(src.getParameters(), from(Instruction::getParameters))
			.as("Instruction date copied")
			.returns(src.getResultParameters(), from(Instruction::getResultParameters))
			.as("Expiration date copied")
			.returns(src.getExpirationDate(), from(Instruction::getExpirationDate))
			;
		// @formatter:on
	}

	private List<Map<String, Object>> allNodeInstructionRows() {
		return allTableData(log, jdbcTemplate, "solarnet.sn_node_instruction", "id");
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeNew_withResultParameters() {
		lastDatum = storeNewInstruction(TEST_NODE_ID, Instant.now(), singletonMap("foo", "bar"));
	}

	@Test
	public void getByPrimaryKey_withResultParameters() {
		storeNew_withResultParameters();
		NodeInstruction datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeNew_withExpiration() {
		// GIVEN
		lastDatum = storeNewInstruction(TEST_NODE_ID, Instant.now(), null, instr -> {
			instr.getInstruction().setExpirationDate(Instant.now().truncatedTo(HOURS).plus(1, HOURS));
		});

		// THEN
		List<Map<String, Object>> rows = allNodeInstructionRows();
		// @formatter:off
		then(rows)
			.as("One row stored")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID persisted")
			.containsEntry("id", lastDatum.getId())
			.as("Expiration date persisted")
			.containsEntry("expire_date", Timestamp.from(lastDatum.getInstruction().getExpirationDate()))
			;
		// @formatter:on
	}

	@Test
	public void getByPrimaryKey_withExpiration() {
		// GIVEN
		storeNew_withExpiration();

		// WHEN
		NodeInstruction result = dao.get(lastDatum.getId());

		// THEN
		validate(lastDatum, result);
	}

	@Test
	public void update() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.getInstruction().setState(InstructionState.Declined);
		datum.getInstruction().setStatusDate(Instant.now());
		Long newId = dao.save(datum);
		then(newId).as("ID preserved").isEqualTo(datum.getId());
		NodeInstruction datum2 = dao.get(datum.getId());
		validate(datum, datum2);
	}

	@Test
	public void updateWithResultParameters() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.getInstruction().setState(InstructionState.Completed);
		datum.getInstruction().setStatusDate(Instant.now());
		datum.getInstruction().setResultParameters(Collections.singletonMap("foo", (Object) "bar"));
		Long newId = dao.save(datum);
		then(newId).as("ID preserved").isEqualTo(datum.getId());
		NodeInstruction datum2 = dao.get(datum.getId());
		validate(datum, datum2);
	}

	@Test
	public void findByNodeId() {
		final Long node2Id = TEST_NODE_ID - 1L;
		setupTestNode(node2Id);

		storeNew();

		// store a second for a different node ID, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.getInstruction().setStatusDate(Instant.now());
		datum.getInstruction().setInstructionDate(Instant.now());
		datum.setNodeId(node2Id);
		datum.getInstruction().setState(InstructionState.Queued);
		datum.getInstruction().setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		then(instr2Id).as("ID 2 returned").isNotNull();
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 1 result")
			.hasSize(1)
			.satisfies(r -> {
				then(r)
					.element(0)
					.isEqualTo(lastDatum)
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(1L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(1, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findByNodeIds() {
		storeNew();

		final List<Long> instructionIds = new ArrayList<Long>(2);

		final Long node2Id = TEST_NODE_ID - 1L;
		setupTestNode(node2Id);
		NodeInstruction instr2 = storeNewInstruction(node2Id);
		instructionIds.add(instr2.getId());

		final Long node3Id = TEST_NODE_ID - 2L;
		setupTestNode(node3Id);
		NodeInstruction instr3 = storeNewInstruction(node3Id);
		instructionIds.add(instr3.getId());

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(new Long[] { node2Id, node3Id });
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 2 result")
			.hasSize(2)
			.satisfies(r -> {
				then(r)
					.asInstanceOf(iterable(NodeInstruction.class))
					.contains(instr2, instr3)
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(2L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(2, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findByInstructionIds() {
		storeNew();

		final Long node2Id = TEST_NODE_ID - 1L;
		setupTestNode(node2Id);
		storeNewInstruction(node2Id);

		final Long node3Id = TEST_NODE_ID - 2L;
		setupTestNode(node3Id);
		storeNewInstruction(node3Id);

		final Long[] instructionIds = new Long[] { addedInstructions.get(0).getId(),
				addedInstructions.get(2).getId() };

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(instructionIds);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 2 result")
			.hasSize(2)
			.satisfies(r -> {
				then(r)
					.asInstanceOf(iterable(NodeInstruction.class))
					.contains(addedInstructions.get(0), addedInstructions.get(2))
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(2L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(2, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findActive() {
		findByNodeId();

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 1 result")
			.hasSize(1)
			.satisfies(r -> {
				then(r)
					.element(0)
					.isEqualTo(lastDatum)
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(1L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(1, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findActiveForNodes() {
		storeNew();
		final Long node2Id = TEST_NODE_ID - 1L;
		setupTestNode(node2Id);
		storeNewInstruction(node2Id);

		final Long node3Id = TEST_NODE_ID - 2L;
		setupTestNode(node3Id);
		storeNewInstruction(node3Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(new Long[] { node2Id, node3Id });
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 2 result")
			.hasSize(2)
			.satisfies(r -> {
				then(r)
					.asInstanceOf(iterable(NodeInstruction.class))
					.contains(addedInstructions.get(1), addedInstructions.get(2))
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(2L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(2, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findActiveForNodes_stream() throws IOException {
		// GIVEN
		storeNew();
		final Long node2Id = TEST_NODE_ID - 1L;
		setupTestNode(node2Id);
		storeNewInstruction(node2Id);

		final Long node3Id = TEST_NODE_ID - 2L;
		setupTestNode(node3Id);
		storeNewInstruction(node3Id);

		// WHEN
		SimpleInstructionFilter f = new SimpleInstructionFilter();
		f.setNodeIds(new Long[] { node2Id, node3Id });
		f.setState(InstructionState.Queued);
		List<NodeInstruction> results = new ArrayList<>(2);
		dao.findFilteredStream(f, new AbstractFilteredResultsProcessor<NodeInstruction>() {

			@Override
			public void handleResultItem(NodeInstruction resultItem) throws IOException {
				results.add(resultItem);
			}

		});

		// THEN
		assertThat("Results for node 2 and 3 returned", results, hasSize(2));
		int i = 0;
		for ( NodeInstruction instr : results ) {
			NodeInstruction expected = addedInstructions.get(++i);
			assertThat("Instruction " + i, instr.getId(), is(equalTo(expected.getId())));
		}
	}

	@Test
	public void findForDateRange_stream() throws IOException {
		// GIVEN
		final Instant ts1 = Instant.now().truncatedTo(MINUTES).minus(1, HOURS);
		storeNewInstruction(TEST_NODE_ID, ts1);
		final Instant ts2 = Instant.now().truncatedTo(MINUTES);
		storeNewInstruction(TEST_NODE_ID, ts2);
		final Instant ts3 = Instant.now().truncatedTo(MINUTES).plus(1, HOURS);
		storeNewInstruction(TEST_NODE_ID, ts3, null, instr -> {
			instr.getInstruction().setExpirationDate(Instant.now().truncatedTo(HOURS).plus(1, HOURS));
		});

		// WHEN
		SimpleInstructionFilter f = new SimpleInstructionFilter();
		f.setNodeId(TEST_NODE_ID);
		f.setStartDate(ts1);
		f.setEndDate(ts3);
		List<NodeInstruction> results = new ArrayList<>(2);
		dao.findFilteredStream(f, new AbstractFilteredResultsProcessor<NodeInstruction>() {

			@Override
			public void handleResultItem(NodeInstruction resultItem) throws IOException {
				results.add(resultItem);
			}

		});

		// THEN
		then(results).as("Results for ts 1 and 2 returned").hasSize(2);
		int i = 0;
		for ( NodeInstruction instr : results ) {
			NodeInstruction expected = addedInstructions.get(i++);
			then(instr.getId()).as("Instruction %d", i).isEqualTo(expected.getId());
			validate(expected, instr);
		}
	}

	@Test
	public void findPending() {
		findByNodeId();

		// store a second for a different state, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.getInstruction().setStatusDate(Instant.now());
		datum.getInstruction().setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.getInstruction().setState(InstructionState.Executing);
		datum.getInstruction().setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		then(instr2Id).isNotNull();
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 2 result")
			.hasSize(2)
			.satisfies(r -> {
				then(r)
					.asInstanceOf(iterable(NodeInstruction.class))
					.contains(lastDatum, datum)
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(2L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(2, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void findPendingForNodes_stream() throws IOException {
		findByNodeId();

		// store a second for a different state, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.getInstruction().setStatusDate(Instant.now());
		datum.getInstruction().setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.getInstruction().setState(InstructionState.Executing);
		datum.getInstruction().setTopic("Test Topic");

		final Long instr2Id = dao.save(datum);
		then(instr2Id).isNotNull();
		datum.setId(instr2Id);

		// WHEN
		SimpleInstructionFilter f = new SimpleInstructionFilter();
		f.setNodeId(TEST_NODE_ID);
		f.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		List<NodeInstruction> results = new ArrayList<>(2);
		dao.findFilteredStream(f, new AbstractFilteredResultsProcessor<NodeInstruction>() {

			@Override
			public void handleResultItem(NodeInstruction resultItem) throws IOException {
				results.add(resultItem);
			}

		});

		// THEN
		assertThat("Results for node 2 and 3 returned", results, hasSize(2));
		Set<Long> expectedIds = new HashSet<>(Arrays.asList(lastDatum.getId(), datum.getId()));
		for ( EntityMatch one : results ) {
			expectedIds.remove(one.getId());
		}
		assertThat("Two expected results returned", expectedIds, hasSize(0));
	}

	@Test
	public void getWithoutParameters() {
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.getInstruction().setStatusDate(Instant.now());
		datum.getInstruction().setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.getInstruction().setState(InstructionState.Executing);
		datum.getInstruction().setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		then(instr2Id).isNotNull();
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		// @formatter:off
		then(matches)
			.as("Result returned")
			.isNotNull()
			.as("Returns 1 result")
			.hasSize(1)
			.satisfies(r -> {
				then(r)
					.element(0, type(NodeInstruction.class))
					.isEqualTo(datum)
					.extracting(ni -> ni.getInstruction().getParameters(), list(InstructionParameter.class))
					.as("Empty parameters")
					.isEmpty()
					;
			})
			.asInstanceOf(type(FilterResults.class))
			.as("Total results provided")
			.returns(1L, from(FilterResults<?, ?>::getTotalResults))
			.as("Returned results provided")
			.returns(1, from(FilterResults<?,?>::getReturnedResultCount))
			;
		// @formatter:on
	}

	@Test
	public void purgeCompletedInstructionsNone() {
		long result = dao.purgeCompletedInstructions(Instant.now());
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsNoMatch() {
		storeNew();
		long result = dao.purgeCompletedInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsMatch() {
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Completed);
		dao.save(lastDatum);
		long result = dao.purgeCompletedInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isOne();
		NodeInstruction instr = dao.get(lastDatum.getId());
		then(instr).as("Purged instruction is not found").isNull();
	}

	@Test
	public void purgeCompletedInstructionsMatchMultiple() {
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Completed);
		dao.save(lastDatum);
		storeNew(); // Queued state, should NOT be deleted
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Declined);
		dao.save(lastDatum);
		long result = dao.purgeCompletedInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isEqualTo(2L);
		NodeInstruction instr = dao.get(lastDatum.getId());
		then(instr).as("Purged instruction is not found").isNull();
	}

	@Test
	public void purgeIncompleteInstructionsNone() {
		long result = dao.purgeIncompleteInstructions(Instant.now());
		then(result).isZero();
	}

	@Test
	public void purgeIncompleteInstructionsQueued() {
		storeNew();
		long result = dao.purgeIncompleteInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isOne();
		then(dao.get(lastDatum.getId())).as("Purged instruction is not found").isNull();
	}

	@Test
	public void purgeIncompleteInstructionsMatch() {
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Received);
		dao.save(lastDatum);
		long result = dao.purgeIncompleteInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isOne();
		then(dao.get(lastDatum.getId())).as("Purged instruction is not found").isNull();
	}

	@Test
	public void purgeIncompleteInstructionsMatchMultiple() {
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Received);
		dao.save(lastDatum);
		storeNew(); // Queued state, should also be deleted
		storeNew();
		lastDatum.getInstruction().setState(InstructionState.Executing);
		dao.save(lastDatum);
		long result = dao.purgeIncompleteInstructions(
				lastDatum.getInstruction().getInstructionDate().plus(1, ChronoUnit.DAYS));
		then(result).isEqualTo(3L);
		then(dao.get(lastDatum.getId())).as("Purged instruction is not found").isNull();
	}

	@Test
	public void updateStateWithResultParameters() {
		// given
		storeNew();

		// when
		Map<String, Object> resultParams = Collections.singletonMap("foo", (Object) "bar");
		boolean updated = dao.updateNodeInstructionState(lastDatum.getId(), lastDatum.getNodeId(),
				InstructionState.Completed, resultParams);

		assertThat("Updated", updated, equalTo(true));

		NodeInstruction datum = dao.get(lastDatum.getId());
		assertThat("State changed", datum.getInstruction().getState(),
				equalTo(InstructionState.Completed));
		assertThat("Result parameters saved", datum.getInstruction().getResultParameters(),
				equalTo(resultParams));
	}

	@Test
	public void updateCompareState() {
		// GIVEN
		storeNew();

		// WHEN
		try {
			Thread.sleep(50L); // sleep so status date changes
		} catch ( InterruptedException e ) {
			// ignore
		}
		Map<String, Object> resultParams = Collections.singletonMap("foo", (Object) "bar");
		boolean updated = dao.compareAndUpdateInstructionState(lastDatum.getId(), lastDatum.getNodeId(),
				InstructionState.Queued, InstructionState.Completed, resultParams);

		// THEN
		then(updated).as("Was updated").isTrue();

		NodeInstruction datum = dao.get(lastDatum.getId());

		// @formatter:off		
		then(datum.getInstruction())
			.as("State updated")
			.returns(InstructionState.Completed, Instruction::getState)
			.as("Result parameters saved")
			.returns(resultParams, Instruction::getResultParameters)
			;
		
		then(datum.getInstruction().getStatusDate())
			.as("Status date updated from change")
			.isAfter(lastDatum.getInstruction().getStatusDate())
			;
		// @formatter:on
	}

	@Test
	public void updateCompareStateDifferentExpectedState() {
		// GIVEN
		storeNew();

		// WHEN
		Map<String, Object> resultParams = Collections.singletonMap("foo", (Object) "bar");
		boolean updated = dao.compareAndUpdateInstructionState(lastDatum.getId(), lastDatum.getNodeId(),
				InstructionState.Executing, InstructionState.Completed, resultParams);

		// THEN
		then(updated).as("Was not updated").isFalse();

		NodeInstruction datum = dao.get(lastDatum.getId());

		// @formatter:off		
		then(datum.getInstruction())
			.as("State unchanged")
			.returns(InstructionState.Queued, Instruction::getState)
			.as("Status date unchanged")
			.returns(lastDatum.getInstruction().getStatusDate(), Instruction::getStatusDate)
			;
		
		// @formatter:on
	}

	@Test
	public void updateStaleStateNothing() {
		// given
		storeNew();

		// when
		long count = dao.updateStaleInstructionsState(InstructionState.Queuing, Instant.now(),
				InstructionState.Completed);

		// then
		assertThat("Update count", count, equalTo(0L));
	}

	@Test
	public void updateStaleStateNotOlder() {
		// given
		storeNew();

		Instant instrDate = lastDatum.getInstruction().getInstructionDate();

		// when
		long count = dao.updateStaleInstructionsState(InstructionState.Queued,
				instrDate.minus(1, ChronoUnit.HOURS), InstructionState.Completed);

		// then
		assertThat("Update count", count, equalTo(0L));
	}

	@Test
	public void updateStaleState() {
		// given
		storeNew();

		Instant instrDate = lastDatum.getInstruction().getInstructionDate();

		// when
		long count = dao.updateStaleInstructionsState(InstructionState.Queued,
				instrDate.plus(1, ChronoUnit.MINUTES), InstructionState.Completed);

		// then
		assertThat("Update count", count, equalTo(1L));

		NodeInstruction updated = dao.get(lastDatum.getId());
		assertThat("Updated state", updated.getInstruction().getState(),
				equalTo(InstructionState.Completed));
	}

	@Test
	public void updateStaleMulti() {
		// given
		Instant startTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final int instrCount = 5;
		List<NodeInstruction> instructions = new ArrayList<NodeInstruction>(instrCount);
		for ( int i = 0; i < instrCount; i++ ) {
			instructions.add(storeNewInstruction(TEST_NODE_ID, startTime.plus(i, ChronoUnit.MINUTES)));
		}

		// when
		final int numMinutes = 3;
		long count = dao.updateStaleInstructionsState(InstructionState.Queued,
				startTime.plus(numMinutes, ChronoUnit.MINUTES), InstructionState.Completed);

		// then
		assertThat("Update count", count, equalTo((long) numMinutes));

		for ( int i = 0; i < instrCount; i++ ) {
			NodeInstruction updated = dao.get(instructions.get(i).getId());
			assertThat("Updated state " + i, updated.getInstruction().getState(),
					equalTo(i < numMinutes ? InstructionState.Completed : InstructionState.Queued));
		}
	}

	@Test
	public void transitionExpired() {
		// GIVEN
		final Instant expirationDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final List<NodeInstruction> instructions = new ArrayList<>();
		for ( int i = 0; i < 20; i++ ) {
			NodeInstruction instr = new NodeInstruction("test", Instant.now(), TEST_NODE_ID,
					expirationDate.minus(10 - i, ChronoUnit.MINUTES));
			instr.getInstruction()
					.setState(InstructionState.values()[i % InstructionState.values().length]);
			Long id = dao.save(instr);
			instr.setId(id);
			instructions.add(instr);
		}

		List<Map<String, Object>> rows = allNodeInstructionRows();
		then(rows).as("All instructions have been persisted").hasSize(instructions.size());

		// WHEN
		var criteria = new NodeInstruction();
		criteria.getInstruction().setExpirationDate(expirationDate);
		criteria.getInstruction().setState(InstructionState.Declined);
		criteria.getInstruction().setResultParameters(Map.of("message", randomString()));
		int result = dao.transitionExpiredInstructions(criteria);

		// THEN
		Set<InstructionState> statesToTransition = EnumSet
				.complementOf(EnumSet.of(InstructionState.Completed, InstructionState.Declined));
		List<NodeInstruction> expectedToTransition = instructions.stream()
				.filter(instr -> instr.getInstruction().getExpirationDate().isBefore(expirationDate)
						&& statesToTransition.contains(instr.getInstruction().getState()))
				.toList();
		Set<Long> expectedToTransitionIds = expectedToTransition.stream().map(NodeInstruction::getId)
				.collect(toUnmodifiableSet());

		// @formatter:off
		then(result)
			.as("Expired rows updated")
			.isEqualTo(expectedToTransition.size())
			;
		
		rows = allNodeInstructionRows();
		then(rows)
			.as("All instructions have been persisted")
			.hasSize(instructions.size())
			;
		
		then(rows.stream().filter(row -> expectedToTransitionIds.contains(row.get("id"))))
			.allSatisfy(row -> {
				then(row)
					.as("State transitioned to given state")
					.containsEntry("deliver_state", criteria.getInstruction().getState().name())
					.hasEntrySatisfying("jresult_params", json -> {
						then(JsonUtils.getStringMap(json.toString()))
							.isEqualTo(criteria.getInstruction().getResultParameters())
							;
					})
					;
			})
			.map(row -> (Long)row.get("id"))
			.as("Expected instructions that expired have been transitioned")
			.containsExactly(expectedToTransition.stream().map(e -> e.getId()).toArray(Long[]::new))
			;
		
		Map<Long, NodeInstruction> instructionsById = instructions.stream().collect(toMap(NodeInstruction::getId, Function.identity()));
		then(rows.stream().filter(row -> !expectedToTransitionIds.contains(row.get("id"))))
			.allSatisfy(row -> {
				Long id = (Long)row.get("id");
				NodeInstruction instr = instructionsById.get(id);
				then(row)
					.as("State not changed for non-expired entity")
					.containsEntry("deliver_state", instr.getInstruction().getState().name())
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void transitionExpired_mergeResultParameters() {
		// GIVEN
		final Instant expirationDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final NodeInstruction instruction = storeNewInstruction(TEST_NODE_ID, Instant.now(),
				Map.of("foo", "bar"), instr -> {
					instr.getInstruction().setState(InstructionState.Queued);
					instr.getInstruction().setExpirationDate(expirationDate.minusSeconds(1));
				});

		// WHEN
		var criteria = new NodeInstruction();
		criteria.getInstruction().setExpirationDate(expirationDate);
		criteria.getInstruction().setState(InstructionState.Declined);
		criteria.getInstruction().setResultParameters(Map.of("message", randomString()));
		int result = dao.transitionExpiredInstructions(criteria);

		// THEN
		// @formatter:off
		then(result)
			.as("Expired rows updated")
			.isOne()
			;
		
		List<Map<String, Object>> rows = allNodeInstructionRows();
		then(rows)
			.as("All instructions are persisted")
			.hasSize(1)
			;
		
		then(rows).element(0, map(String.class, Object.class))
			.as("State transitioned to given state")
			.containsEntry("deliver_state", criteria.getInstruction().getState().name())
			.hasEntrySatisfying("jresult_params", json -> {
				var merged = new LinkedHashMap<>(instruction.getInstruction().getResultParameters());
				merged.putAll(criteria.getInstruction().getResultParameters());
				then(JsonUtils.getStringMap(json.toString()))
					.as("Result parameters is merged from original and update properties")
					.isEqualTo(merged)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void updateForDateRange() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		setupTestUser(userId);
		setupTestUserNode(userId, TEST_NODE_ID);

		final Instant ts1 = Instant.now().truncatedTo(MINUTES).minus(1, HOURS);
		final NodeInstruction ni1 = storeNewInstruction(TEST_NODE_ID, ts1);
		final Instant ts2 = Instant.now().truncatedTo(MINUTES);
		final NodeInstruction ni2 = storeNewInstruction(TEST_NODE_ID, ts2);
		final Instant ts3 = Instant.now().truncatedTo(MINUTES).plus(1, HOURS);
		storeNewInstruction(TEST_NODE_ID, ts3, null, instr -> {
			instr.getInstruction().setExpirationDate(Instant.now().truncatedTo(HOURS).plus(1, HOURS));
		});

		// WHEN
		SimpleInstructionFilter f = new SimpleInstructionFilter();
		f.setNodeId(TEST_NODE_ID);
		f.setStartDate(ts1);
		f.setEndDate(ts3);
		Collection<Long> results = dao.updateNodeInstructionsState(userId, f, InstructionState.Declined);

		// THEN
		then(results).as("Results for ts 1 and 2 returned").contains(ni1.getId(), ni2.getId());

		List<Map<String, Object>> rows = allTableData(log, jdbcTemplate, "solarnet.sn_node_instruction",
				"id");
		// @formatter:off
		then(rows)
			.as("Rows for each instruction exist")
			.hasSize(3)
			.filteredOn(row -> row.get("deliver_state").equals(InstructionState.Declined.name()))
			.extracting(row -> (Long)row.get("id"))
			.as("Instructions 1 and 2 updated to desired state because they match search criteria")
			.containsOnly(ni1.getId(), ni2.getId())
			;
		// @formatter:on
	}

	@Test
	public void updateForStates() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		setupTestUser(userId);
		setupTestUserNode(userId, TEST_NODE_ID);

		final Instant ts1 = Instant.now().truncatedTo(MINUTES).minus(1, HOURS);
		final NodeInstruction ni1 = storeNewInstruction(TEST_NODE_ID, ts1, null, instr -> {
			instr.getInstruction().setState(InstructionState.Executing);
		});
		final Instant ts2 = Instant.now().truncatedTo(MINUTES);
		storeNewInstruction(TEST_NODE_ID, ts2);
		final Instant ts3 = Instant.now().truncatedTo(MINUTES).plus(1, HOURS);
		final NodeInstruction ni3 = storeNewInstruction(TEST_NODE_ID, ts3, null, instr -> {
			instr.getInstruction().setState(InstructionState.Received);
		});

		// WHEN
		SimpleInstructionFilter f = new SimpleInstructionFilter();
		f.setNodeId(TEST_NODE_ID);
		f.setStateSet(EnumSet.of(InstructionState.Executing, InstructionState.Received));
		Collection<Long> results = dao.updateNodeInstructionsState(userId, f, InstructionState.Declined);

		// THEN
		then(results).as("Results for ts 1 and 3 returned").contains(ni1.getId(), ni3.getId());

		List<Map<String, Object>> rows = allTableData(log, jdbcTemplate, "solarnet.sn_node_instruction",
				"id");
		// @formatter:off
		then(rows)
			.as("Rows for each instruction exist")
			.hasSize(3)
			.filteredOn(row -> row.get("deliver_state").equals(InstructionState.Declined.name()))
			.extracting(row -> (Long)row.get("id"))
			.as("Instructions 1 and 2 updated to desired state because they match search criteria")
			.containsOnly(ni1.getId(), ni3.getId())
			;
		// @formatter:on
	}

}
