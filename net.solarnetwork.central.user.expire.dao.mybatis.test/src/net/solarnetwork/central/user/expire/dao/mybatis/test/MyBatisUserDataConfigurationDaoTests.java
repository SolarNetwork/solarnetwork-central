/* ==================================================================
 * MyBatisUserDataConfigurationDaoTests.java - 9/07/2018 11:32:33 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.dao.mybatis.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.expire.dao.mybatis.MyBatisUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;

/**
 * Test cases for the {@link MyBatisUserDataConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserDataConfigurationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private static final String TEST_NAME = "test.name";
	private static final String TEST_SERVICE_IDENT = "test.ident";
	private static final int TEST_EXPIRE_DAYS = 35; // 5 weeks
	private static final String TEST_SOURCE_ID = "test.source";

	private MyBatisUserDataConfigurationDao confDao;

	private User user;
	private UserDataConfiguration conf;

	@Before
	public void setUp() throws Exception {
		confDao = new MyBatisUserDataConfigurationDao();
		confDao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		conf = null;

		setupTestNode();

		setupUserNode(TEST_NODE_ID, this.user.getId());
	}

	private void setupUserNode(Long nodeId, Long userId) {
		jdbcTemplate.update("INSERT INTO solaruser.user_node (node_id, user_id) VALUES (?,?)", nodeId,
				userId);
	}

	@Test
	public void storeNew() {
		UserDataConfiguration conf = new UserDataConfiguration();
		conf.setCreated(new DateTime());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setServiceIdentifier(TEST_SERVICE_IDENT);

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(TEST_NODE_ID);
		conf.setFilter(filter);

		conf.setExpireDays(TEST_EXPIRE_DAYS);

		Long id = confDao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());

		// stash results for other tests to use
		conf.setId(id);
		this.conf = conf;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserDataConfiguration conf = confDao.get(this.conf.getId(), this.user.getId());
		assertThat("Found by PK", conf, notNullValue());
		assertThat("PK", conf.getId(), equalTo(this.conf.getId()));
		assertThat("Created", conf.getCreated().secondOfMinute().roundFloorCopy(),
				equalTo(this.conf.getCreated().secondOfMinute().roundFloorCopy()));
		assertThat("User ID", conf.getUserId(), equalTo(this.user.getId()));
		assertThat("Name", conf.getName(), equalTo(TEST_NAME));
		assertThat("Service identifier", conf.getServiceIdentifier(), equalTo(TEST_SERVICE_IDENT));

		Map<String, ?> sprops = conf.getServiceProperties();
		assertThat("Service props", sprops, notNullValue());
		assertThat("Service props size", sprops.keySet(), hasSize(3));
		assertThat(sprops, hasEntry("string", "foo"));
		assertThat(sprops, hasEntry("number", 42));
		assertThat(sprops, hasEntry("list", Arrays.asList("first", "second")));

		AggregateGeneralNodeDatumFilter filter = conf.getDatumFilter();
		assertThat("Filter", filter, notNullValue());
		assertThat("Filter aggregate", filter.getAggregation(), equalTo(Aggregation.Day));
		assertThat("Filter node ID", filter.getNodeId(), equalTo(TEST_NODE_ID));

		assertThat("Expire days", conf.getExpireDays(), equalTo(TEST_EXPIRE_DAYS));
	}

	@Test
	public void update() {
		storeNew();
		UserDataConfiguration conf = confDao.get(this.conf.getId(), this.user.getId());

		conf.setName("new.name");
		conf.setServiceIdentifier("new.ident");
		conf.setExpireDays(TEST_EXPIRE_DAYS + 1);
		conf.setActive(true);

		Map<String, Object> options = conf.getServiceProps();
		options.put("string", "updated");
		options.put("added-string", "added");
		conf.setServiceProps(options); // necessary to clear cached JSON

		DatumFilterCommand filter = conf.getFilter();
		filter.setNodeId((long) Integer.MIN_VALUE);
		filter.setSourceId("test.source");
		conf.setFilter(filter); // necessary to clear cached JSON

		Long id = confDao.store(conf);
		assertThat("PK unchanged", id, equalTo(this.conf.getId()));

		UserDataConfiguration updatedConf = confDao.get(id, this.user.getId());
		assertThat("Found by PK", updatedConf, notNullValue());
		assertThat("New entity returned", updatedConf, not(sameInstance(conf)));
		assertThat("PK", updatedConf.getId(), equalTo(conf.getId()));
		assertThat("Created unchanged", updatedConf.getCreated(), equalTo(conf.getCreated()));
		assertThat("Uesr ID", updatedConf.getUserId(), equalTo(conf.getUserId()));
		assertThat("Updated name", updatedConf.getName(), equalTo(conf.getName()));
		assertThat("Updated service identifier", updatedConf.getServiceIdentifier(),
				equalTo(conf.getServiceIdentifier()));
		assertThat("Updated expire days", updatedConf.getExpireDays(), equalTo(conf.getExpireDays()));
		assertThat("Updated active", updatedConf.isActive(), equalTo(true));

		Map<String, ?> sprops = updatedConf.getServiceProperties();
		assertThat("Service props", sprops, notNullValue());
		assertThat("Service props size", sprops.keySet(), hasSize(4));
		assertThat(sprops, hasEntry("string", "updated"));
		assertThat(sprops, hasEntry("number", 42));
		assertThat(sprops, hasEntry("list", Arrays.asList("first", "second")));
		assertThat(sprops, hasEntry("added-string", "added"));

		AggregateGeneralNodeDatumFilter f = updatedConf.getDatumFilter();
		assertThat("Filter", f, notNullValue());
		assertThat("New filter instance", f, not(sameInstance(filter)));
		assertThat("Filter aggregate", f.getAggregation(), equalTo(filter.getAggregation()));
		assertThat("Filter node ID", f.getNodeId(), equalTo(filter.getNodeId()));
		assertThat("Filter source ID", f.getSourceId(), equalTo(filter.getSourceId()));

		assertThat("Expire days", updatedConf.getExpireDays(), equalTo(TEST_EXPIRE_DAYS + 1));
	}

	@Test
	public void findAllForUser() {
		List<UserDataConfiguration> confs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			storeNew();
			confs.add(this.conf);
		}

		List<UserDataConfiguration> found = confDao.findConfigurationsForUser(this.user.getId());
		assertThat(found, not(sameInstance(confs)));
		assertThat(found, equalTo(confs));
	}

	@Test
	public void getAllNoData() {
		List<UserDataConfiguration> found = confDao.getAll(null);
		assertThat(found, notNullValue());
		assertThat(found, hasSize(0));
	}

	@Test
	public void getAll() {
		storeNew();

		User user2 = createNewUser("2nd.user@localhost");

		UserDataConfiguration conf2 = new UserDataConfiguration();
		conf2.setCreated(new DateTime());
		conf2.setUserId(user2.getId());
		conf2.setName(TEST_NAME);
		conf2.setServiceIdentifier(TEST_SERVICE_IDENT);
		conf2.setExpireDays(TEST_EXPIRE_DAYS);

		conf2 = confDao.get(confDao.store(conf2), user2.getId());

		List<UserDataConfiguration> expected = Arrays.asList(this.conf, conf2);

		List<UserDataConfiguration> found = confDao.getAll(null);
		assertThat(found, not(sameInstance(expected)));
		assertThat(found, equalTo(expected));
	}

	@Test
	public void expireNoData() {
		storeNew();
		long result = confDao.deleteExpiredDataForConfiguration(this.conf);
		assertThat("Nothing to delete", result, equalTo(0L));
	}

	private void insertDatum(DateTime date, Long nodeId, String sourceId) {
		jdbcTemplate.update(
				"INSERT INTO solardatum.da_datum (posted, ts, node_id, source_id, jdata_i) VALUES (?,?,?,?,?::jsonb)",
				new Timestamp(System.currentTimeMillis()), new Timestamp(date.getMillis()), nodeId,
				sourceId, "{\"watts\":10}");
	}

	private void insertHourlyDatum(DateTime date, Long nodeId, String sourceId) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_i) VALUES (?,?,?,?,?::jsonb)",
				new Timestamp(date.getMillis()),
				new Timestamp(date.toLocalDateTime().toDateTime().getMillis()), nodeId, sourceId,
				"{\"watts\":10}");
	}

	private void insertDailyDatum(DateTime date, Long nodeId, String sourceId) {
		log.debug("Inserting day datum {} {} @ {}", nodeId, sourceId, date);
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_i) VALUES (?,?,?,?,?::jsonb) ON CONFLICT DO NOTHING",
				new Timestamp(date.getMillis()),
				new Timestamp(date.toLocalDateTime().toDateTime().getMillis()), nodeId, sourceId,
				"{\"watts\":10}");
	}

	private void insertMonthlyDatum(DateTime date, Long nodeId, String sourceId) {
		log.debug("Inserting month datum {} {} @ {}", nodeId, sourceId, date);
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_i) VALUES (?,?,?,?,?::jsonb) ON CONFLICT DO NOTHING",
				new Timestamp(date.getMillis()),
				new Timestamp(date.toLocalDateTime().toDateTime().getMillis()), nodeId, sourceId,
				"{\"watts\":10}");
	}

	private void insertAuditDatumMonthly(DateTime date, Long nodeId, String sourceId) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_monthly (ts_start, node_id, source_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",
				new Timestamp(date.getMillis()), nodeId, sourceId);
	}

	private List<Map<String, Object>> findAllDatum(Long nodeId, String sourceId) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solardatum.da_datum WHERE node_id = ? AND source_id = ? ORDER BY ts",
				nodeId, sourceId);
	}

	private List<Map<String, Object>> findAllHourlyDatum(Long nodeId, String sourceId) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_datum_hourly WHERE node_id = ? AND source_id = ? ORDER BY ts_start",
				nodeId, sourceId);
	}

	private List<Map<String, Object>> findAllDailyDatum(Long nodeId, String sourceId) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_datum_daily WHERE node_id = ? AND source_id = ? ORDER BY ts_start",
				nodeId, sourceId);
	}

	private List<Map<String, Object>> findAllMonthlyDatum(Long nodeId, String sourceId) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_datum_monthly WHERE node_id = ? AND source_id = ? ORDER BY ts_start",
				nodeId, sourceId);
	}

	private List<Map<String, Object>> findAllAggStaleDatum() {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum ORDER BY agg_kind, node_id, ts_start, source_id");
	}

	private List<Map<String, Object>> findAllAuditDatumDailyStale() {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.aud_datum_daily_stale ORDER BY aud_kind, node_id, ts_start, source_id");
	}

	private static final class DataToExpire {

		private DateTime start;
		private DateTime expire;
		private long rawCount = 0L;
		private long expiredCount = 0L;
		private long hourCount = 0;
		private long expiredHourCount = 0;
		private int dayCount = 0;
		private int expiredDayCount = 0;
		private int monthCount = 0;
		private int expiredMonthCount = 0;
	}

	private DataToExpire setupDataToExpire() {
		DataToExpire result = new DataToExpire();
		DateTime today = new DateTime(DateTimeZone.forID(TEST_TZ)).dayOfMonth().roundFloorCopy();
		result.expire = new DateTime().minusDays(35).dayOfMonth().roundFloorCopy();

		DateTime start = today.minusWeeks(8);
		result.start = start;
		DateTime month = start.monthOfYear().roundFloorCopy();
		for ( int i = 0; i < 8; i++ ) {
			DateTime currDay = start.plusWeeks(i);
			DateTime currMonth = currDay.monthOfYear().roundFloorCopy();

			insertDatum(currDay, TEST_NODE_ID, TEST_SOURCE_ID);
			insertHourlyDatum(currDay, TEST_NODE_ID, TEST_SOURCE_ID);
			insertDailyDatum(currDay, TEST_NODE_ID, TEST_SOURCE_ID);

			insertMonthlyDatum(currMonth, TEST_NODE_ID, TEST_SOURCE_ID);

			// make sure monthly audit record exists, because "stale" records for these will be created
			insertAuditDatumMonthly(currMonth, TEST_NODE_ID, TEST_SOURCE_ID);

			result.rawCount++;
			result.hourCount++;
			result.dayCount++;
			if ( currDay.isBefore(result.expire) ) {
				result.expiredCount++;
				result.expiredHourCount++;
				result.expiredDayCount++;
			}
			if ( result.monthCount < 1 || month.isBefore(currMonth) ) {
				result.monthCount++;
				if ( currMonth.isBefore(result.expire.monthOfYear().roundFloorCopy()) ) {
					result.expiredMonthCount++;
				}
				month = currMonth;
			}
		}

		// delete any "stale hourly agg" records
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		return result;
	}

	private void assertAuditDatumDailyStaleMonths(DateTime start, int count) {
		List<Map<String, Object>> datum = findAllAuditDatumDailyStale();
		assertThat("Datum daily stale month count", datum, hasSize(count));
		DateTime month = start.monthOfYear().roundFloorCopy();
		for ( int i = 0; i < count; i++ ) {
			assertThat("Monthly stale " + i, datum.get(i),
					allOf(hasEntry("ts_start", (Object) new Timestamp(month.plusMonths(i).getMillis())),
							hasEntry("node_id", (Object) TEST_NODE_ID),
							hasEntry("aud_kind", (Object) "m")));
		}
	}

	private void assertCounts(String desc, DatumRecordCounts counts, Long datumCount, Long hourlyCount,
			Integer dailyCount, Integer monthlyCount) {
		assertThat("Record counts " + desc, counts, notNullValue());
		assertThat("Datum count " + desc, counts.getDatumCount(), equalTo(datumCount));
		assertThat("Hourly datum count " + desc, counts.getDatumHourlyCount(), equalTo(hourlyCount));
		assertThat("Daily datum count " + desc, counts.getDatumDailyCount(), equalTo(dailyCount));
		assertThat("Monthly datum count " + desc, counts.getDatumMonthlyCount(), equalTo(monthlyCount));
	}

	private void assertNoAggStaleDatum() {
		List<Map<String, Object>> datum = findAllAggStaleDatum();
		if ( !datum.isEmpty() ) {
			log.warn("Unexpected agg stale datum: {}", datum);
		}
		assertThat("Agg stale datum count", datum, hasSize(0));
	}

	@Test
	public void expireRawData() {
		final DataToExpire range = setupDataToExpire();
		final DateTime start = range.start;

		storeNew();

		// clear aggregation
		DatumFilterCommand filter = this.conf.getFilter();
		filter.setAggregate(null);
		this.conf.setFilter(filter);

		// first verify "preview"
		DatumRecordCounts counts = confDao.countExpiredDataForConfiguration(this.conf);
		assertCounts("preview", counts, range.expiredCount, null, null, null);

		// now execute delete
		long result = confDao.deleteExpiredDataForConfiguration(this.conf);
		assertThat("Deleted raw datum count", result, equalTo(range.expiredCount));

		List<Map<String, Object>> datum = findAllDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0), hasEntry("ts",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredCount).getMillis())));

		datum = findAllHourlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Hourly datum count", datum, hasSize((int) range.hourCount));

		datum = findAllDailyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Daily datum count", datum, hasSize(range.dayCount));

		datum = findAllMonthlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start, range.monthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyData() {
		final DataToExpire range = setupDataToExpire();
		final DateTime start = range.start;

		storeNew();

		// include hourly agg deletion
		DatumFilterCommand filter = this.conf.getFilter();
		filter.setAggregate(Aggregation.Hour);
		this.conf.setFilter(filter);

		// first verify "preview"
		DatumRecordCounts counts = confDao.countExpiredDataForConfiguration(this.conf);
		assertCounts("preview", counts, range.expiredCount, range.expiredHourCount, null, null);

		// now execute delete
		long result = confDao.deleteExpiredDataForConfiguration(this.conf);
		assertThat("Deleted raw + hourly datum count", result,
				equalTo(range.expiredCount + range.expiredHourCount));

		List<Map<String, Object>> datum = findAllDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0), hasEntry("ts",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredCount).getMillis())));

		datum = findAllHourlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0), hasEntry("ts_start",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredHourCount).getMillis())));

		datum = findAllDailyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Daily datum count", datum, hasSize(range.dayCount));

		datum = findAllMonthlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start, range.monthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyAndDailyData() {
		final DataToExpire range = setupDataToExpire();
		final DateTime start = range.start;

		storeNew();

		// include daily agg deletion
		DatumFilterCommand filter = this.conf.getFilter();
		filter.setAggregate(Aggregation.Day);
		this.conf.setFilter(filter);

		// first verify "preview"
		DatumRecordCounts counts = confDao.countExpiredDataForConfiguration(this.conf);
		assertCounts("preview", counts, range.expiredCount, range.expiredHourCount,
				range.expiredDayCount, null);

		// now execute delete
		long result = confDao.deleteExpiredDataForConfiguration(this.conf);
		assertThat("Deleted raw + hourly + daily datum count", result,
				equalTo(range.expiredCount + range.expiredHourCount + range.expiredDayCount));

		List<Map<String, Object>> datum = findAllDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0), hasEntry("ts",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredCount).getMillis())));

		datum = findAllHourlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0), hasEntry("ts_start",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredHourCount).getMillis())));

		datum = findAllDailyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Daily datum count", datum, hasSize(range.dayCount - range.expiredDayCount));
		assertThat("First daily date", datum.get(0), hasEntry("ts_start",
				(Object) new Timestamp(start.plusWeeks(range.expiredDayCount).getMillis())));

		datum = findAllMonthlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start, range.monthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyAndDailyAndMonthlyData() {
		final DataToExpire range = setupDataToExpire();
		final DateTime start = range.start;

		storeNew();

		// include monthly agg deletion
		DatumFilterCommand filter = this.conf.getFilter();
		filter.setAggregate(Aggregation.Month);
		this.conf.setFilter(filter);

		// first verify "preview"
		DatumRecordCounts counts = confDao.countExpiredDataForConfiguration(this.conf);
		assertCounts("preview", counts, range.expiredCount, range.expiredHourCount,
				range.expiredDayCount, range.expiredMonthCount);

		// now execute delete
		long result = confDao.deleteExpiredDataForConfiguration(this.conf);
		assertThat("Deleted raw + hourly + daily datum count", result, equalTo(range.expiredCount
				+ range.expiredHourCount + range.expiredDayCount + range.expiredMonthCount));

		List<Map<String, Object>> datum = findAllDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0), hasEntry("ts",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredCount).getMillis())));

		datum = findAllHourlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0), hasEntry("ts_start",
				(Object) new Timestamp(start.plusWeeks((int) range.expiredHourCount).getMillis())));

		datum = findAllDailyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Daily datum count", datum, hasSize(range.dayCount - range.expiredDayCount));
		assertThat("First daily date", datum.get(0), hasEntry("ts_start",
				(Object) new Timestamp(start.plusWeeks(range.expiredDayCount).getMillis())));

		datum = findAllMonthlyDatum(TEST_NODE_ID, TEST_SOURCE_ID);
		assertThat("Monthly datum count", datum, hasSize(range.monthCount - range.expiredMonthCount));
		assertThat("First monthly date", datum.get(0), hasEntry("ts_start", (Object) new Timestamp(
				start.monthOfYear().roundFloorCopy().plusMonths(range.expiredMonthCount).getMillis())));

		Period p = new Period(range.start.monthOfYear().roundFloorCopy(),
				range.expire.monthOfYear().roundCeilingCopy());
		assertAuditDatumDailyStaleMonths(start, p.getMonths());
		assertNoAggStaleDatum();
	}
}
