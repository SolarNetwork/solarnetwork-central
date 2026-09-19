/* ==================================================================
 * FixedIntervalCloudDatumStreamServiceTests.java - 14 Sept 2026 9:50:45 am
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

package net.solarnetwork.central.c2c.biz.fixed.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.groupingBy;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.Reference;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.SpelExpression;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.RequestEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.fixed.FixedGranularity;
import net.solarnetwork.central.c2c.biz.fixed.FixedIntervalCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import tools.jackson.databind.JsonNode;

/**
 * Test cases for the {@link FixedIntervalCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class FixedIntervalCloudDatumStreamServiceTests implements CloudIntegrationsUserEvents {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Mock
	private RestOperations restOps;

	@Mock
	private OAuth2AuthorizedClientManager oauthClientManager;

	@Captor
	private ArgumentCaptor<OAuth2AuthorizeRequest> authRequestCaptor;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Mock
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Mock
	private DatumEntityDao datumDao;

	@Captor
	private ArgumentCaptor<DatumCriteria> datumCriteriaCaptor;

	@Mock
	private Cache<Long, CloudDataValue[]> siteInventoryCache;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private CloudIntegrationsExpressionService expressionService;

	private FixedIntervalCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new FixedIntervalCloudDatumStreamService(clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(FixedIntervalCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void dataValue() {
		// no vales supported
		then(service.dataValueFilters(Locale.getDefault())).as("No data value filters supported")
				.isEmpty();
	}

	@Test
	public void dataValues_root() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of());

		// THEN
		// @formatter:off
		and.then(results)
			.as("Results provided")
			.isEmpty()
			;
		// @formatter:off
	}

	@Test
	public void latestDatum_staticValues() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", Reference, "123");
		prop1.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "b", Reference, "234");
		prop2.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now(), Status, "c", Reference, "abc");
		prop3.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.TenMinute.name()
		));
		// @formatter:on

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum generated from static values")
			.hasSize(1)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", Integer.valueOf(prop1.getValueReference()));
				expectedSamples1.putAccumulatingSampleValue("b", Integer.valueOf(prop2.getValueReference()));
				expectedSamples1.putStatusSampleValue("c", prop3.getValueReference());

				and.then(list)
					.element(0)
					.as("Datum timestamp is start of previous period")
					.returns(clock.instant().minus(FixedGranularity.TenMinute.getTickAmount()), from(Datum::getTimestamp))
					.as("Datum samples generated from static property references")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void nonNumberInstantaneousAndAccumulatingPropertiesIgnored() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", Reference, "abc");
		prop1.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "b", Reference, "def");
		prop2.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now(), Status, "c", Reference, "ghi");
		prop3.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.TenMinute.name()
		));
		// @formatter:on

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		// non-number instantaneous & accumulating values are ignored
		expectedSamples.putStatusSampleValue("c", prop3.getValueReference());

		// @formatter:off
		and.then(result)
			.as("Datum generated from static values")
			.hasSize(1)
			.element(0)
			.as("Datum kind is from DatumStream configuration")
			.returns(datumStream.getKind(), from(Datum::getKind))
			.as("Datum object ID is from DatumStream configuration")
			.returns(datumStream.getObjectId(), from(Datum::getObjectId))
			.as("Datum source ID is from DatumStream configuration")
			.returns(datumStream.getSourceId(), from(Datum::getSourceId))
			.as("Datum timestamp is start of previous period")
			.returns(clock.instant().minus(FixedGranularity.TenMinute.getTickAmount()), from(Datum::getTimestamp))
			.as("Datum samples generated from static property references")
			.returns(expectedSamples, from(Datum::asSampleOperations))
			;
		// @formatter:on
	}

	@Test
	public void datum_staticValues() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", Reference, "123");
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.FifteenMinute.name()
		));
		// @formatter:on

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(Duration.ofHours(1)));
		filter.setEndDate(clock.instant());

		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum generated from static values for 1 hour")
			.hasSize(4)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", Integer.valueOf(prop1.getValueReference()));
				for (int i = 0; i < 4; i++ ) {
					and.then(list)
						.element(i)
						.as("Datum %d timestamp", i)
						.returns(filter.getStartDate().plus(Duration.ofMinutes(15 * i)), from(Datum::getTimestamp))
						.as("Datum samples generated from static property references")
						.returns(expectedSamples1, from(Datum::asSampleOperations))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_expressionValues() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", SpelExpression,
				"1 + 2");
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.FifteenMinute.name()
		));
		// @formatter:on

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(Duration.ofHours(1)));
		filter.setEndDate(clock.instant());

		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum generated from expression values for 1 hour")
			.hasSize(4)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", (1 + 2));
				for (int i = 0; i < 4; i++ ) {
					and.then(list)
						.element(i)
						.as("Datum %d timestamp", i)
						.returns(filter.getStartDate().plus(Duration.ofMinutes(15 * i)), from(Datum::getTimestamp))
						.as("Datum samples generated from expression property references")
						.returns(expectedSamples1, from(Datum::asSampleOperations))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_virtualSourceIds() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", SpelExpression,
				"1 + 2");
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.FifteenMinute.name(),
				CloudDatumStreamService.VIRTUAL_SOURCE_IDS_SETTING, List.of("v1", "v2")
		));
		// @formatter:on

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(Duration.ofHours(1)));
		filter.setEndDate(clock.instant());

		Iterable<Datum> result = service.datum(datumStream, filter);

		Map<Instant, List<Datum>> resultGroupedByTs = StreamSupport.stream(result.spliterator(), false)
				.collect(groupingBy(Datum::getTimestamp));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum generated from expression values for 1 hour")
			.hasSize(4 * 3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			;
		and.then(resultGroupedByTs)
			.hasSize(4)
			.containsKeys(
				  filter.getStartDate()
				, filter.getStartDate().plus(15L, MINUTES)
				, filter.getStartDate().plus(30L, MINUTES)
				, filter.getStartDate().plus(45L, MINUTES)
			)
			.satisfies(map -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", (1 + 2));
				for (int i = 0; i < 4; i++ ) {
					final List<Datum> dataForTimestamp = map.get(filter.getStartDate().plus(15L * i, MINUTES));
					final Set<String> sourceIds = dataForTimestamp.stream().map(Datum::getSourceId).collect(Collectors.toSet());
					and.then(sourceIds)
						.as("Datum for original source + 2 virtual source IDs provided")
						.containsExactlyInAnyOrder(datumStream.getSourceId(), "v1", "v2")
						;
					and.then(dataForTimestamp)
						.allSatisfy(d -> {
							and.then(d)
								.as("Datum samples generated from expression property reference")
								.returns(expectedSamples1, from(Datum::asSampleOperations))
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_dayGranularity_customTimeZone() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", Reference, "123");
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final ZoneId zone = ZoneId.of("America/Los_Angeles");
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.Day.name(),
				FixedIntervalCloudDatumStreamService.TIME_ZONE_SETTING, zone.getId()
		));
		// @formatter:on

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(Duration.ofDays(4)));
		filter.setEndDate(clock.instant());

		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		final ZonedDateTime expectedQueryStartDate = filter.getStartDate().atZone(zone)
				.truncatedTo(ChronoUnit.DAYS);
		ZonedDateTime expectedQueryEndDate = filter.getEndDate().atZone(zone)
				.truncatedTo(ChronoUnit.DAYS);
		if ( expectedQueryEndDate.toInstant().isBefore(filter.getEndDate()) ) {
			expectedQueryEndDate = expectedQueryEndDate.plusDays(1);
		}

		// @formatter:off
		and.then(result)
			.as("Datum generated from static values for 4 days")
			.hasSize((int)ChronoUnit.DAYS.between(expectedQueryStartDate, expectedQueryEndDate))
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", Integer.valueOf(prop1.getValueReference()));
				for (int i = 0; i < 4; i++ ) {
					and.then(list)
						.element(i)
						.as("Datum %d timestamp is SOD in configured time zone", i)
						.returns(expectedQueryStartDate.plusDays(i).toInstant(), from(Datum::getTimestamp))
						.as("Datum samples generated from static property references")
						.returns(expectedSamples1, from(Datum::asSampleOperations))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_monthGranularity_customTimeZone() {
		// GIVEN
		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "a", Reference, "123");
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final ZoneId zone = ZoneId.of("America/Los_Angeles");
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				FixedIntervalCloudDatumStreamService.GRANULARITY_SETTING, FixedGranularity.Month.name(),
				FixedIntervalCloudDatumStreamService.TIME_ZONE_SETTING, zone.getId()
		));
		// @formatter:on

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(Duration.ofDays(90)));
		filter.setEndDate(clock.instant());

		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		final ZonedDateTime expectedQueryStartDate = filter.getStartDate().atZone(zone)
				.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
		ZonedDateTime expectedQueryEndDate = filter.getEndDate().atZone(zone)
				.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
		if ( expectedQueryEndDate.toInstant().isBefore(filter.getEndDate()) ) {
			expectedQueryEndDate = expectedQueryEndDate.plusMonths(1);
		}

		// @formatter:off
		and.then(result)
			.as("Datum generated from static values for 4 days")
			.hasSize((int)ChronoUnit.MONTHS.between(expectedQueryStartDate, expectedQueryEndDate))
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("a", Integer.valueOf(prop1.getValueReference()));
				for (int i = 0; i < 4; i++ ) {
					and.then(list)
						.element(i)
						.as("Datum %d timestamp is first day of month SOD in configured time zone", i)
						.returns(expectedQueryStartDate.plusMonths(i).toInstant(), from(Datum::getTimestamp))
						.as("Datum samples generated from static property references")
						.returns(expectedSamples1, from(Datum::asSampleOperations))
						;
				}
			})
			;
		// @formatter:on
	}

}
