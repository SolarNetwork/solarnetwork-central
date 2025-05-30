/* ==================================================================
 * DaoDataCollectorBizTests.java - Oct 23, 2011 2:49:59 PM
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
 */

package net.solarnetwork.central.in.biz.dao.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link DaoDataCollectorBiz} class.
 * 
 * @author matt
 * @version 3.3
 */
public class DaoDataCollectorBizTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private DaoDataCollectorBiz biz;

	private DatumEntityDao datumDao;
	private DatumStreamMetadataDao metaDao;
	private SolarLocationDao locationDao;
	private SolarNodeDao nodeDao;
	private DatumMetadataBiz datumMetadataBiz;

	@BeforeEach
	public void setup() {
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		locationDao = EasyMock.createMock(SolarLocationDao.class);
		nodeDao = EasyMock.createMock(SolarNodeDao.class);
		biz = new DaoDataCollectorBiz(datumDao);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setMetaDao(metaDao);
		biz.setSolarLocationDao(locationDao);
		biz.setSolarNodeDao(nodeDao);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(datumDao, metaDao, datumMetadataBiz, locationDao, nodeDao);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, metaDao, datumMetadataBiz, locationDao, nodeDao);
	}

	@Test
	public void findLocation() {
		// GIVEN
		SolarLocation filter = new SolarLocation();
		filter.setCountry("NZ");
		filter.setPostalCode("6011");

		SolarLocation l = new SolarLocation();
		BasicFilterResults<LocationMatch, Long> filterResults = new BasicFilterResults<>(singleton(l),
				1L, 0L, 1);
		expect(locationDao.findFiltered(eq(filter), isNull(), eq(0L), anyObject()))
				.andReturn(filterResults);

		// WHEN
		replayAll();
		List<LocationMatch> results = biz.findLocations(filter);

		// THEN
		assertThat(results, notNullValue());
		assertThat(results.size(), equalTo(1));

		LocationMatch loc = results.get(0);
		assertThat("Expected location returned", loc, sameInstance(l));
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		// GIVEN
		Long nodeId = UUID.randomUUID().getMostSignificantBits();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		datumMetadataBiz.addGeneralNodeDatumMetadata(nodeId, TEST_SOURCE_ID, meta);

		// WHEN
		replayAll();

		SecurityUtils.becomeNode(nodeId);
		biz.addGeneralNodeDatumMetadata(nodeId, TEST_SOURCE_ID, meta);
	}

	@Test
	public void postGeneralNodeDatum() {
		// GIVEN
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(UUID.randomUUID().getMostSignificantBits());
		d.setSourceId(TEST_SOURCE_ID);
		d.setCreated(Instant.now());
		d.setSamples(new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("foo", 1);

		expect(datumDao.persist(d)).andReturn(new DatumPK(UUID.randomUUID(), d.getCreated()));

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(d.getNodeId());
		biz.postGeneralNodeDatum(singleton(d));

		// THEN
	}

	@Test
	public void postStreamDatum_kindNotCached() {
		// GIVEN
		DatumProperties p = DatumProperties.propertiesOf(decimalArray("1.23"), decimalArray("2.34"),
				new String[] { "a" }, new String[] { "b" });
		BasicStreamDatum d = new BasicStreamDatum(UUID.randomUUID(), Instant.now(), p);
		BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(d.getStreamId(),
				"Etc/UTC", ObjectDatumKind.Node, 1L, "test", null, new String[] { "a" },
				new String[] { "b" }, new String[] { "c" }, null);
		DatumPK datumPk = new DatumPK(d.getStreamId(), d.getTimestamp());

		// lookup stream metadata
		Capture<StreamMetadataCriteria> metaCriteriaCaptor = new Capture<>();
		expect(metaDao.findStreamMetadata(capture(metaCriteriaCaptor))).andReturn(meta);

		// save datum
		Capture<DatumEntity> datumCaptor = new Capture<>();
		expect(datumDao.store(capture(datumCaptor))).andReturn(datumPk);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(meta.getObjectId());
		biz.postStreamDatum(singleton(d));

		// THEN
		StreamMetadataCriteria metaCriteria = metaCriteriaCaptor.getValue();
		assertThat("Meta criteria stream ID", metaCriteria.getStreamId(), is(equalTo(d.getStreamId())));

		DatumEntity entity = datumCaptor.getValue();
		assertThat("Datum ID copied", entity.getId(), is(equalTo(datumPk)));
		assertThat("Datum properties copied", entity.getProperties(), is(equalTo(p)));
	}

	private SolarLocation createLocation(Long id) {
		SolarLocation curr = new SolarLocation();
		curr.setId(id);
		curr.setCountry("CO");
		curr.setElevation(new BigDecimal("1.23"));
		curr.setLatitude(new BigDecimal("2.34"));
		curr.setLocality("locality");
		curr.setLongitude(new BigDecimal("3.45"));
		curr.setPostalCode("postalcode");
		curr.setRegion("region");
		curr.setStateOrProvince("state");
		curr.setStreet("street");
		curr.setTimeZoneId("UTC");
		return curr;
	}

	@Test
	public void getNodeLocation() {
		// GIVEN
		final Long nodeId = 1L;

		SolarLocation loc = createLocation(123L);

		expect(locationDao.getSolarLocationForNode(nodeId)).andReturn(loc);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		Location result = biz.getLocationForNode(nodeId);

		// THEN
		assertThat("DAO location returned", result, is(sameInstance(loc)));
	}

	@Test
	public void getNodeLocation_unauthorized() {
		// GIVEN
		final Long nodeId = 1L;

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(2L);
		thenThrownBy(() -> biz.getLocationForNode(nodeId)).isInstanceOf(AuthorizationException.class);
	}

	@Test
	public void updateLocation_ignoreNonGpsFields() {
		// GIVEN
		final Long nodeId = 1L;

		SolarLocation curr = createLocation(123L);

		expect(locationDao.getSolarLocationForNode(nodeId)).andReturn(curr);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		SolarLocation loc = new SolarLocation();
		loc.setCountry("B");
		loc.setElevation(curr.getElevation());
		loc.setLatitude(curr.getLatitude());
		loc.setLocality("B");
		loc.setLongitude(curr.getLongitude());
		loc.setPostalCode("B");
		loc.setRegion("B");
		loc.setStateOrProvince("B");
		loc.setStreet("B");
		loc.setTimeZoneId("B");
		biz.updateLocation(nodeId, loc);

		// THEN
		// no save as unchanged GPS data
	}

	private void assertLocationPropertiesMatch(String message, SolarLocation actual,
			SolarLocation expected) {
		assertThat(message + " ID", actual.getId(), is(expected.getId()));
		assertThat(message + " country", actual.getCountry(), is(expected.getCountry()));
		assertThat(message + " elevation", actual.getElevation(), is(expected.getElevation()));
		assertThat(message + " latitude", actual.getLatitude(), is(expected.getLatitude()));
		assertThat(message + " locality", actual.getLocality(), is(expected.getLocality()));
		assertThat(message + " longitude", actual.getLongitude(), is(expected.getLongitude()));
		assertThat(message + " postal code", actual.getPostalCode(), is(expected.getPostalCode()));
		assertThat(message + " region", actual.getRegion(), is(expected.getRegion()));
		assertThat(message + " state", actual.getStateOrProvince(), is(expected.getStateOrProvince()));
		assertThat(message + " street", actual.getStreet(), is(expected.getStreet()));
		assertThat(message + " time zone", actual.getTimeZoneId(), is(expected.getTimeZoneId()));
	}

	@Test
	public void updateLocation_change() {
		// GIVEN
		final Long nodeId = 1L;

		SolarLocation curr = createLocation(123L);

		expect(locationDao.getSolarLocationForNode(nodeId)).andReturn(curr);

		Capture<SolarLocation> locCaptor = new Capture<>();
		expect(locationDao.save(capture(locCaptor))).andReturn(curr.getId());

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		SolarLocation loc = new SolarLocation();
		loc.setElevation(new BigDecimal("1.234"));
		loc.setLatitude(new BigDecimal("2.345"));
		loc.setLongitude(new BigDecimal("3.456"));
		biz.updateLocation(nodeId, loc);

		// THEN
		SolarLocation update = locCaptor.getValue();
		SolarLocation expectedUpdate = curr.clone();
		expectedUpdate.setElevation(loc.getElevation());
		expectedUpdate.setLatitude(loc.getLatitude());
		expectedUpdate.setLongitude(loc.getLongitude());
		assertThat("Updated location saved", update, is(notNullValue()));
		assertLocationPropertiesMatch("Saved location", update, expectedUpdate);
	}

	@Test
	public void updateLocation_changeFromNull() {
		// GIVEN
		final Long nodeId = 1L;

		SolarLocation curr = createLocation(123L);
		curr.setElevation(null);
		curr.setLatitude(null);
		curr.setLongitude(null);

		expect(locationDao.getSolarLocationForNode(nodeId)).andReturn(curr);

		Capture<SolarLocation> locCaptor = new Capture<>();
		expect(locationDao.save(capture(locCaptor))).andReturn(curr.getId());

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		SolarLocation loc = new SolarLocation();
		loc.setElevation(new BigDecimal("1.234"));
		loc.setLatitude(new BigDecimal("2.345"));
		loc.setLongitude(new BigDecimal("3.456"));
		biz.updateLocation(nodeId, loc);

		// THEN
		SolarLocation update = locCaptor.getValue();
		SolarLocation expectedUpdate = curr.clone();
		expectedUpdate.setElevation(loc.getElevation());
		expectedUpdate.setLatitude(loc.getLatitude());
		expectedUpdate.setLongitude(loc.getLongitude());
		assertThat("Updated location saved", update, is(notNullValue()));
		assertLocationPropertiesMatch("Saved location", update, expectedUpdate);
	}

	@Test
	public void updateLocation_changeShared() {
		// GIVEN
		final Long nodeId = 1L;

		SolarLocation curr = new SolarLocation();
		curr.setId(123L);
		curr.setCountry("CO");
		curr.setLocality("locality");
		curr.setPostalCode("postalcode");
		curr.setRegion("region");
		curr.setStateOrProvince("state");
		curr.setTimeZoneId("UTC");

		expect(locationDao.getSolarLocationForNode(nodeId)).andReturn(curr);

		// save new non-shared instance
		final Long newLocId = 234L;
		Capture<SolarLocation> locCaptor = new Capture<>(CaptureType.ALL);
		expect(locationDao.save(capture(locCaptor))).andReturn(newLocId);

		// re-assign node to new location
		SolarNode node = new SolarNode(nodeId, curr.getId());
		expect(nodeDao.get(nodeId)).andReturn(node);

		expect(nodeDao.save(assertWith(new Assertion<SolarNode>() {

			@Override
			public void check(SolarNode argument) throws Throwable {
				assertThat("Saving same node", argument, is(sameInstance(node)));
				assertThat("Location ID changed to new location", argument.getLocationId(),
						is(newLocId));
			}

		}))).andReturn(nodeId);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		SolarLocation loc = new SolarLocation();
		loc.setElevation(new BigDecimal("1.234"));
		loc.setLatitude(new BigDecimal("2.345"));
		loc.setLongitude(new BigDecimal("3.456"));
		biz.updateLocation(nodeId, loc);

		// THEN
		assertThat("Saved new location", locCaptor.getValues(), hasSize(1));
		SolarLocation update = locCaptor.getValues().get(0);
		SolarLocation expectedUpdate = curr.clone();
		expectedUpdate.setId(null);
		expectedUpdate.setPostalCode(curr.getPostalCode().toUpperCase());
		expectedUpdate.setElevation(loc.getElevation());
		expectedUpdate.setLatitude(loc.getLatitude());
		expectedUpdate.setLongitude(loc.getLongitude());
		assertThat("New location has updated GPS details", update, is(notNullValue()));
		assertLocationPropertiesMatch("Saved location", update, expectedUpdate);
	}

}
