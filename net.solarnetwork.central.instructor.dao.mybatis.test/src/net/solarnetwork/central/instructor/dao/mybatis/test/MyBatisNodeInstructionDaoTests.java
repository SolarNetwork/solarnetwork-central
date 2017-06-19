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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.instructor.dao.mybatis.MyBatisNodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;

/**
 * Test cases for the {@link MyBatisNodeInstructionDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisNodeInstructionDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisNodeInstructionDao dao;

	private NodeInstruction lastDatum;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisNodeInstructionDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastDatum = null;
		setupTestNode();
	}

	@Test
	public void storeNew() {
		NodeInstruction datum = new NodeInstruction();
		datum.setCreated(new DateTime());
		datum.setInstructionDate(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Queued);
		datum.setTopic("Test Topic");

		datum.addParameter("Test param 1", "Test value 1");
		datum.addParameter("Test param 2", "Test value 2");

		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(NodeInstruction src, NodeInstruction entity) {
		assertNotNull("NodeInstruction should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
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
	public void update() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.setState(InstructionState.Declined);
		Long newId = dao.store(datum);
		assertEquals(datum.getId(), newId);
		NodeInstruction datum2 = dao.get(datum.getId());
		validate(datum, datum2);
	}

	@Test
	public void updateWithResultParameters() {
		storeNew();
		NodeInstruction datum = dao.get(lastDatum.getId());
		datum.setState(InstructionState.Completed);
		datum.setResultParameters(Collections.singletonMap("foo", (Object) "bar"));
		Long newId = dao.store(datum);
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
		datum.setCreated(new DateTime());
		datum.setInstructionDate(new DateTime());
		datum.setNodeId(node2Id);
		datum.setState(InstructionState.Queued);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.store(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(Integer.valueOf(1), matches.getReturnedResultCount());
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
	public void findActive() {
		findByNodeId();

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(Integer.valueOf(1), matches.getReturnedResultCount());
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
	public void findPending() {
		findByNodeId();

		// store a second for a different state, to make sure filter working
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(new DateTime());
		datum.setInstructionDate(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Executing);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.store(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		filter.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		FilterResults<EntityMatch> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(2L), matches.getTotalResults());
		assertEquals(Integer.valueOf(2), matches.getReturnedResultCount());
		assertNotNull(matches.getResults());

		Set<Long> expectedIds = new HashSet<Long>(Arrays.asList(lastDatum.getId(), datum.getId()));
		for ( EntityMatch one : matches.getResults() ) {
			expectedIds.remove(one.getId());
		}
		assertEquals("Two results returned", 0, expectedIds.size());
	}

	@Test
	public void getWithoutParameters() {
		final NodeInstruction datum = new NodeInstruction();
		datum.setCreated(new DateTime());
		datum.setInstructionDate(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setState(InstructionState.Executing);
		datum.setTopic("Test Topic");
		final Long instr2Id = dao.store(datum);
		assertNotNull(instr2Id);
		datum.setId(instr2Id);

		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(TEST_NODE_ID);
		FilterResults<EntityMatch> matches = dao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Long.valueOf(1L), matches.getTotalResults());
		assertEquals(Integer.valueOf(1), matches.getReturnedResultCount());
		assertNotNull(matches.getResults());

		EntityMatch match = matches.getResults().iterator().next();
		assertTrue(match instanceof NodeInstruction);
		NodeInstruction ni = (NodeInstruction) match;
		assertNotNull(ni.getParameters());
		assertEquals("Empty parameters", 0, ni.getParameters().size());
	}

	@Test
	public void purgeCompletedInstructionsNone() {
		long result = dao.purgeCompletedInstructions(new DateTime());
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsNoMatch() {
		storeNew();
		long result = dao.purgeCompletedInstructions(lastDatum.getInstructionDate().plusDays(1));
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsMatch() {
		storeNew();
		lastDatum.setState(InstructionState.Completed);
		dao.store(lastDatum);
		long result = dao.purgeCompletedInstructions(lastDatum.getInstructionDate().plusDays(1));
		assertEquals(1, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
	}

	@Test
	public void purgeCompletedInstructionsMatchMultiple() {
		storeNew();
		lastDatum.setState(InstructionState.Completed);
		dao.store(lastDatum);
		storeNew(); // Queued state, should NOT be deleted
		storeNew();
		lastDatum.setState(InstructionState.Declined);
		dao.store(lastDatum);
		long result = dao.purgeCompletedInstructions(lastDatum.getInstructionDate().plusDays(1));
		assertEquals(2, result);
		NodeInstruction instr = dao.get(lastDatum.getId());
		assertNull("Purged instruction is not found", instr);
	}

}
