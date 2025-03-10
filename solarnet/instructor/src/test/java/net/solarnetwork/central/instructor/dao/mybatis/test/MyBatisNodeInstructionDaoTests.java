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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.instructor.dao.mybatis.MyBatisNodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link MyBatisNodeInstructionDao} class.
 * 
 * @author matt
 * @version 2.4
 */
public class MyBatisNodeInstructionDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisNodeInstructionDao dao;

	private NodeInstruction lastDatum;
	private List<NodeInstruction> addedInstructions;

	@Before
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

		NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.setStatusDate(date);
		datum.setInstructionDate(date);
		datum.setNodeId(nodeId);
		datum.setState(InstructionState.Queued);
		datum.setTopic("Test Topic");
		datum.setResultParameters(resultParams);

		datum.addParameter("Test param 1", "Test value 1");
		datum.addParameter("Test param 2", "Test value 2");

		Long id = dao.save(datum);
		assertNotNull(id);
		datum.setId(id);
		addedInstructions.add(datum);
		return datum;
	}

	@Test
	public void storeNew() {
		lastDatum = storeNewInstruction(TEST_NODE_ID);
	}

	private void validate(NodeInstruction src, NodeInstruction entity) {
		assertNotNull("NodeInstruction should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getStatusDate(), entity.getStatusDate());
		assertEquals(src.getInstructionDate(), entity.getInstructionDate());
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getState(), entity.getState());
		assertEquals(src.getTopic(), entity.getTopic());
		assertEquals(src.getParameters(), entity.getParameters());
		assertEquals(src.getResultParameters(), entity.getResultParameters());
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
	public void update() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.setState(InstructionState.Declined);
		datum.setStatusDate(Instant.now());
		Long newId = dao.save(datum);
		assertEquals(datum.getId(), newId);
		NodeInstruction datum2 = dao.get(datum.getId());
		validate(datum, datum2);
	}

	@Test
	public void updateWithResultParameters() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.setState(InstructionState.Completed);
		datum.setStatusDate(Instant.now());
		datum.setResultParameters(Collections.singletonMap("foo", (Object) "bar"));
		Long newId = dao.save(datum);
		assertEquals(datum.getId(), newId);
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
		datum.setStatusDate(Instant.now());
		datum.setInstructionDate(Instant.now());
		datum.setNodeId(node2Id);
		datum.setState(InstructionState.Queued);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(1, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		int count = 0;
		EntityMatch m = null;
		for ( EntityMatch one : matches.getResults() ) {
			count++;
			m = one;
		}
		assertEquals(1, count);
		assertEquals(lastDatum.getId(), m.getId());
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
		assertNotNull(matches);
		assertEquals(Long.valueOf(2L), matches.getTotalResults());
		assertEquals(2, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		int count = 0;
		for ( EntityMatch one : matches.getResults() ) {
			assertThat(one.getId(), equalTo(instructionIds.get(count)));
			count++;
		}
		assertEquals(2, count);
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
		assertNotNull(matches);
		assertEquals(Long.valueOf(2L), matches.getTotalResults());
		assertEquals(2, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		int count = 0;
		for ( EntityMatch one : matches.getResults() ) {
			assertThat(one.getId(), equalTo(instructionIds[count]));
			count++;
		}
		assertEquals(2, count);
	}

	@Test
	public void findActive() {
		findByNodeId();

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(1, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		int count = 0;
		EntityMatch m = null;
		for ( EntityMatch one : matches.getResults() ) {
			count++;
			m = one;
		}
		assertEquals(1, count);
		assertEquals(lastDatum.getId(), m.getId());
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
		assertNotNull(matches);
		assertEquals(Long.valueOf(2L), matches.getTotalResults());
		assertEquals(2, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		int count = 0;
		for ( EntityMatch one : matches.getResults() ) {
			count++;
			assertThat(one.getId(), equalTo(addedInstructions.get(count).getId()));
		}
		assertEquals(2, count);
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
		final Instant ts1 = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS);
		storeNewInstruction(TEST_NODE_ID, ts1);
		final Instant ts2 = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		storeNewInstruction(TEST_NODE_ID, ts2);
		final Instant ts3 = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.HOURS);
		storeNewInstruction(TEST_NODE_ID, ts3);

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
		assertThat("Results for ts 1 and 2 returned", results, hasSize(2));
		int i = 0;
		for ( NodeInstruction instr : results ) {
			NodeInstruction expected = addedInstructions.get(i++);
			assertThat("Instruction " + i, instr.getId(), is(equalTo(expected.getId())));
		}
	}

	@Test
	public void findPending() {
		findByNodeId();

		// store a second for a different state, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.setStatusDate(Instant.now());
		datum.setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Executing);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(2L), matches.getTotalResults());
		assertEquals(2, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());

		Set<Long> expectedIds = new HashSet<Long>(Arrays.asList(lastDatum.getId(), datum.getId()));
		for ( EntityMatch one : matches.getResults() ) {
			expectedIds.remove(one.getId());
		}
		assertEquals("Two results returned", 0, expectedIds.size());
	}

	@Test
	public void findPendingForNodes_stream() throws IOException {
		findByNodeId();

		// store a second for a different state, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(Instant.now());
		datum.setStatusDate(Instant.now());
		datum.setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Executing);
		datum.setTopic("Test Topic");

		final Long instr2Id = dao.save(datum);
		assertNotNull(instr2Id);
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
		datum.setStatusDate(Instant.now());
		datum.setInstructionDate(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Executing);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.save(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch, Long> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(1, matches.getReturnedResultCount());
		assertNotNull(matches.getResults());

		EntityMatch match = matches.getResults().iterator().next();
		assertTrue(match instanceof NodeInstruction);
		NodeInstruction ni = (NodeInstruction) match;
		assertNotNull(ni.getParameters());
		assertEquals("Empty parameters", 0, ni.getParameters().size());
	}

	@Test
	public void purgeCompletedInstructionsNone() {
		long result = dao.purgeCompletedInstructions(Instant.now());
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsNoMatch() {
		storeNew();
		long result = dao
				.purgeCompletedInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsMatch() {
		storeNew();
		lastDatum.setState(InstructionState.Completed);
		dao.save(lastDatum);
		long result = dao
				.purgeCompletedInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(1, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
	}

	@Test
	public void purgeCompletedInstructionsMatchMultiple() {
		storeNew();
		lastDatum.setState(InstructionState.Completed);
		dao.save(lastDatum);
		storeNew(); // Queued state, should NOT be deleted
		storeNew();
		lastDatum.setState(InstructionState.Declined);
		dao.save(lastDatum);
		long result = dao
				.purgeCompletedInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(2, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
	}

	@Test
	public void purgeIncompleteInstructionsNone() {
		long result = dao.purgeIncompleteInstructions(Instant.now());
		assertEquals(0, result);
	}

	@Test
	public void purgeIncompleteInstructionsQueued() {
		storeNew();
		long result = dao
				.purgeIncompleteInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(1, result);
		assertNull("Purged instruction is not found", dao.get(lastDatum.getId()));
	}

	@Test
	public void purgeIncompleteInstructionsMatch() {
		storeNew();
		lastDatum.setState(InstructionState.Received);
		dao.save(lastDatum);
		long result = dao
				.purgeIncompleteInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(1, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
	}

	@Test
	public void purgeIncompleteInstructionsMatchMultiple() {
		storeNew();
		lastDatum.setState(InstructionState.Received);
		dao.save(lastDatum);
		storeNew(); // Queued state, should also be deleted
		storeNew();
		lastDatum.setState(InstructionState.Executing);
		dao.save(lastDatum);
		long result = dao
				.purgeIncompleteInstructions(lastDatum.getInstructionDate().plus(1, ChronoUnit.DAYS));
		assertEquals(3, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
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
		assertThat("State changed", datum.getState(), equalTo(InstructionState.Completed));
		assertThat("Result parameters saved", datum.getResultParameters(), equalTo(resultParams));
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
		then(datum)
			.as("State updated")
			.returns(InstructionState.Completed, NodeInstruction::getState)
			.as("Result parameters saved")
			.returns(resultParams, NodeInstruction::getResultParameters)
			;
		
		then(datum.getStatusDate())
			.as("Status date updated from change")
			.isAfter(lastDatum.getStatusDate())
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
		then(datum)
			.as("State unchanged")
			.returns(InstructionState.Queued, NodeInstruction::getState)
			.as("Status date unchanged")
			.returns(lastDatum.getStatusDate(), NodeInstruction::getStatusDate)
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

		Instant instrDate = lastDatum.getInstructionDate();

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

		Instant instrDate = lastDatum.getInstructionDate();

		// when
		long count = dao.updateStaleInstructionsState(InstructionState.Queued,
				instrDate.plus(1, ChronoUnit.MINUTES), InstructionState.Completed);

		// then
		assertThat("Update count", count, equalTo(1L));

		NodeInstruction updated = dao.get(lastDatum.getId());
		assertThat("Updated state", updated.getState(), equalTo(InstructionState.Completed));
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
			assertThat("Updated state " + i, updated.getState(),
					equalTo(i < numMinutes ? InstructionState.Completed : InstructionState.Queued));
		}
	}
}
