/* ==================================================================
 * AbstractMyBatisDaoTestSupport.java - 25/02/2020 7:50:22 am
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Base class for MyBatis DAO tests.
 * 
 * @author matt
 * @version 2.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisDaoTestSupport extends AbstractCentralTransactionalTest {

	private SqlSessionFactory sqlSessionFactory;
	private SqlSessionTemplate sqlSessionTemplate;

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		if ( sqlSessionTemplate == null ) {
			sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
		}
		return sqlSessionTemplate;
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	@After
	public void flushStatements() {
		getSqlSessionTemplate().flushStatements();
	}

	/**
	 * Get the last update statement's update count result.
	 * 
	 * <p>
	 * This method can be used to automatically flush the MyBatis batch
	 * statements and return the last statement's update count.
	 * </p>
	 * 
	 * @param result
	 *        the result as returned by a {@code getSqlSession().update()} style
	 *        statement
	 * @return the update count, flushing batch statements as necessary
	 */
	protected int lastUpdateCount(int result) {
		if ( result < 0 ) {
			List<BatchResult> batchResults = getSqlSessionTemplate().flushStatements();
			if ( batchResults != null && !batchResults.isEmpty() ) {
				int[] counts = batchResults.get(batchResults.size() - 1).getUpdateCounts();
				result = counts[counts.length - 1];
			}
		}
		return result;
	}

	/**
	 * Create a test address instance.
	 * 
	 * @return the address
	 */
	protected Address createTestAddress() {
		Address s = new Address(null, Instant.ofEpochMilli(System.currentTimeMillis()));
		s.setName("Tester Dude");
		s.setEmail("test@localhost");
		s.setCountry("NZ");
		s.setTimeZoneId("Pacific/Auckland");
		s.setRegion("Region");
		s.setStateOrProvince("State");
		s.setLocality("Wellington");
		s.setPostalCode("1001");
		s.setStreet(new String[] { "Level 1", "123 Main Street" });
		return s;
	}

	/**
	 * Create a test account for a given address.
	 * 
	 * @param address
	 *        the address
	 * @return the account
	 */
	protected Account createTestAccount(Address address) {
		Account account = new Account(null, UUID.randomUUID().getMostSignificantBits(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		account.setAddress(address);
		account.setCurrencyCode("NZD");
		account.setLocale("en_NZ");
		return account;
	}

	protected void debugQuery(String query) {
		StringBuilder buf = new StringBuilder();
		buf.append("Query ").append(query).append(":\n");
		for ( Map<String, Object> row : jdbcTemplate.queryForList(query) ) {
			buf.append(row).append("\n");
		}
		log.debug(buf.toString());
	}

	protected void debugRows(String table, String sort) {
		StringBuilder buf = new StringBuilder();
		buf.append("Table ").append(table).append(":\n");
		for ( Map<String, Object> row : rows(table, sort) ) {
			buf.append(row).append("\n");
		}
		log.debug(buf.toString());
	}

	protected List<Map<String, Object>> rows(String table) {
		return rows(table, "id");
	}

	protected List<Map<String, Object>> rows(String table, String sort) {
		StringBuilder buf = new StringBuilder("select * from ");
		buf.append(table);
		if ( sort != null ) {
			buf.append(" order by ").append(sort);
		}
		return jdbcTemplate.queryForList(buf.toString());
	}

	protected UUID setupDatumStream(Long nodeId, String sourceId) {
		jdbcTemplate.update(
				"insert into solardatm.da_datm_meta " + "(stream_id,node_id,source_id) VALUES (?,?,?) "
						+ "ON CONFLICT (node_id, source_id) DO NOTHING",
				UUID.randomUUID(), nodeId, sourceId);
		UUID streamId = jdbcTemplate.queryForObject(
				"select stream_id from solardatm.da_datm_meta where node_id = ? and source_id = ?",
				UUID.class, nodeId, sourceId);
		return streamId;
	}

	protected UUID addAuditDatumMonthly(Long nodeId, String sourceId, Instant date, long propCount,
			long datumQueryCount, int datumCount, short datumHourlyCount, short datumDailyCount,
			boolean monthPresent) {
		UUID streamId = setupDatumStream(nodeId, sourceId);
		jdbcTemplate.update("insert into solardatm.aud_datm_monthly "
				+ "(ts_start,stream_id,prop_count,datum_q_count,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres)"
				+ "VALUES (?,?::uuid,?,?,?,?,?,?)", new Timestamp(date.toEpochMilli()),
				streamId.toString(), propCount, datumQueryCount, datumCount, datumHourlyCount,
				datumDailyCount, monthPresent);
		return streamId;
	}

	protected void addAuditAccumulatingDatumDaily(Long nodeId, String sourceId, Instant date,
			int datumCount, int datumHourlyCount, int datumDailyCount, int datumMonthlyCount) {
		UUID streamId = setupDatumStream(nodeId, sourceId);
		jdbcTemplate.update("insert into solardatm.aud_acc_datm_daily "
				+ "(ts_start,stream_id,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_count)"
				+ "VALUES (?,?::uuid,?,?,?,?)", new Timestamp(date.toEpochMilli()), streamId.toString(),
				datumCount, datumHourlyCount, datumDailyCount, datumMonthlyCount);
	}

	protected void assertAccountBalance(Long accountId, BigDecimal chargeTotal,
			BigDecimal paymentTotal) {
		getSqlSessionTemplate().flushStatements();
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"select * from solarbill.bill_account_balance where acct_id = ?", accountId);
		assertThat("Account balance row found", rows, hasSize(1));
		BigDecimal charge = (BigDecimal) rows.get(0).get("charge_total");
		BigDecimal payment = (BigDecimal) rows.get(0).get("payment_total");
		assertThat("Account total charge matches", chargeTotal.compareTo(charge), equalTo(0));
		assertThat("Account total payment matches", paymentTotal.compareTo(payment), equalTo(0));
	}

}
