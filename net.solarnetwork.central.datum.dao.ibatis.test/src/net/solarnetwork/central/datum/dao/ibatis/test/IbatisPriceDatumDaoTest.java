/* ==================================================================
 * IbatisPriceDatumDaoTest.java - Sep 12, 2011 3:31:40 PM
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

import java.util.List;

import net.solarnetwork.central.datum.dao.ibatis.IbatisPriceDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.PriceDatum;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisPriceDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisPriceDatumDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private IbatisPriceDatumDao dao;
	
	private PriceDatum lastDatum;
	
	@Before
	public void setUp() throws Exception {
		lastDatum = null;
	}

	@Test
	public void storeNew() {
		PriceDatum datum = new PriceDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_PRICE_LOC_ID);
		datum.setPrice(1.0F);
		datum.setPosted(new DateTime());
		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(PriceDatum src, PriceDatum entity) {
		assertNotNull("PriceDatum should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getPrice(), entity.getPrice());
		assertEquals(src.getPosted(), entity.getPosted());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	PriceDatum datum = dao.get(lastDatum.getId());
    	validate(lastDatum, datum);
	}

    @Test
    public void getMostRecent() {
    	storeNew();
    	
    	DatumQueryCommand criteria = new DatumQueryCommand();
    	criteria.setLocationId(TEST_PRICE_LOC_ID);
    	
    	List<PriceDatum> results = dao.getMostRecentDatum(criteria);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	assertEquals(lastDatum, results.get(0));
    	
    	PriceDatum datum2 = new PriceDatum();
    	datum2.setCreated(new DateTime().plusHours(1));
		datum2.setLocationId(TEST_PRICE_LOC_ID);
    	datum2.setPrice(1.2F);
    	Long id2 = dao.store(datum2);

    	results = dao.getMostRecentDatum(criteria);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	assertEquals(id2, results.get(0).getId());
    }

}
