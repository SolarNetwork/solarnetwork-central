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

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.JdbcOperations;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.Location;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.util.NumberUtils;

/**
 * Helper methods for datum tests.
 * 
 * @author matt
 * @version 1.2
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
		String txt = ClassUtils.getResourceAsString(resource, clazz, skip);
		return Matchers.equalToIgnoringWhiteSpace(txt);
	}

	/**
	 * Assert one datum stream metadata has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result metadata
	 * @param expected
	 *        the expected metadata
	 */
	public static void assertDatumStreamMetadata(String prefix, DatumStreamMetadata result,
			DatumStreamMetadata expected) {
		assertThat(prefix + " meta returned", result, notNullValue());
		assertThat(prefix + " stream ID", result.getStreamId(), equalTo(expected.getStreamId()));
		assertThat(prefix + " property names", result.getPropertyNames(),
				arrayContaining(expected.getPropertyNames()));
		assertThat(prefix + " time zone ID", result.getTimeZoneId(), equalTo(expected.getTimeZoneId()));
		if ( expected instanceof ObjectDatumStreamMetadata ) {
			assertThat(prefix + " is object metadata", result,
					instanceOf(ObjectDatumStreamMetadata.class));
			ObjectDatumStreamMetadata oResult = (ObjectDatumStreamMetadata) result;
			ObjectDatumStreamMetadata oExpected = (ObjectDatumStreamMetadata) expected;
			assertThat(prefix + " object kind", oResult.getKind(), equalTo(oExpected.getKind()));
			assertThat(prefix + " object ID", oResult.getObjectId(), equalTo(oExpected.getObjectId()));
			assertThat(prefix + " source ID", oResult.getSourceId(), equalTo(oExpected.getSourceId()));
			assertThat(prefix + " JSON", JsonUtils.getStringMap(oResult.getMetaJson()),
					equalTo(JsonUtils.getStringMap(oExpected.getMetaJson())));
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

		// don't verify timestamp for RunningTotal
		if ( !(expected instanceof AggregateDatum
				&& ((AggregateDatum) expected).getAggregation() == Aggregation.RunningTotal) ) {
			assertThat(prefix + " timestamp", result.getTimestamp(), equalTo(expected.getTimestamp()));
		}

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
				assertThat(prefix + " tags", result.getProperties().getTags(),
						Matchers.arrayContainingInAnyOrder(expected.getProperties().getTags()));
			}
		}
	}

	/**
	 * Assert one datum auxiliary has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertDatumAuxiliary(String prefix, DatumAuxiliary result,
			DatumAuxiliary expected) {
		assertThat(prefix + " datum returned", result, notNullValue());
		assertThat(prefix + " stream ID matches", result.getStreamId(), equalTo(expected.getStreamId()));
		assertThat(prefix + " timestamp", result.getTimestamp(), equalTo(expected.getTimestamp()));
		assertThat(prefix + " type", result.getType(), equalTo(expected.getType()));
		assertThat(prefix + " samples final", result.getSamplesFinal(),
				equalTo(expected.getSamplesFinal()));
		assertThat(prefix + " samples start", result.getSamplesStart(),
				equalTo(expected.getSamplesStart()));
		assertThat(prefix + " metadata", result.getMetadata(), equalTo(expected.getMetadata()));
	}

	/**
	 * Assert one audit datum has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static void assertAuditDatum(String prefix, AuditDatum audit, AuditDatum expected) {
		assertThat(prefix + " " + expected.getAggregation() + " audit record stream ID",
				audit.getStreamId(), equalTo(expected.getStreamId()));
		assertThat(prefix + " " + expected.getAggregation() + " audit record timestamp",
				audit.getTimestamp(), equalTo(expected.getTimestamp()));
		assertThat(prefix + " " + expected.getAggregation() + " audit record aggregate",
				audit.getAggregation(), equalTo(expected.getAggregation()));
		assertThat(prefix + " " + expected.getAggregation() + " audit datum count",
				audit.getDatumCount(), equalTo(expected.getDatumCount()));
		if ( expected.getAggregation() != Aggregation.RunningTotal ) {
			assertThat(prefix + " " + expected.getAggregation() + " audit prop count",
					audit.getDatumPropertyCount(), equalTo(expected.getDatumPropertyCount()));
			assertThat(prefix + " " + expected.getAggregation() + " audit datum query count",
					audit.getDatumQueryCount(), equalTo(expected.getDatumQueryCount()));
			assertThat(prefix + " " + expected.getAggregation() + " audit prop update count",
					audit.getDatumPropertyUpdateCount(),
					equalTo(expected.getDatumPropertyUpdateCount()));
		}
		if ( expected.getAggregation() == Aggregation.Day
				|| expected.getAggregation() == Aggregation.Month ) {
			assertThat(prefix + " " + expected.getAggregation() + " audit datum hour count",
					audit.getDatumHourlyCount(), equalTo(expected.getDatumHourlyCount()));
			assertThat(prefix + " " + expected.getAggregation() + " audit monthly hour count",
					audit.getDatumMonthlyCount(), equalTo(expected.getDatumMonthlyCount()));
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
				assertThat(prefix + " accumulating stats", result.getStatistics().getAccumulating(),
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
		assertThat(prefix + " stale aggregate record stream ID", result.getStreamId(),
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
	 * Assert one record counts has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static final void assertDatumRecordCounts(String prefix, DatumRecordCounts result,
			DatumRecordCounts expected) {
		assertThat(prefix + " datum returned", result, notNullValue());
		assertThat(prefix + " datum count", result.getDatumCount(), equalTo(expected.getDatumCount()));
		assertThat(prefix + " datum hourly count", result.getDatumHourlyCount(),
				equalTo(expected.getDatumHourlyCount()));
		assertThat(prefix + " datum daily count", result.getDatumDailyCount(),
				equalTo(expected.getDatumDailyCount()));
		assertThat(prefix + " datum monthly count", result.getDatumMonthlyCount(),
				equalTo(expected.getDatumMonthlyCount()));
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
		jdbcTemplate.update("DELETE FROM solardatm.da_datm_meta");
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_flux");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_hourly");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_monthly");
		jdbcTemplate.update("DELETE FROM solardatm.aud_acc_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_io");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_daily");
		jdbcTemplate.update("DELETE FROM solardatm.aud_datm_monthly");
		jdbcTemplate.update("DELETE FROM solardatm.aud_stale_datm");
		jdbcTemplate.update("DELETE FROM solaruser.user_node");
		jdbcTemplate.update("DELETE FROM solaruser.user_user");
		jdbcTemplate.update("DELETE FROM solarnet.sn_node");
		jdbcTemplate.update("DELETE FROM solarnet.sn_loc");
	}

	/**
	 * Get a function that remaps the stream ID of {@link AggregateDatum}.
	 * 
	 * @param streamId
	 *        the stream ID to remap datum to
	 * @return the function
	 */
	public static Function<AggregateDatum, AggregateDatum> remapStream(UUID streamId) {
		return e -> {
			return new AggregateDatumEntity(streamId, e.getTimestamp(), e.getAggregation(),
					e.getProperties(), e.getStatistics());
		};
	}

	/**
	 * Assert one location has values that match another.
	 * 
	 * @param prefix
	 *        an assertion message prefix
	 * @param result
	 *        the result datum
	 * @param expected
	 *        the expected datum
	 */
	public static final void assertLocation(String prefix, Location result, Location expected) {
		assertThat(prefix + " returned", result, notNullValue());
		assertThat(prefix + " country", result.getCountry(), equalTo(expected.getCountry()));
		assertThat(prefix + " region", result.getRegion(), equalTo(expected.getRegion()));
		assertThat(prefix + " state", result.getStateOrProvince(),
				equalTo(expected.getStateOrProvince()));
		assertThat(prefix + " locality", result.getLocality(), equalTo(expected.getLocality()));
		assertThat(prefix + " postal code", result.getPostalCode(), equalTo(expected.getPostalCode()));
		assertThat(prefix + " street", result.getStreet(), equalTo(expected.getStreet()));
		assertThat(prefix + " lat", result.getLatitude(), equalTo(expected.getLatitude()));
		assertThat(prefix + " lon", result.getLongitude(), equalTo(expected.getLongitude()));
		assertThat(prefix + " elevation", result.getCountry(), equalTo(expected.getCountry()));
		assertThat(prefix + " time zone", result.getTimeZoneId(), equalTo(expected.getTimeZoneId()));
	}

	/**
	 * Get an iterator for Datum parsed from a classpath CSV file.
	 * 
	 * @param clazz
	 *        the class to load the CSV resource from
	 * @param resource
	 *        the CSV resource to load
	 * @param metaProvider
	 *        the metadata provider
	 * @return the iterator
	 * @since 1.1
	 */
	public static Iterator<Datum> datumResourceIterator(Class<?> clazz, String resource,
			ObjectDatumStreamMetadataProvider metaProvider) {
		try {
			return new DatumCsvIterator(new CsvListReader(
					new InputStreamReader(clazz.getResourceAsStream(resource), "UTF-8"),
					CsvPreference.STANDARD_PREFERENCE), metaProvider);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a list of Datum parsed from a classpath CSV file.
	 * 
	 * @param clazz
	 *        the class to load the CSV resource from
	 * @param resource
	 *        the CSV resource to load
	 * @param metaProvider
	 *        the metadata provider
	 * @return the list
	 * @since 1.1
	 */
	public static List<Datum> datumResourceToList(Class<?> clazz, String resource,
			ObjectDatumStreamMetadataProvider metaProvider) {
		List<Datum> result = new ArrayList<>();
		Iterator<Datum> itr = datumResourceIterator(clazz, resource, metaProvider);
		while ( itr.hasNext() ) {
			result.add(itr.next());
		}
		return result;
	}

}
