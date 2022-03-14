/* ==================================================================
 * MyBatisTaxCodeDaoTests.java - 24/07/2020 6:53:52 AM
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

import static java.time.Instant.now;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisTaxCodeDao;
import net.solarnetwork.central.user.billing.snf.domain.TaxCode;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Identity;

/**
 * Test cases for the {@link MyBatisTaxCodeDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisTaxCodeDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String NZ_TAX = "GST";
	private static final String NZ_ITEM = "pavlova";
	private static final String NZ_ZONE = "NZ";
	private static final ZoneId NZ_TZ = ZoneId.of("Pacific/Auckland");

	private static final String CA_TAX = "CA Sales Tax";
	private static final String SF_TAX = "SF Sales Tax";
	private static final String SF_ITEM = "sourdough";
	private static final String SF_ZONE = "US.CA.SF";
	private static final ZoneId SF_TZ = ZoneId.of("America/Los_Angeles");

	private MyBatisTaxCodeDao dao;

	@Before
	public void setup() {
		dao = new MyBatisTaxCodeDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
	}

	private List<TaxCode> populateTestTaxData() {
		final List<TaxCode> codes = new ArrayList<>(8);
		// NZ GST
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), NZ_ZONE, NZ_ITEM,
				NZ_TAX, new BigDecimal("0.10"),
				LocalDate.of(1986, 10, 1).atStartOfDay(NZ_TZ).toInstant(),
				LocalDate.of(1989, 7, 1).atStartOfDay(NZ_TZ).toInstant()));
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), NZ_ZONE, NZ_ITEM,
				NZ_TAX, new BigDecimal("0.125"),
				LocalDate.of(1989, 7, 1).atStartOfDay(NZ_TZ).toInstant(),
				LocalDate.of(2010, 10, 1).atStartOfDay(NZ_TZ).toInstant()));
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), NZ_ZONE, NZ_ITEM,
				NZ_TAX, new BigDecimal("0.125"),
				LocalDate.of(2010, 10, 1).atStartOfDay(NZ_TZ).toInstant(), null));

		// Tax 2
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), SF_ZONE, SF_ITEM,
				CA_TAX, new BigDecimal("0.04"),
				LocalDate.of(1984, 11, 1).atStartOfDay(SF_TZ).toInstant(),
				LocalDate.of(2014, 1, 1).atStartOfDay(SF_TZ).toInstant()));
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), SF_ZONE, SF_ITEM,
				CA_TAX, new BigDecimal("0.0825"),
				LocalDate.of(2014, 1, 1).atStartOfDay(SF_TZ).toInstant(), null));

		// Tax 3
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), SF_ZONE, SF_ITEM,
				SF_TAX, new BigDecimal("0.02"),
				LocalDate.of(1999, 11, 1).atStartOfDay(SF_TZ).toInstant(),
				LocalDate.of(2012, 1, 1).atStartOfDay(SF_TZ).toInstant()));
		codes.add(new TaxCode(UUID.randomUUID().getMostSignificantBits(), now(), SF_ZONE, SF_ITEM,
				SF_TAX, new BigDecimal("0.025"),
				LocalDate.of(2012, 1, 1).atStartOfDay(SF_TZ).toInstant(),
				LocalDate.of(2020, 1, 1).atStartOfDay(SF_TZ).toInstant()));

		jdbcTemplate.batchUpdate(
				"insert into solarbill.bill_tax_code (id,created,tax_zone,item_key,tax_code,tax_rate,valid_from,valid_to)"
						+ " VALUES (?,?,?,?,?,?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						TaxCode c = codes.get(i);
						int col = 0;
						ps.setLong(++col, c.getId());
						ps.setTimestamp(++col, new Timestamp(c.getCreated().toEpochMilli()));
						ps.setString(++col, c.getZone());
						ps.setString(++col, c.getItemKey());
						ps.setString(++col, c.getCode());
						ps.setBigDecimal(++col, c.getRate());
						ps.setTimestamp(++col, new Timestamp(c.getValidFrom().toEpochMilli()));
						if ( c.getValidTo() != null ) {
							ps.setTimestamp(++col, new Timestamp(c.getValidTo().toEpochMilli()));
						} else {
							ps.setNull(++col, Types.TIMESTAMP_WITH_TIMEZONE);
						}
					}

					@Override
					public int getBatchSize() {
						return codes.size();
					}
				});
		return codes;
	}

	private <T extends Identity<K>, K> List<T> assertFilterResults(String prefix,
			FilterResults<T, K> results, int returnedResultCount) {
		assertThat(prefix + " results not null", results, notNullValue());
		assertThat(prefix + " returned result count", results.getReturnedResultCount(),
				equalTo(returnedResultCount));
		return StreamSupport.stream(results.spliterator(), false).collect(Collectors.toList());
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_noMatch() {
		// GIVEN
		populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone("No Zone");
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(2020, 1, 1).atStartOfDay(NZ_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		assertFilterResults("No match", results, 0);
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_beforeStart() {
		// GIVEN
		populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(NZ_ZONE);
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(1970, 1, 1).atStartOfDay(NZ_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		assertFilterResults("No matches before start", results, 0);
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_pastMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(NZ_ZONE);
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(1987, 1, 1).atStartOfDay(NZ_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("First tax code applies", results, 1);
		assertThat("First GST rate", codes.get(0), equalTo(allCodes.get(0)));
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_recentPastMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(NZ_ZONE);
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(2008, 1, 1).atStartOfDay(NZ_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("Updated tax code applies", results, 1);
		assertThat("2nd GST rate", codes.get(0), equalTo(allCodes.get(1)));
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_contemporaryMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(NZ_ZONE);
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(2020, 7, 1).atStartOfDay(SF_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("Current rates apply when no validTo configured",
				results, 1);
		assertThat("Current GST rate", codes.get(0), equalTo(allCodes.get(2)));
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_transitionBoundaryMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(NZ_ZONE);
		f.setItemKey(NZ_ITEM);
		f.setDate(LocalDate.of(1989, 7, 1).atStartOfDay(NZ_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("New rate applies at boundary", results, 1);
		assertThat("GST rate at transition uses exclusive end date", codes.get(0),
				equalTo(allCodes.get(1)));
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_pastMultiMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(SF_ZONE);
		f.setItemKey(SF_ITEM);
		f.setDate(LocalDate.of(2011, 1, 1).atStartOfDay(SF_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("CA and SF taxes apply", results, 2);
		assertThat("Multi tax codes at date, sorted by code", codes,
				contains(allCodes.get(3), allCodes.get(5)));
	}

	@Test
	public void filter_zoneAndItemKeyAndDate_contemporaryMultiMatch() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZone(SF_ZONE);
		f.setItemKey(SF_ITEM);
		f.setDate(LocalDate.of(2020, 7, 1).atStartOfDay(SF_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("CA tax only because SF tax ended", results, 1);
		assertThat("Multi tax codes at date, sorted by code", codes, contains(allCodes.get(4)));
	}

	@Test
	public void filter_zoneAndDate_multiZones() {
		// GIVEN
		List<TaxCode> allCodes = populateTestTaxData();

		// WHEN
		TaxCodeFilter f = new TaxCodeFilter();
		f.setZones(new String[] { NZ_ZONE, SF_ZONE });
		f.setDate(LocalDate.of(2013, 1, 1).atStartOfDay(SF_TZ).toInstant());
		FilterResults<TaxCode, Long> results = dao.findFiltered(f, null, null, null);

		// THEN
		List<TaxCode> codes = assertFilterResults("CA, GST, SF zones apply", results, 3);
		assertThat("Multi tax codes at date, sorted by code", codes,
				contains(allCodes.get(3), allCodes.get(2), allCodes.get(6)));
	}
}
