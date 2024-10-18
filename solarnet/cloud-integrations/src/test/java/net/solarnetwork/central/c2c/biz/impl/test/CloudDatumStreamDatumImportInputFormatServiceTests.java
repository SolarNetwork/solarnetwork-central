/* ==================================================================
 * CloudDatumStreamDatumImportInputFormatServiceTests.java - 15/10/2024 8:52:07 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.DATUM_STREAM_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.END_DATE_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.START_DATE_SETTING;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link CloudDatumStreamDatumImportInputFormatService}
 * class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CloudDatumStreamDatumImportInputFormatServiceTests {

	private static final Long TEST_USER_ID = randomLong();
	private static final String TEST_DATUM_STREAM_SERVICE_IDENTIFIER = randomString();

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Captor
	private ArgumentCaptor<CloudDatumStreamQueryFilter> filterCaptor;

	private CloudDatumStreamDatumImportInputFormatService service;

	@BeforeEach
	public void setup() {
		var datumStreamServices = Map.of(TEST_DATUM_STREAM_SERVICE_IDENTIFIER, datumStreamService);
		service = new CloudDatumStreamDatumImportInputFormatService(datumStreamDao,
				datumStreamServices::get);
	}

	@Test
	public void singleBatchRange() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		List<Datum> datumList = new ArrayList<>(8);
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
		}
		// @formatter:off
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(datumList))
				.willReturn(new BasicCloudDatumStreamQueryResult(Collections.emptyList()));
		// @formatter:on

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		// WHEN
		try (var ctx = service.createImportContext(config, resource, (importService, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		then(datumStreamService).shouldHaveNoMoreInteractions();

		// TODO: validate progress?

		// @formatter:off
		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(datumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(datumList.get(i)))
				;
		}
		// @formatter:on
	}

	@Test
	public void multiBatchRange() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		final List<List<Datum>> datumPages = new ArrayList<>(8);
		List<Datum> datumList = new ArrayList<>(8);
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
			if ( day.getDayOfWeek() == DayOfWeek.MONDAY ) {
				datumPages.add(datumList);
				datumList = new ArrayList<>(8);
			}
		}

		// return some paginated results
		final List<BasicQueryFilter> queryFilters = new ArrayList<>(8);
		queryFilters.add(BasicQueryFilter.ofRange(startDate.toInstant(), endDate.toInstant()));

		var datumStub = given(datumStreamService.datum(same(datumStream), any()));
		for ( var datumPage : datumPages ) {
			var filter = BasicQueryFilter.ofRange(now(), now());
			queryFilters.add(filter);
			datumStub = datumStub
					.willReturn(new BasicCloudDatumStreamQueryResult(null, filter, datumPage));
		}
		// return the final non-paginated end result
		datumStub.willReturn(new BasicCloudDatumStreamQueryResult(emptyList()));

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		// WHEN
		try (var ctx = service.createImportContext(config, resource, (importService, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		// @formatter:off
		then(datumStreamService).should(times(queryFilters.size()))
			.datum(same(datumStream), filterCaptor.capture());

		and.then(filterCaptor.getAllValues())
			.as("Queried for pages using original filter plus all next filters provided in service results")
			.satisfies(filters -> {
				for (int i =0, len = filters.size(); i < len; i++ ) {
					and.then(filters).element(i)
						.as("Start date from expected page filter")
						.returns(queryFilters.get(i).getStartDate(), CloudDatumStreamQueryFilter::getStartDate)
						.as("End date from expected page filter")
						.returns(queryFilters.get(i).getEndDate(), CloudDatumStreamQueryFilter::getEndDate)
						;
				}
			})
			;

		// TODO: validate progress?

		List<Datum> allDatumList = datumPages.stream().flatMap(l -> l.stream()).toList();
		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(allDatumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(allDatumList.get(i)))
				;
		}
		// @formatter:on
	}

}
