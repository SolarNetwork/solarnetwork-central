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

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.expire.dao.mybatis.MyBatisUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;

/**
 * Test cases for the {@link MyBatisUserDataConfigurationDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDataConfigurationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private static final String TEST_NAME = "test.name";
	private static final String TEST_SERVICE_IDENT = "test.ident";
	private static final int TEST_EXPIRE_DAYS = 35; // 5 weeks
	private static final String TEST_SOURCE_ID = "test.source";

	private MyBatisUserDataConfigurationDao confDao;

	private User user;
	private UserDataConfiguration conf;
	private ObjectDatumStreamMetadata streamMeta;

	@Before
	public void setUp() throws Exception {
		confDao = new MyBatisUserDataConfigurationDao();
		confDao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		conf = null;

		setupTestNode();

		setupUserNode(TEST_NODE_ID, this.user.getId());

		streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ, ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "watts" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(streamMeta));
	}

	private void setupUserNode(Long nodeId, Long userId) {
		jdbcTemplate.update("INSERT INTO solaruser.user_node (node_id, user_id) VALUES (?,?)", nodeId,
				userId);
	}

	@Test
	public void storeNew() {
		UserDataConfiguration conf = new UserDataConfiguration();
		conf.setCreated(Instant.now());
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
		assertThat("Created", conf.getCreated().truncatedTo(ChronoUnit.SECONDS),
				equalTo(this.conf.getCreated().truncatedTo(ChronoUnit.SECONDS)));
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
		conf2.setCreated(Instant.now());
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

	private void insertDatum(Instant date, UUID streamId) {
		DatumProperties p = propertiesOf(decimalArray("10"), null, null, null);
		DatumEntity d = new DatumEntity(streamId, date, Instant.now(), p);
		DatumDbUtils.insertDatum(log, jdbcTemplate, singleton(d));
	}

	private void insertHourlyDatum(Instant date, UUID streamId) {
		DatumProperties p = propertiesOf(decimalArray("10"), null, null, null);
		DatumPropertiesStatistics s = DatumPropertiesStatistics
				.statisticsOf(new BigDecimal[][] { decimalArray("6", "10", "10") }, null);
		AggregateDatumEntity d = new AggregateDatumEntity(streamId, date, Aggregation.Hour, p, s);
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, singleton(d));
	}

	private void insertDailyDatum(Instant date, UUID streamId) {
		log.debug("Inserting day datum {} @ {}", streamId, date);
		DatumProperties p = propertiesOf(decimalArray("10"), null, null, null);
		DatumPropertiesStatistics s = DatumPropertiesStatistics
				.statisticsOf(new BigDecimal[][] { decimalArray("6", "10", "10") }, null);
		AggregateDatumEntity d = new AggregateDatumEntity(streamId, date, Aggregation.Day, p, s);
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, singleton(d));
	}

	private void insertMonthlyDatum(Instant date, UUID streamId) {
		log.debug("Inserting month datum {} @ {}", streamId, date);
		DatumProperties p = propertiesOf(decimalArray("10"), null, null, null);
		DatumPropertiesStatistics s = DatumPropertiesStatistics
				.statisticsOf(new BigDecimal[][] { decimalArray("6", "10", "10") }, null);
		AggregateDatumEntity d = new AggregateDatumEntity(streamId, date, Aggregation.Month, p, s);
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, singleton(d));
	}

	private void insertAuditDatumMonthly(Instant date, UUID streamId) {
		AuditDatumEntity d = AuditDatumEntity.monthlyAuditDatum(streamId, date, 0L, 0L, 0, 0, 0L, 0L,
				0L);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, singleton(d));
	}

	private List<Datum> findAllDatum(UUID streamId) {
		List<Datum> rows = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Current datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private Comparator<AggregateDatum> orderByTime() {
		return new Comparator<AggregateDatum>() {

			@Override
			public int compare(AggregateDatum o1, AggregateDatum o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		};
	}

	private List<AggregateDatum> findAllHourlyDatum(UUID streamId) {
		List<AggregateDatum> rows = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Hour)
				.stream().filter(e -> e.getStreamId().equals(streamId)).sorted(orderByTime())
				.collect(toList());
		log.debug("Current Hour datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private List<AggregateDatum> findAllDailyDatum(UUID streamId) {
		List<AggregateDatum> rows = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Day)
				.stream().filter(e -> e.getStreamId().equals(streamId)).sorted(orderByTime())
				.collect(toList());
		log.debug("Current Day datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private List<AggregateDatum> findAllMonthlyDatum(UUID streamId) {
		List<AggregateDatum> rows = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Month)
				.stream().filter(e -> e.getStreamId().equals(streamId)).sorted(orderByTime())
				.collect(toList());
		log.debug("Current Month datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private List<StaleAggregateDatum> findAllAggStaleDatum() {
		List<StaleAggregateDatum> rows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		log.debug("Current stale datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private List<StaleAuditDatum> findAllAuditDatumDailyStale() {
		List<StaleAuditDatum> rows = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		log.debug("Current stale audit datum table:\n{}\n",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private static final class DataToExpire {

		private Instant start;
		private Instant expire;
		private long rawCount = 0L;
		private long expiredCount = 0L;
		private long hourCount = 0;
		private long expiredHourCount = 0;
		private int dayCount = 0;
		private int expiredDayCount = 0;
		private int monthCount = 0;
		private int expiredMonthCount = 0;
		private int staleAuditMonthCount = 0;
	}

	private DataToExpire setupDataToExpire() {
		final ZoneId zone = ZoneId.of(TEST_TZ);
		DataToExpire result = new DataToExpire();
		ZonedDateTime today = ZonedDateTime.now(zone).with(firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);
		result.expire = ZonedDateTime.now(zone).minusDays(35).with(firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS).toInstant();

		ZonedDateTime start = today.minus(8, ChronoUnit.WEEKS);
		result.start = start.toInstant();
		ZonedDateTime month = start.with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
		Set<ZonedDateTime> months = new LinkedHashSet<>();
		for ( int i = 0; i < 8; i++ ) {
			ZonedDateTime currDay = start.plusWeeks(i);
			ZonedDateTime currMonth = currDay.with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);

			insertDatum(currDay.toInstant(), streamMeta.getStreamId());
			insertHourlyDatum(currDay.toInstant(), streamMeta.getStreamId());
			insertDailyDatum(currDay.toInstant(), streamMeta.getStreamId());

			months.add(currMonth);

			result.rawCount++;
			result.hourCount++;
			result.dayCount++;
			if ( currDay.toInstant().isBefore(result.expire) ) {
				result.expiredCount++;
				result.expiredHourCount++;
				result.expiredDayCount++;
			}
			if ( result.monthCount < 1 || month.isBefore(currMonth) ) {
				result.monthCount++;
				if ( currMonth.isBefore(result.expire.atZone(zone).with(firstDayOfMonth())
						.truncatedTo(ChronoUnit.DAYS)) ) {
					result.expiredMonthCount++;
				}
				if ( currMonth.toInstant().isBefore(result.expire) ) {
					result.staleAuditMonthCount++;
				}
				month = currMonth;
			}
		}
		for ( ZonedDateTime currMonth : months ) {
			insertMonthlyDatum(currMonth.toInstant(), streamMeta.getStreamId());

			// make sure monthly audit record exists, because "stale" records for these will be created
			insertAuditDatumMonthly(currMonth.toInstant(), streamMeta.getStreamId());
		}

		findAllDatum(streamMeta.getStreamId());
		findAllHourlyDatum(streamMeta.getStreamId());
		findAllDailyDatum(streamMeta.getStreamId());
		findAllMonthlyDatum(streamMeta.getStreamId());

		return result;
	}

	private void assertAuditDatumDailyStaleMonths(Instant start, int count) {
		List<StaleAuditDatum> datum = findAllAuditDatumDailyStale();
		assertThat("Datum daily stale month count", datum, hasSize(count));
		ZonedDateTime month = start.atZone(ZoneId.of(TEST_TZ)).with(firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);
		for ( int i = 0; i < count; i++ ) {
			StaleAuditDatum d = datum.get(i);
			assertThat("Monthly stale stream " + i, d.getStreamId(), equalTo(streamMeta.getStreamId()));
			assertThat("Monthly stale date " + i, d.getTimestamp(),
					equalTo(month.plusMonths(i).toInstant()));
			assertThat("Monhly stale kind " + i, d.getKind(), equalTo(Aggregation.Month));
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
		List<StaleAggregateDatum> datum = findAllAggStaleDatum();
		if ( !datum.isEmpty() ) {
			log.warn("Unexpected agg stale datum: {}", datum);
		}
		assertThat("Agg stale datum count", datum, hasSize(0));
	}

	@Test
	public void expireRawData() {
		final DataToExpire range = setupDataToExpire();
		final ZonedDateTime start = range.start.atZone(ZoneId.of(TEST_TZ));

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

		List<? extends Datum> datum = findAllDatum(streamMeta.getStreamId());
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks(range.expiredCount).toInstant()));

		datum = findAllHourlyDatum(streamMeta.getStreamId());
		assertThat("Hourly datum count", datum, hasSize((int) range.hourCount));

		datum = findAllDailyDatum(streamMeta.getStreamId());
		assertThat("Daily datum count", datum, hasSize(range.dayCount));

		datum = findAllMonthlyDatum(streamMeta.getStreamId());
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start.toInstant(), range.staleAuditMonthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyData() {
		final DataToExpire range = setupDataToExpire();
		final ZonedDateTime start = range.start.atZone(ZoneId.of(TEST_TZ));

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

		List<? extends Datum> datum = findAllDatum(streamMeta.getStreamId());
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredCount).toInstant()));

		datum = findAllHourlyDatum(streamMeta.getStreamId());
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredHourCount).toInstant()));

		datum = findAllDailyDatum(streamMeta.getStreamId());
		assertThat("Daily datum count", datum, hasSize(range.dayCount));

		datum = findAllMonthlyDatum(streamMeta.getStreamId());
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start.toInstant(), range.staleAuditMonthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyAndDailyData() {
		final DataToExpire range = setupDataToExpire();
		final ZonedDateTime start = range.start.atZone(ZoneId.of(TEST_TZ));

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

		List<? extends Datum> datum = findAllDatum(streamMeta.getStreamId());
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredCount).toInstant()));

		datum = findAllHourlyDatum(streamMeta.getStreamId());
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredHourCount).toInstant()));

		datum = findAllDailyDatum(streamMeta.getStreamId());
		assertThat("Daily datum count", datum, hasSize(range.dayCount - range.expiredDayCount));
		assertThat("First daily date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks(range.expiredDayCount).toInstant()));

		datum = findAllMonthlyDatum(streamMeta.getStreamId());
		assertThat("Monthly datum count", datum, hasSize(range.monthCount));

		assertAuditDatumDailyStaleMonths(start.toInstant(), range.staleAuditMonthCount);
		assertNoAggStaleDatum();
	}

	@Test
	public void expireRawAndHourlyAndDailyAndMonthlyData() {
		final DataToExpire range = setupDataToExpire();
		final ZonedDateTime start = range.start.atZone(ZoneId.of(TEST_TZ));

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

		List<? extends Datum> datum = findAllDatum(streamMeta.getStreamId());
		assertThat("Datum count", datum, hasSize((int) (range.rawCount - range.expiredCount)));
		assertThat("First datum date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredCount).toInstant()));

		datum = findAllHourlyDatum(streamMeta.getStreamId());
		assertThat("Hourly datum count", datum,
				hasSize((int) (range.hourCount - range.expiredHourCount)));
		assertThat("First hourly date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks((int) range.expiredHourCount).toInstant()));

		datum = findAllDailyDatum(streamMeta.getStreamId());
		assertThat("Daily datum count", datum, hasSize(range.dayCount - range.expiredDayCount));
		assertThat("First daily date", datum.get(0).getTimestamp(),
				equalTo(start.plusWeeks(range.expiredDayCount).toInstant()));

		datum = findAllMonthlyDatum(streamMeta.getStreamId());
		assertThat("Monthly datum count", datum, hasSize(range.monthCount - range.expiredMonthCount));
		assertThat("First monthly date", datum.get(0).getTimestamp(),
				equalTo(start.with(firstDayOfMonth()).truncatedTo(ChronoUnit.MONTHS)
						.plusMonths(range.expiredMonthCount).toInstant()));

		Period p = Period.between(
				start.with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toLocalDate(),
				range.expire.atZone(start.getZone()).with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
						.plusMonths(1).toLocalDate());
		assertAuditDatumDailyStaleMonths(start.toInstant(), p.getMonths());
		assertNoAggStaleDatum();
	}
}
