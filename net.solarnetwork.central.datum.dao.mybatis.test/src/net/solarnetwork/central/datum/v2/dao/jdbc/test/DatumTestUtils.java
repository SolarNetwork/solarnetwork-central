/* ==================================================================
 * DatumTestUtils.java - 30/10/2020 2:26:18 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.util.NumberUtils;

/**
 * Helper methods for datum tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class DatumTestUtils {

	private DatumTestUtils() {
		// don't construct me
	}

	/**
	 * Create a {@link Matcher} for an array of {@link BigDecimal} values.
	 * 
	 * @param nums
	 *        the string numbers, which will be parsed as {@link BigDecimal}
	 *        instances
	 * @return the matcher
	 */
	public static Matcher<BigDecimal[]> arrayOfDecimals(String... nums) {
		BigDecimal[] vals = NumberUtils.decimalArray(nums);
		return Matchers.arrayContaining(vals);
	}

	/**
	 * Create a {@link Matcher} for a string that compares to the contents of a
	 * text resource.
	 * 
	 * @param resource
	 *        the name of the resource
	 * @param clazz
	 *        the class to load the resource from
	 * @return the matcher
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 */
	public static Matcher<String> equalToTextResource(String resource, Class<?> clazz) {
		return equalToTextResource(resource, clazz, null);
	}

	/**
	 * Create a {@link Matcher} for a string that compares to the contents of a
	 * text resource.
	 * 
	 * @param resource
	 *        the name of the resource
	 * @param clazz
	 *        the class to load the resource from
	 * @param skip
	 *        an optional pattern that will be used to match against lines;
	 *        matches will be left out of the string used to match
	 * @return the matcher
	 * @throws RuntimeException
	 *         if the resource cannot be loaded
	 */
	public static Matcher<String> equalToTextResource(String resource, Class<?> clazz, Pattern skip) {
		try (InputStream in = clazz.getResourceAsStream(resource)) {
			if ( in == null ) {
				throw new RuntimeException(
						"Resource " + resource + " not found from class " + clazz.getName() + ".");
			}
			String txt = null;
			if ( skip == null ) {
				txt = FileCopyUtils.copyToString(new InputStreamReader(in, Charset.forName("UTF-8")));
			} else {
				StringBuilder buf = new StringBuilder(1024);
				try (BufferedReader r = new BufferedReader(
						new InputStreamReader(in, Charset.forName("UTF-8")))) {
					while ( true ) {
						String line = r.readLine();
						if ( line == null ) {
							break;
						}
						if ( skip.matcher(line).find() ) {
							continue;
						}
						buf.append(line).append("\n");
					}
				}
				txt = buf.toString();
			}
			return Matchers.equalToIgnoringWhiteSpace(txt);
		} catch ( IOException e ) {
			throw new RuntimeException("Error reading text resource [" + resource + "]", e);
		}
	}

	/**
	 * Assert one datum has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertDatum(String prefix, Datum result, Datum expected) {
		assertThat(prefix + " datum returned", result, notNullValue());
		assertThat(prefix + " stream ID matches", result.getStreamId(), equalTo(expected.getStreamId()));
		assertThat(prefix + " timestamp", result.getTimestamp(), equalTo(expected.getTimestamp()));
		if ( expected.getProperties() != null ) {
			if ( expected.getProperties().getInstantaneous() != null ) {
				assertThat(prefix + " instantaneous", result.getProperties().getInstantaneous(),
						Matchers.arrayContaining(expected.getProperties().getInstantaneous()));
			}
			if ( expected.getProperties().getAccumulating() != null ) {
				assertThat(prefix + " accumulating", result.getProperties().getAccumulating(),
						Matchers.arrayContaining(expected.getProperties().getAccumulating()));
			}
			if ( expected.getProperties().getStatus() != null ) {
				assertThat(prefix + " status", result.getProperties().getStatus(),
						Matchers.arrayContaining(expected.getProperties().getStatus()));
			}
			if ( expected.getProperties().getTags() != null ) {
				assertThat(prefix + " accumulating", result.getProperties().getAccumulating(),
						Matchers.arrayContaining(expected.getProperties().getAccumulating()));
			}
		}
	}

	/**
	 * Assert one aggregate datum has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertAggregateDatum(String prefix, AggregateDatum result,
			AggregateDatum expected) {
		assertDatum(prefix, result, expected);
		if ( expected.getStatistics() != null ) {
			if ( expected.getStatistics().getInstantaneous() != null ) {
				List<Matcher<? super BigDecimal[]>> m = new ArrayList<>(
						expected.getStatistics().getInstantaneousLength());
				for ( BigDecimal[] a : expected.getStatistics().getInstantaneous() ) {
					m.add(Matchers.arrayContaining(a));
				}
				assertThat(prefix + " instantaneous stats", result.getStatistics().getInstantaneous(),
						Matchers.arrayContaining(m));
			}
			if ( expected.getStatistics().getAccumulating() != null ) {
				List<Matcher<? super BigDecimal[]>> m = new ArrayList<>(
						expected.getStatistics().getAccumulatingLength());
				for ( BigDecimal[] a : expected.getStatistics().getAccumulating() ) {
					m.add(Matchers.arrayContaining(a));
				}
				assertThat(prefix + " instantaneous stats", result.getStatistics().getAccumulating(),
						Matchers.arrayContaining(m));
			}
		}
	}

	/**
	 * Assert one stale aggregate datum has values that match another.
	 * 
	 * @param prefix
	 *        the assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertStaleAggregateDatum(String prefix, StaleAggregateDatum result,
			StaleAggregateDatum expected) {
		assertThat(prefix + " stale aggregate record kind", result.getKind(),
				equalTo(expected.getKind()));
		assertThat(prefix + "stale aggregate record stream ID", result.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale aggregate record timestamp", result.getTimestamp(),
				equalTo(expected.getTimestamp()));
	}

	/**
	 * Assert one stale audit datum has values that match another.
	 * 
	 * @param prefix
	 *        the assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertStaleAuditDatum(String prefix, StaleAuditDatum result,
			StaleAuditDatum expected) {
		assertThat(prefix + " stale audit record kind", result.getKind(), equalTo(expected.getKind()));
		assertThat(prefix + "stale audit record stream ID", result.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale audit record timestamp", result.getTimestamp(),
				equalTo(expected.getTimestamp()));
	}

	/**
	 * Assert one reading datum has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertReadingDatum(String prefix, ReadingDatum result, ReadingDatum expected) {
		DatumTestUtils.assertAggregateDatum(prefix, result, expected);
		assertThat(prefix + " end timestamp", result.getEndTimestamp(),
				equalTo(expected.getEndTimestamp()));
	}

	/**
	 * Delete from common datum tables.
	 * 
	 * <p>
	 * This is designed to help with tests that circumvent test transaction
	 * auto-rollback.
	 * </p>
	 * 
	 * @param jdbcTemplate
	 *        the JDBC operations to use
	 */
	public static void cleanupDatabase(JdbcOperations jdbcTemplate) {
		if ( jdbcTemplate == null ) {
			return;
		}
		jdbcTemplate.update("DELETE FROM solardatm.da_datm");
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_flux");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_hourly");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_monthly");
		jdbcTemplate.update("DELETE FROM solardatm.aud_acc_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_hourly");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_monthly");
		jdbcTemplate.update("DELETE FROM solardatm.aud_stale_datm");
		jdbcTemplate.update("DELETE FROM solaruser.user_node");
		jdbcTemplate.update("DELETE FROM solaruser.user_user");
		jdbcTemplate.update("DELETE FROM solarnet.sn_node");
		jdbcTemplate.update("DELETE FROM solarnet.sn_loc");
	}

}
