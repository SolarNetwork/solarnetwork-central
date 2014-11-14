/* ==================================================================
 * MyBatisPriceLocationDaoTests.java - Nov 10, 2014 1:36:42 PM
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

package net.solarnetwork.central.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.solarnetwork.central.dao.mybatis.MyBatisPriceLocationDao;
import net.solarnetwork.central.dao.mybatis.MyBatisPriceSourceDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.domain.PriceSource;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.support.PriceLocationFilter;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.central.support.SourceLocationFilter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisPriceLocationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisPriceLocationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisPriceLocationDao dao;
	private MyBatisPriceSourceDao priceSourceDao;
	private MyBatisSolarLocationDao solarLocationDao;

	private SolarLocation location = null;
	private PriceLocation priceLocation = null;
	private PriceSource priceSource = null;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisPriceLocationDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		priceSourceDao = new MyBatisPriceSourceDao();
		priceSourceDao.setSqlSessionFactory(getSqlSessionFactory());

		solarLocationDao = new MyBatisSolarLocationDao();
		solarLocationDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestLocation();
		setupTestPriceLocation();
		this.location = solarLocationDao.get(TEST_LOC_ID);
		this.priceSource = priceSourceDao.get(TEST_PRICE_SOURCE_ID);
	}

	private PriceLocation newPriceLocationInstance() {
		PriceLocation loc = new PriceLocation();
		loc.setCreated(new DateTime());
		loc.setName("A test location");
		loc.setSource(this.priceSource);
		loc.setLocation(this.location);
		loc.setSourceData("Test source data");
		loc.setCurrency(TEST_CURRENCY);
		loc.setUnit("MWh");
		return loc;
	}

	@Test
	public void storeNew() {
		PriceLocation loc = newPriceLocationInstance();
		Long id = dao.store(loc);
		assertNotNull(id);
		loc.setId(id);
		priceLocation = loc;
	}

	private void validate(PriceLocation src, PriceLocation entity) {
		assertNotNull("UserNodeConfirmation should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getName(), entity.getName());
		assertEquals(src.getSource(), entity.getSource());
		assertEquals(src.getSourceData(), entity.getSourceData());
		assertEquals(src.getCurrency(), entity.getCurrency());
		assertEquals(src.getUnit(), entity.getUnit());
		assertEquals(src.getLocation(), entity.getLocation());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		PriceLocation loc = dao.get(priceLocation.getId());
		validate(priceLocation, loc);
	}

	@Test
	public void update() {
		storeNew();
		PriceLocation loc = dao.get(priceLocation.getId());
		loc.setSourceData("new data");
		Long newId = dao.store(loc);
		assertEquals(loc.getId(), newId);
		PriceLocation loc2 = dao.get(priceLocation.getId());
		validate(loc, loc2);
	}

	@Test
	public void findByName() {
		PriceSource source = new PriceSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = priceSourceDao.get(priceSourceDao.store(source));

		PriceLocation loc = newPriceLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		PriceLocation found = dao.getPriceLocationForName(source.getName(), loc.getName());
		assertNotNull(found);
		validate(loc, found);
	}

	@Test
	public void findBySourceFilter() {
		PriceSource source = new PriceSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = priceSourceDao.get(priceSourceDao.store(source));

		PriceLocation loc = newPriceLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		SourceLocationFilter filter = new SourceLocationFilter(source.getName(), loc.getLocationName());
		FilterResults<SourceLocationMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(1), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		SourceLocationMatch match = results.getResults().iterator().next();
		assertEquals(loc.getId(), match.getId());
		assertEquals(this.location.getId(), match.getLocationId());
		assertEquals(source.getName(), match.getSourceName());
	}

	@Test
	public void findByCurrencySortedDefault() {
		PriceSource source = new PriceSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = priceSourceDao.get(priceSourceDao.store(source));

		PriceLocation loc = newPriceLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		PriceLocationFilter filter = new PriceLocationFilter();
		filter.setCurrency(TEST_CURRENCY);
		FilterResults<SourceLocationMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(2), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		Iterator<SourceLocationMatch> itr = results.getResults().iterator();

		SourceLocationMatch match = itr.next();
		assertEquals(loc.getId(), match.getId());
		assertEquals(this.location.getId(), match.getLocationId());
		assertEquals(source.getName(), match.getSourceName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());

		match = itr.next();
		assertEquals(TEST_PRICE_SOURCE_NAME, match.getSourceName());
		assertEquals(TEST_LOC_NAME, match.getLocationName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());
	}

	@Test
	public void findByCurrencySorted() {
		PriceSource source = new PriceSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = priceSourceDao.get(priceSourceDao.store(source));

		try {
			Thread.sleep(100);
		} catch ( InterruptedException e ) {
			// ignore
		}

		PriceLocation loc = newPriceLocationInstance();
		loc.setCreated(new DateTime());
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		PriceLocationFilter filter = new PriceLocationFilter();
		filter.setCurrency(TEST_CURRENCY);
		List<SortDescriptor> sorts = new ArrayList<SortDescriptor>(1);
		sorts.add(new SimpleSortDescriptor("created", false));
		FilterResults<SourceLocationMatch> results = dao.findFiltered(filter, sorts, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(2), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		Iterator<SourceLocationMatch> itr = results.getResults().iterator();

		SourceLocationMatch match;
		match = itr.next();
		assertEquals(TEST_PRICE_SOURCE_NAME, match.getSourceName());
		assertEquals(TEST_LOC_NAME, match.getLocationName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());

		match = itr.next();
		assertEquals(loc.getId(), match.getId());
		assertEquals(this.location.getId(), match.getLocationId());
		assertEquals(source.getName(), match.getSourceName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());
	}

	@Test
	public void findByCurrencySortedMulti() {
		PriceSource source = new PriceSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = priceSourceDao.get(priceSourceDao.store(source));

		try {
			Thread.sleep(100);
		} catch ( InterruptedException e ) {
			// ignore
		}

		PriceLocation loc = newPriceLocationInstance();
		loc.setCreated(new DateTime());
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		PriceLocationFilter filter = new PriceLocationFilter();
		filter.setCurrency(TEST_CURRENCY);
		List<SortDescriptor> sorts = new ArrayList<SortDescriptor>(1);
		sorts.add(new SimpleSortDescriptor("currency", false));
		sorts.add(new SimpleSortDescriptor("created", true));
		FilterResults<SourceLocationMatch> results = dao.findFiltered(filter, sorts, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(2), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		Iterator<SourceLocationMatch> itr = results.getResults().iterator();

		SourceLocationMatch match;

		match = itr.next();
		assertEquals(loc.getId(), match.getId());
		assertEquals(this.location.getId(), match.getLocationId());
		assertEquals(source.getName(), match.getSourceName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());

		match = itr.next();
		assertEquals(TEST_PRICE_SOURCE_NAME, match.getSourceName());
		assertEquals(TEST_LOC_NAME, match.getLocationName());
		assertTrue(match instanceof PriceLocation);
		assertEquals(TEST_CURRENCY, ((PriceLocation) match).getCurrency());
	}

}
