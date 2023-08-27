/* ==================================================================
 * MyBatisCentralChargeSessionDaoTests.java - 26/02/2020 12:09:29 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.mybatis.test;

import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargeSessionDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.RegistrationStatus;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;

/**
 * Test cases for the {@link MyBatisCentralChargeSessionDao} class.
 * 
 * @author matt
 * @version 2.1
 */
public class MyBatisCentralChargeSessionDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao chargePointDao;
	private MyBatisCentralChargeSessionDao dao;

	private Long userId;
	private Long nodeId;
	private CentralChargeSession last;

	@Before
	public void setUp() throws Exception {
		chargePointDao = new MyBatisCentralChargePointDao();
		chargePointDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisCentralChargeSessionDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		UUID uuid = UUID.randomUUID();
		userId = uuid.getMostSignificantBits();
		nodeId = uuid.getLeastSignificantBits();
		setupTestUser(userId);
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	private CentralChargePoint createAndSaveTestChargePoint(String vendor, String model) {
		CentralChargePoint cp = createTestChargePoint(vendor, model);
		return (CentralChargePoint) chargePointDao.get(chargePointDao.save(cp));
	}

	private CentralChargePoint createTestChargePoint(String vendor, String model) {
		ChargePointInfo info = new ChargePointInfo(UUID.randomUUID().toString());
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		CentralChargePoint cp = new CentralChargePoint(null, userId, nodeId,
				Instant.ofEpochMilli(System.currentTimeMillis()), info);
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Accepted);
		cp.setConnectorCount(2);
		return cp;
	}

	private CentralChargeSession createTestChargeSession(long chargePointId) {
		CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()),
				UUID.randomUUID().toString().substring(0, 20), chargePointId, 1, 0);
		return sess;
	}

	@Test
	public void insert() {
		// given
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		// when
		CentralChargeSession sess = createTestChargeSession(cp.getId());
		UUID pk = dao.save(sess);

		// then
		assertThat("PK preserved", pk, equalTo(sess.getId()));
		last = sess;
	}

	@Test
	public void getByPK() {
		insert();
		ChargeSession entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Auth ID", entity.getAuthId(), equalTo(last.getAuthId()));
		assertThat("Conn ID", entity.getConnectorId(), equalTo(last.getConnectorId()));
		assertThat("Transaction ID generated", entity.getTransactionId(), greaterThan(0));
	}

	@Test
	public void getByUserAndId() {
		insert();
		ChargeSession entity = dao.get(last.getId(), userId);

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Auth ID", entity.getAuthId(), equalTo(last.getAuthId()));
		assertThat("Conn ID", entity.getConnectorId(), equalTo(last.getConnectorId()));
		assertThat("Transaction ID generated", entity.getTransactionId(), greaterThan(0));
	}

	@Test
	public void getByUserAndId_userIdNoMatch() {
		insert();
		ChargeSession entity = dao.get(last.getId(), -1L);

		assertThat("Entity not found for mis-matched userId", entity, nullValue());
	}

	@Test
	public void update() {
		insert();
		ChargeSession sess = dao.get(last.getId());
		sess.setEnded(last.getCreated().plus(1, ChronoUnit.HOURS));
		sess.setEndReason(ChargeSessionEndReason.Local);
		sess.setEndAuthId(last.getAuthId());
		sess.setPosted(last.getCreated().plus(2, ChronoUnit.HOURS));
		UUID pk = dao.save(sess);
		assertThat("PK unchanged", pk, equalTo(sess.getId()));

		ChargeSession entity = dao.get(pk);
		assertThat("Ended updated", entity.getEnded(), equalTo(sess.getEnded()));
		assertThat("Posted updated", entity.getPosted(), equalTo(sess.getPosted()));
	}

	@Test
	public void findIncomplete_tx_none() {
		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(1L, 1);
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx_noMatchingId() {
		insert();
		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(last.getChargePointId() - 1,
				1);
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx_noMatchingTxId() {
		insert();
		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(last.getChargePointId(), 123);
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx_onlyComplete() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setEnded(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(s.getChargePointId(),
				s.getTransactionId());
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx() {
		insert();

		ChargeSession s = dao.get(last.getId());

		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(s.getChargePointId(),
				s.getTransactionId());
		assertThat("Incomplete session found", sess, equalTo(last));
	}

	@Test
	public void findIncomplete_tx_wildcardTxId() {
		insert();

		ChargeSession s = dao.get(last.getId());

		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(s.getChargePointId(), -1);
		assertThat("Incomplete session found using wildcard tx ID", sess, equalTo(last));
	}

	@Test
	public void findIncomplete_conn() {
		insert();

		ChargeSession sess = dao.getIncompleteChargeSessionForConnector(last.getChargePointId(),
				last.getConnectorId());
		assertThat("Incomplete session found", sess, equalTo(last));
	}

	@Test
	public void findIncomplete_chargePoint_onlyChargePoint() {
		insert();

		// add another for different charge point
		ChargePoint cp2 = createTestChargePoint("vendor 2", "model 2");
		cp2 = chargePointDao.get(chargePointDao.save(cp2));
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new CentralChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), s.getAuthId(), cp2.getId(),
				s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionsForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s));
	}

	@Test
	public void findIncomplete_chargePoint_onlyComplete() {
		insert();

		// add another for same charge point, but complete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new CentralChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), s.getAuthId(), s.getChargePointId(),
				s.getConnectorId() + 1, s.getTransactionId() + 1);
		two.setEnded(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionsForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s));
	}

	@Test
	public void findIncomplete_chargePoint() {
		insert();

		// add another for same charge point, also incomplete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new CentralChargeSession(UUID.randomUUID(), s.getCreated().plusSeconds(1),
				s.getAuthId(), s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionsForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s, two));
	}

	@Test
	public void findIncomplete_user_chargePoint() {
		insert();

		// add another for same charge point, also incomplete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new CentralChargeSession(UUID.randomUUID(), s.getCreated().plusSeconds(1),
				s.getAuthId(), s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao.getIncompleteChargeSessionsForUserForChargePoint(userId,
				s.getChargePointId());
		assertThat("Incomplete sessions found", sess, contains(s, two));
	}

	@Test
	public void findIncomplete_user_chargePoint_userNoMatch() {
		insert();

		// add another for same charge point, also incomplete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new CentralChargeSession(UUID.randomUUID(), s.getCreated().plusSeconds(1),
				s.getAuthId(), s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao.getIncompleteChargeSessionsForUserForChargePoint(-1L,
				s.getChargePointId());
		assertThat("No incomplete sessions found because user mis-match", sess, hasSize(0));
	}

	private List<SampledValue> createTestReadings() {
		// @formatter:off
		SampledValue v1 = SampledValue.builder().withSessionId(last.getId())
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()).minusSeconds(60))
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue v2 = SampledValue.builder().withSessionId(last.getId())
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("4321")
				.build();
		// @formatter:on
		return Arrays.asList(v1, v2);
	}

	@Test
	public void addReadings() {
		insert();
		dao.addReadings(createTestReadings());
		assertThat("Readings persisted", jdbcTemplate.queryForObject(
				"select count(*) from solarev.ocpp_charge_sess_reading", Integer.class), equalTo(2));
	}

	@Test
	public void findReadings() {
		insert();
		List<SampledValue> expected = createTestReadings();
		dao.addReadings(expected);

		List<SampledValue> results = dao.findReadingsForSession(last.getId());
		assertThat("Readings found", results, equalTo(expected));
	}

	@Test
	public void findReadings_none() {
		insert();

		List<SampledValue> results = dao.findReadingsForSession(last.getId());
		assertThat("Readings found", results, hasSize(0));
	}

	@Test
	public void deletePosted() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setPosted(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		int result = lastUpdateCount(dao.deletePostedChargeSessions(s.getPosted().plusSeconds(1)));
		assertThat("Deleted posted", result, equalTo(1));
		assertThat("Table empty", jdbcTemplate.queryForObject(
				"select count(*) from solarev.ocpp_charge_sess", Integer.class), equalTo(0));
	}

	@Test
	public void deletePosted_onlyOlder() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setPosted(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		ChargeSession two = new CentralChargeSession(UUID.randomUUID(), s.getCreated(), s.getAuthId(),
				s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		two.setPosted(s.getPosted().minusSeconds(1));
		dao.save(two);

		int result = lastUpdateCount(dao.deletePostedChargeSessions(s.getPosted()));
		assertThat("Deleted posted", result, equalTo(1));
		assertThat("Remaining sessions", dao.getAll(null), contains(s));
	}

	@Test
	public void deletePosted_noIncomplete() {
		insert();

		ChargeSession s = dao.get(last.getId());
		dao.save(s);

		ChargeSession two = new CentralChargeSession(UUID.randomUUID(), s.getCreated(), s.getAuthId(),
				s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		getSqlSessionTemplate().flushStatements();

		int result = lastUpdateCount(dao.deletePostedChargeSessions(
				Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(1)));
		assertThat("Deleted posted", result, equalTo(0));
		assertThat("Remaining sessions", dao.getAll(null), contains(s, two));
	}

	@Test
	public void findFiltered_sessionId() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, 0);
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setChargeSessionIds(new UUID[] { sessions.get(0).getId(), sessions.get(1).getId() });
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(0), sessions.get(1) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_active() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, i + 1);
			if ( i >= 2 ) {
				sess.setEnded(Instant.now());
			}
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setActive(true);
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(0), sessions.get(1) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_inactive() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, i + 1);
			if ( i >= 2 ) {
				sess.setEnded(Instant.now());
			}
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setActive(false);
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(2) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_transactionId() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, i + 1);
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setTransactionIds(new Integer[] { sessions.get(0).getTransactionId(),
				sessions.get(1).getTransactionId() });
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(0), sessions.get(1) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_chargePointIdentifier() {
		// GIVEN
		final List<ChargePoint> cps = new ArrayList<>();
		cps.add(createAndSaveTestChargePoint("foo", "bar"));
		cps.add(createAndSaveTestChargePoint("bim", "bam"));
		cps.add(createAndSaveTestChargePoint("zig", "zag"));

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cps.get(i).getId(), 1, i + 1);
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setIdentifiers(
				new String[] { cps.get(0).getInfo().getId(), cps.get(1).getInfo().getId() });
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(0), sessions.get(1) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_user_pagination() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, i + 1);
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setMax(2);
		FilterResults<ChargeSession, UUID> results1 = dao.findFiltered(filter);
		filter.setOffset(2);
		FilterResults<ChargeSession, UUID> results2 = dao.findFiltered(filter);

		// THEN
		assertThat("Results 1 returned", results1, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(0), sessions.get(1) };
		assertThat("Results 1", stream(results1.spliterator(), false).toList(), contains(expected));

		expected = new ChargeSession[] { sessions.get(2) };
		assertThat("Results 2", stream(results2.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void findFiltered_endReason() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		final int sessionCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final List<ChargeSession> sessions = new ArrayList<>(sessionCount);
		for ( int i = 0; i < sessionCount; i++ ) {
			CentralChargeSession sess = new CentralChargeSession(UUID.randomUUID(), start.plusSeconds(i),
					UUID.randomUUID().toString().substring(0, 20), cp.getId(), 1, i + 1);
			sess.setEnded(Instant.now());
			sess.setEndReason(ChargeSessionEndReason.forCode(i));
			sessions.add(dao.get(dao.save(sess)));
		}

		// WHEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		filter.setEndReason(ChargeSessionEndReason.EmergencyStop);
		FilterResults<ChargeSession, UUID> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, is(notNullValue()));

		ChargeSession[] expected = new ChargeSession[] { sessions.get(1) };
		assertThat("Results", stream(results.spliterator(), false).toList(), contains(expected));
	}

	@Test
	public void endSession() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargeSession sess = dao.get(dao.save(createTestChargeSession(cp.getId())));

		// WHEN
		boolean result = dao.endSession(userId, sess.getId(), ChargeSessionEndReason.Other, null);
		ChargeSession updated = dao.get(sess.getId());

		// THEN
		assertThat("Session updated", result, is(equalTo(true)));
		assertThat("Updated session end date set", updated.getEnded(), is(notNullValue()));
		assertThat("Updated session end reason", updated.getEndReason(),
				is(equalTo(ChargeSessionEndReason.Other)));
		assertThat("Updated session end auth ID", updated.getEndAuthId(), is(nullValue()));
	}

	@Test
	public void endSession_wrongUserId() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargeSession sess = dao.get(dao.save(createTestChargeSession(cp.getId())));

		// WHEN
		boolean result = dao.endSession(userId + 1, sess.getId(), ChargeSessionEndReason.Other, null);

		// THEN
		assertThat("Session updated", result, is(equalTo(false)));
	}

	@Test
	public void endSession_endAuthId() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargeSession sess = dao.get(dao.save(createTestChargeSession(cp.getId())));

		// WHEN
		boolean result = dao.endSession(userId, sess.getId(), ChargeSessionEndReason.Other, "yo yo");
		ChargeSession updated = dao.get(sess.getId());

		// THEN
		assertThat("Session updated", result, is(equalTo(true)));
		assertThat("Updated session end date set", updated.getEnded(), is(notNullValue()));
		assertThat("Updated session end reason", updated.getEndReason(),
				is(equalTo(ChargeSessionEndReason.Other)));
		assertThat("Updated session end auth ID", updated.getEndAuthId(), is(equalTo("yo yo")));
	}

	@Test
	public void endSession_alreadyEnded() {
		// GIVEN
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargeSession sess = createTestChargeSession(cp.getId());
		sess.setEnded(Instant.now());
		sess.setEndReason(ChargeSessionEndReason.EmergencyStop);
		sess.setEndAuthId("foo");
		sess = dao.get(dao.save(sess));

		// WHEN
		boolean result = dao.endSession(userId, sess.getId(), ChargeSessionEndReason.Other, null);
		ChargeSession updated = dao.get(sess.getId());

		// THEN
		assertThat("Session updated", result, is(equalTo(false)));
		assertThat("Fetched session end date unchanged", updated.getEnded(),
				is(equalTo(sess.getEnded())));
		assertThat("Fetched session end reason unchanged", updated.getEndReason(),
				is(equalTo(sess.getEndReason())));
		assertThat("Fetched session end auth ID unchanged", updated.getEndAuthId(),
				is(equalTo(sess.getEndAuthId())));
	}

	@Test
	public void nextTransactionId() {
		// GIVEN
		final int count = 5;

		// WHEN
		Set<Integer> ids = new LinkedHashSet<>(count);
		for ( int i = 0; i < count; i++ ) {
			ids.add(dao.nextTransactionId());
		}

		// THEN
		then(ids).as("Unique IDs generated").hasSize(count);
	}

}
