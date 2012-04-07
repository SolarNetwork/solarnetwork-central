/* ==================================================================
 * IbatisOutboundMailDaoTest.java - Jun 18, 2011 8:26:29 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This outboundMail is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This outboundMail is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this outboundMail; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao.ibatis.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.OutboundMailDao;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.OutboundMail;
import net.solarnetwork.central.dras.support.SimpleOutboundMailFilter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link OutboundMailDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisOutboundMailDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private OutboundMailDao outboundMailDao;
	
	private Long lastOutboundMailId;
	
	@Before
	public void setup() {
		lastOutboundMailId = null;
	}
	
	@Test
	public void getOutboundMailById() {
		setupTestOutboundMail(TEST_OUTBOUND_MAIL_ID, TEST_PROGRAM_NAME);
		OutboundMail outboundMail = outboundMailDao.get(TEST_OUTBOUND_MAIL_ID);
		assertNotNull(outboundMail);
		assertNotNull(outboundMail.getId());
		assertEquals(TEST_OUTBOUND_MAIL_ID, outboundMail.getId());
		assertEquals(TEST_OUTBOUND_MAIL_MESSAGE_ID, outboundMail.getMessageId());
	}
	
	@Test
	public void getNonExistingOutboundMailById() {
		OutboundMail outboundMail = outboundMailDao.get(-99999L);
		assertNull(outboundMail);
	}
	
	private void validateOutboundMail(OutboundMail outboundMail, OutboundMail entity) {
		assertNotNull("OutboundMail should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(outboundMail.getCreator(), entity.getCreator());
		assertEquals(outboundMail.getId(), entity.getId());
		assertEquals(outboundMail.getMessageBody(), entity.getMessageBody());
		assertEquals(outboundMail.getSubject(), entity.getSubject());
		assertArrayEquals(outboundMail.getTo(), entity.getTo());
	}
	
	@Test
	public void insertOutboundMail() {
		OutboundMail outboundMail = new OutboundMail();
		outboundMail.setCreator(TEST_USER_ID);
		outboundMail.setMessageBody("My test message gobble.");
		outboundMail.setMessageId(TEST_OUTBOUND_MAIL_MESSAGE_ID);
		outboundMail.setSubject("Test subject bauble.");
		outboundMail.setToAddress("unittest@localhost.localdomain");
		
		logger.debug("Inserting new OutboundMail: " +outboundMail);
		
		Long id = outboundMailDao.store(outboundMail);
		assertNotNull(id);
		
		OutboundMail entity = outboundMailDao.get(id);
		validateOutboundMail(outboundMail, entity);
		
		lastOutboundMailId = id;
	}

	@Test
	public void updateOutboundMail() {
		insertOutboundMail();
		
		OutboundMail outboundMail = outboundMailDao.get(lastOutboundMailId);
		outboundMail.setMessageBody("foo.update");
		
		try {
			outboundMailDao.store(outboundMail);
			fail("Should have not allowed updating OutboundMail entity.");
		} catch ( UnsupportedOperationException e) {
			// expected!
		}
	}

	@Test
	public void findFilteredMessage() {
		insertOutboundMail();
		
		SimpleOutboundMailFilter filter = new SimpleOutboundMailFilter();
		filter.setQuery("nadda");
		FilterResults<Match> results = outboundMailDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// test search on message body
		filter.setQuery("gobble");
		results = outboundMailDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastOutboundMailId, results.getResults().iterator().next().getId());

		// test search on subject
		filter.setQuery("bauble");
		results = outboundMailDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastOutboundMailId, results.getResults().iterator().next().getId());
}

}
