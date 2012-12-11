/* ==================================================================
 * IbatisNodeInstructionDaoTest.java - Sep 30, 2011 9:22:25 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.instructor.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.instructor.dao.ibatis.IbatisNodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisNodeInstructionDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisNodeInstructionDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired
	private IbatisNodeInstructionDao dao;

	private NodeInstruction lastDatum;

	@Before
	public void setUp() throws Exception {
		lastDatum = null;
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

}
