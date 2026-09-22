/* ==================================================================
 * TenantDatumFixtures.java - 22/09/2026 9:31:08 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.test.tenant;

import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Insert datum for the streams of a {@link TestTenant}.
 *
 * <p>
 * The tenant streams must already be inserted, via
 * {@link TestTenant#insertStreams(JdbcOperations)}. Every datum has a
 * {@code watts} instantaneous property and a {@code wattHours} accumulating
 * property.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class TenantDatumFixtures {

	/** The default start date: midnight, 1 January 2026, in the tenant time zone. */
	public static final ZonedDateTime DEFAULT_START = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
			ZoneId.of(TestTenant.TIME_ZONE));

	private TenantDatumFixtures() {
		// not available
	}

	/**
	 * Insert raw datum for every stream of a tenant.
	 *
	 * <p>
	 * The {@code watts} property is the datum index, and the {@code wattHours}
	 * property is 10 times the datum index.
	 * </p>
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param tenant
	 *        the tenant
	 * @param start
	 *        the date of the first datum
	 * @param period
	 *        the time between datum
	 * @param count
	 *        the number of datum to insert per stream
	 * @return the inserted datum
	 */
	public static List<Datum> insertDatum(JdbcOperations jdbcOps, TestTenant tenant,
			ZonedDateTime start, Duration period, int count) {
		final Instant received = Instant.now();
		final List<Datum> datums = new ArrayList<>(tenant.streams().size() * count);
		for ( ObjectDatumStreamIdentity s : tenant.streams() ) {
			for ( int i = 0; i < count; i++ ) {
				final Instant ts = start.plus(period.multipliedBy(i)).toInstant();
				datums.add(new DatumEntity(s.getStreamId(), ts, received,
						propertiesOf(new BigDecimal[] { BigDecimal.valueOf(i) },
								new BigDecimal[] { BigDecimal.valueOf(i * 10L) }, null, null)));
			}
		}
		DatumDbUtils.insertDatum(null, jdbcOps, datums);
		return datums;
	}

	/**
	 * Insert aggregate datum for every stream of a tenant.
	 *
	 * <p>
	 * The {@code watts} property is the datum index, and the {@code wattHours}
	 * property is {@literal 10}.
	 * </p>
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param tenant
	 *        the tenant
	 * @param aggregation
	 *        the aggregation, one of {@code Hour}, {@code Day}, or
	 *        {@code Month}
	 * @param start
	 *        the date of the first datum
	 * @param count
	 *        the number of datum to insert per stream, one per aggregation
	 *        period
	 * @return the inserted datum
	 * @throws IllegalArgumentException
	 *         if {@code aggregation} is not supported
	 */
	public static List<AggregateDatum> insertAggregateDatum(JdbcOperations jdbcOps,
			TestTenant tenant, Aggregation aggregation, ZonedDateTime start, int count) {
		final List<AggregateDatum> datums = new ArrayList<>(tenant.streams().size() * count);
		for ( ObjectDatumStreamIdentity s : tenant.streams() ) {
			for ( int i = 0; i < count; i++ ) {
				final ZonedDateTime date = switch (aggregation) {
					case Hour -> start.plusHours(i);
					case Day -> start.plusDays(i);
					case Month -> start.plusMonths(i);
					default -> throw new IllegalArgumentException(
							"Unsupported aggregation: " + aggregation);
				};
				final BigDecimal watts = BigDecimal.valueOf(i);
				final BigDecimal wattHoursStart = BigDecimal.valueOf(i * 10L);
				// @formatter:off
				datums.add(new AggregateDatumEntity(s.getStreamId(), date.toInstant(), aggregation,
						propertiesOf(
								new BigDecimal[] { watts },
								new BigDecimal[] { BigDecimal.TEN },
								null, null),
						statisticsOf(
								new BigDecimal[][] { { BigDecimal.ONE, watts, watts } },
								new BigDecimal[][] { { wattHoursStart,
										wattHoursStart.add(BigDecimal.TEN), BigDecimal.TEN } })));
				// @formatter:on
			}
		}
		DatumDbUtils.insertAggregateDatum(null, jdbcOps, datums);
		return datums;
	}

	/**
	 * Insert daily audit datum for every stream of a tenant.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param tenant
	 *        the tenant
	 * @param start
	 *        the date of the first day
	 * @param count
	 *        the number of days to insert per stream
	 * @return the inserted audit datum
	 */
	public static List<AuditDatum> insertDailyAuditDatum(JdbcOperations jdbcOps, TestTenant tenant,
			ZonedDateTime start, int count) {
		final List<AuditDatum> datums = new ArrayList<>(tenant.streams().size() * count);
		for ( ObjectDatumStreamIdentity s : tenant.streams() ) {
			for ( int i = 0; i < count; i++ ) {
				datums.add(AuditDatumEntity.dailyAuditDatum(s.getStreamId(),
						start.plusDays(i).toInstant(), 100L, 24L, 1, 200L, 10L, 0L, 0L));
			}
		}
		DatumDbUtils.insertAuditDatum(null, jdbcOps, datums);
		return datums;
	}

	/**
	 * Insert reset auxiliary datum for every stream of a tenant.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param tenant
	 *        the tenant
	 * @param start
	 *        the date of the first auxiliary datum
	 * @param count
	 *        the number of hourly auxiliary datum to insert per stream
	 * @return the inserted auxiliary datum
	 */
	public static List<DatumAuxiliary> insertDatumAuxiliary(JdbcOperations jdbcOps,
			TestTenant tenant, ZonedDateTime start, int count) {
		final Instant updated = Instant.now();
		final List<DatumAuxiliary> datums = new ArrayList<>(tenant.streams().size() * count);
		for ( ObjectDatumStreamIdentity s : tenant.streams() ) {
			for ( int i = 0; i < count; i++ ) {
				final DatumSamples end = new DatumSamples();
				end.putAccumulatingSampleValue("wattHours", 100);
				final DatumSamples begin = new DatumSamples();
				begin.putAccumulatingSampleValue("wattHours", 0);
				datums.add(new DatumAuxiliaryEntity(s.getStreamId(), start.plusHours(i).toInstant(),
						DatumAuxiliaryType.Reset, updated, end, begin, null, null));
			}
		}
		DatumDbUtils.insertDatumAuxiliary(null, jdbcOps, datums);
		return datums;
	}

}
