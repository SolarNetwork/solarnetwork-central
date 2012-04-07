/* ==================================================================
 * IbatisPowerDatumDaoTest.java - Sep 12, 2011 12:31:31 PM
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

package net.solarnetwork.central.datum.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.datum.dao.ibatis.IbatisPowerDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.PowerDatum;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisPowerDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisPowerDatumDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private IbatisPowerDatumDao dao;
	
	private PowerDatum lastDatum;
	
	@Before
	public void setUp() throws Exception {
		lastDatum = null;
	}

	@Test
	public void storeNew() {
		PowerDatum datum = new PowerDatum();
		datum.setBatteryAmpHours(1.3F);
		datum.setBatteryVolts(1.4F);
		datum.setCreated(new DateTime());
		datum.setKWattHoursToday(1.7F);
		datum.setLocationId(TEST_PRICE_LOC_ID);
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(new DateTime());
		
		// note we are setting legacy amp/volt properties here
		datum.setPvAmps(1.8F);
		datum.setPvVolts(1.9F);
		
		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(PowerDatum src, PowerDatum entity) {
		assertNotNull("PowerDatum should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getBatteryAmpHours(), entity.getBatteryAmpHours());
		assertEquals(src.getBatteryVolts(), entity.getBatteryVolts());
		assertEquals(src.getWattHourReading(), entity.getWattHourReading());
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getPosted(), entity.getPosted());
		assertEquals(src.getWatts(), entity.getWatts());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	PowerDatum datum = dao.get(lastDatum.getId());
    	validate(lastDatum, datum);
	}

    @Test
    public void getMostRecent() {
    	storeNew();
    	
    	DatumQueryCommand criteria = new DatumQueryCommand();
    	criteria.setNodeId(TEST_NODE_ID);
    	
    	List<PowerDatum> results = dao.getMostRecentDatum(criteria);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	assertEquals(lastDatum, results.get(0));
    	
    	PowerDatum datum2 = new PowerDatum();
    	datum2.setCreated(new DateTime().plusHours(1));
    	datum2.setNodeId(TEST_NODE_ID);
    	datum2.setSourceId(lastDatum.getSourceId());
    	datum2.setPvAmps(1.2F);
    	Long id2 = dao.store(datum2);

    	results = dao.getMostRecentDatum(criteria);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	assertEquals(id2, results.get(0).getId());
    	
    	PowerDatum datum3 = new PowerDatum();
    	datum3.setNodeId(TEST_NODE_ID);
    	datum3.setSourceId("/test/source/2");
    	datum3.setPvAmps(1.3F);
    	Long id3 = dao.store(datum3);
    	
    	results = dao.getMostRecentDatum(criteria);
    	assertNotNull(results);
    	assertEquals(2, results.size());
    	Set<Long> ids = new LinkedHashSet<Long>();
    	for ( PowerDatum d : results ) {
    		ids.add(d.getId());
    	}
    	assertTrue(ids.contains(id2));
    	assertTrue(ids.contains(id3));
    }

}
