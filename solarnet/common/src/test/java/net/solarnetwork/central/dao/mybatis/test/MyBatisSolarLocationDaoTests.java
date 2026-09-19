/* ==================================================================
 * MyBatisSolarLocationDaoTests.java - Nov 10, 2014 9:05:24 AM
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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisSolarLocationDao} class.
 * 
 * @author matt
 * @version 2.1
 */
public class MyBatisSolarLocationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisSolarLocationDao solarLocationDao;

	private SolarLocation location = null;

	@BeforeEach
	public void setUp() throws Exception {
		solarLocationDao = new MyBatisSolarLocationDao();
		solarLocationDao.setSqlSessionFactory(getSqlSessionFactory());
	}

	@Test
	public void storeNew() {
		SolarLocation loc = new SolarLocation();
		loc.setCountry("NZ");
		loc.setCreated(Instant.now());
		loc.setLatitude(new BigDecimal("1.2"));
		loc.setLocality("locality");
		loc.setLongitude(new BigDecimal("1.1"));
		loc.setElevation(new BigDecimal("1053"));
		loc.setName("test location");
		loc.setPostalCode("6011");
		loc.setRegion("region");
		loc.setStateOrProvince("state");
		loc.setStreet("street");
		loc.setTimeZoneId("UTC");
		Long id = solarLocationDao.save(loc);
		then(id).isNotNull();
		loc.setId(id);
		location = loc;
	}

	private void validatePublic(SolarLocation src, SolarLocation entity) {
		then(entity).as("SolarLocation should exist").isNotNull().satisfies(e -> {
			then(e.getCreated()).as("Created date should be set").isNotNull();
		}).returns(src.getCountry(), from(SolarLocation::getCountry))
				.returns(src.getLocality(), from(SolarLocation::getLocality))
				.returns(src.getPostalCode(), from(SolarLocation::getPostalCode))
				.returns(src.getRegion(), from(SolarLocation::getRegion))
				.returns(src.getStateOrProvince(), from(SolarLocation::getStateOrProvince))
				.returns(src.getTimeZoneId(), from(SolarLocation::getTimeZoneId));
	}

	private void validate(SolarLocation src, SolarLocation entity) {
		validatePublic(src, entity);
		if ( src.getLatitude() != null ) {
			then(entity.getLatitude()).isEqualByComparingTo(src.getLatitude());
		}
		if ( src.getLongitude() != null ) {
			then(entity.getLongitude()).isEqualByComparingTo(src.getLongitude());
		}
		if ( src.getElevation() != null ) {
			then(entity.getElevation()).isEqualByComparingTo(src.getElevation());
		}
		then(entity).returns(src.getStreet(), from(SolarLocation::getStreet));
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		SolarLocation loc = solarLocationDao.get(location.getId());
		validate(location, loc);
	}

	@Test
	public void update() {
		storeNew();
		SolarLocation loc = solarLocationDao.get(location.getId());
		loc.setName("new name");
		Long newId = solarLocationDao.save(loc);
		then(newId).isEqualTo(loc.getId());
		SolarLocation loc2 = solarLocationDao.get(location.getId());
		validate(loc, loc2);
	}

	@Test
	public void findByTimeZoneNoResults() {
		storeNew();
		// should not find this location, because properties other than country and time zone are set
		SolarLocation loc = solarLocationDao.getSolarLocationForTimeZone(location.getCountry(),
				location.getTimeZoneId());
		then(loc).isNull();
	}

	@Test
	public void findByTimeZone() {
		SolarLocation loc = new SolarLocation();
		loc.setCreated(Instant.now());
		loc.setName("NZ - Pacific/Auckland");
		loc.setCountry("NZ");
		loc.setTimeZoneId("Pacific/Auckland");
		Long id = solarLocationDao.save(loc);
		then(id).isNotNull();
		loc.setId(id);
		// should not find this location, because properties other than country and time zone are set
		SolarLocation found = solarLocationDao.getSolarLocationForTimeZone(loc.getCountry(),
				loc.getTimeZoneId());
		then(loc).isNotNull();
		validate(loc, found);
	}

	@Test
	public void findFilteredNoMatch() {
		SolarLocation filter = new SolarLocation();
		filter.setName("does-not-exist");
		FilterResults<LocationMatch, Long> results = solarLocationDao.findFiltered(filter, null, null,
				null);
		then(results).asInstanceOf(type(FilterResults.class))
				.returns(0, from(FilterResults<?, ?>::getReturnedResultCount))
				.returns(0L, from(FilterResults<?, ?>::getTotalResults));
	}

	@Test
	public void findFilteredMatch() {
		setupTestLocation();
		SolarLocation filter = new SolarLocation();
		filter.setRegion(TEST_LOC_REGION);
		FilterResults<LocationMatch, Long> results = solarLocationDao.findFiltered(filter, null, null,
				null);
		then(results).asInstanceOf(type(FilterResults.class))
				.returns(1, from(FilterResults<?, ?>::getReturnedResultCount))
				.returns(1L, from(FilterResults<?, ?>::getTotalResults));
		then(results).element(0).returns(TEST_LOC_ID, from(LocationMatch::getId));
	}

	@Test
	public void findFilteredMultipleOrder() {
		setupTestLocation();
		SolarLocation loc2 = new SolarLocation();
		loc2.setCreated(Instant.now());
		loc2.setRegion(TEST_LOC_REGION);
		loc2.setCountry(TEST_LOC_COUNTRY);
		loc2.setPostalCode(TEST_LOC_POSTAL_CODE);
		loc2.setTimeZoneId(TEST_TZ);
		Long id2 = solarLocationDao.save(loc2);

		SolarLocation loc3 = new SolarLocation();
		loc3.setCreated(Instant.now());
		loc3.setRegion(TEST_LOC_REGION);
		loc3.setCountry(TEST_LOC_COUNTRY);
		loc3.setPostalCode(TEST_LOC_POSTAL_CODE);
		loc3.setTimeZoneId(TEST_TZ);
		Long id3 = solarLocationDao.save(loc3);

		SolarLocation filter = new SolarLocation();
		filter.setRegion(TEST_LOC_REGION);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);
		filter.setTimeZoneId(TEST_TZ);
		FilterResults<LocationMatch, Long> results = solarLocationDao.findFiltered(filter, null, null,
				null);
		then(results).asInstanceOf(type(FilterResults.class))
				.returns(3, from(FilterResults<?, ?>::getReturnedResultCount))
				.returns(3L, from(FilterResults<?, ?>::getTotalResults));

		then(results).extracting(LocationMatch::getId).as("Results should be ordered by name")
				.containsExactly(TEST_LOC_ID, id2, id3);
	}

	@Test
	public void findExactLocationNoMatch() {
		setupTestLocation();
		SolarLocation criteria = new SolarLocation();
		criteria.setPostalCode(TEST_LOC_POSTAL_CODE);
		SolarLocation match = solarLocationDao.getSolarLocationForLocation(criteria);
		then(match).isNull();
	}

	@Test
	public void findExactLocationMatch() {
		setupTestLocation();
		SolarLocation criteria = solarLocationDao.get(TEST_LOC_ID);
		SolarLocation match = solarLocationDao.getSolarLocationForLocation(criteria);
		then(match).isNotNull().isEqualTo(criteria);
	}

	@Test
	public void findForNode_noMatch() {
		// GIVEN
		setupTestNode();

		// WHEN
		SolarLocation loc = solarLocationDao.getSolarLocationForNode(12345L);

		// THEN
		assertThat("No location found for non-existing node", loc, is(nullValue()));
	}

	@Test
	public void findForNode_match() {
		// GIVEN
		setupTestNode();

		// WHEN
		SolarLocation loc = solarLocationDao.getSolarLocationForNode(TEST_NODE_ID);

		// THEN
		SolarLocation expected = solarLocationDao.get(TEST_LOC_ID);
		assertThat("Location found for node", loc, is(expected));
	}

}
