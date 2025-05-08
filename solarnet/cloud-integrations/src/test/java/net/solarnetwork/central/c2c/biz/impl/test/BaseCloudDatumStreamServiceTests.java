/* ==================================================================
 * BaseCloudDatumStreamServiceTests.java - 13/12/2024 3:04:27 pm
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
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Test cases for the {@link BaseCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class BaseCloudDatumStreamServiceTests {

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private CloudIntegrationsExpressionService expressionService;

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Mock
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private SolarNodeMetadataReadOnlyDao nodeMetadataReadOnlyDao;

	@Mock
	private DatumEntityDao datumEntityDao;

	@Captor
	private ArgumentCaptor<DatumCriteria> filterCaptor;

	@Mock
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache;

	private TestCloudDatumStreamService service(List<String> supportedPlaceholders) {
		return new TestCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao,
				supportedPlaceholders);
	}

	private TestCloudDatumStreamService serviceWithExpressionSupport() {
		BasicCloudIntegrationsExpressionService exprSrvc = new BasicCloudIntegrationsExpressionService(
				nodeOwnershipDao);
		exprSrvc.setMetadataDao(nodeMetadataReadOnlyDao);
		return new TestCloudDatumStreamService(userEventAppenderBiz, encryptor, exprSrvc, integrationDao,
				datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, List.of());
	}

	private class TestCloudDatumStreamService extends BaseCloudDatumStreamService {

		private final List<String> supportedPlaceholders;

		public TestCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
				TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
				CloudIntegrationConfigurationDao integrationDao,
				CloudDatumStreamConfigurationDao datumStreamDao,
				CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
				CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
				List<String> supportedPlaceholders) {
			super("test", "Test", Clock.systemUTC(), userEventAppenderBiz, encryptor, expressionService,
					integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao,
					Collections.emptyList());
			this.supportedPlaceholders = supportedPlaceholders;
			setDatumStreamMetadataDao(datumStreamMetadataDao);
			setDatumStreamMetadataCache(datumStreamMetadataCache);
		}

		@Override
		protected Iterable<String> supportedPlaceholders() {
			return supportedPlaceholders;
		}

		@Override
		public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
			return null;
		}

		@Override
		public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
				Map<String, ?> filters) {
			return null;
		}

		@Override
		public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
			return null;
		}

		@Override
		public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
				CloudDatumStreamQueryFilter filter) {
			return null;
		}

		@Override
		public List<Map<String, ?>> resolvePlaceholderSets(Map<String, ?> placeholders,
				Collection<String> sourceValueRefs) {
			return super.resolvePlaceholderSets(placeholders, sourceValueRefs);
		}

	}

	@Test
	public void resolvePlaceholderSets_noPlaceholders_noRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(null, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result contains single map")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_noPlaceholders_withRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var refs = List.of("/123/abc", "/123/def", "/234/geh");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(null, refs);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result contains one map for each value ref input")
			.hasSize(refs.size())
			.contains(
					Map.of("a", "123", "b", "abc"),
					Map.of("a", "123", "b", "def"),
					Map.of("a", "234", "b", "geh")
					)
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_withPlaceholders_noRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var placeholders = Map.of("a", "123", "b", "abc");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(placeholders, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result contains one map of given placeholders")
			.hasSize(1)
			.contains(Map.of("a", "123", "b", "abc"))
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholderSets_withPlaceholders_withRefs() {
		// GIVEN
		var service = service(List.of("a", "b"));

		var placeholders = Map.of("a", "123", "b", "abc", "c", "---");

		var refs = List.of("/123/abc", "/123/def", "/234/geh");

		// WHEN
		List<Map<String, ?>> result = service.resolvePlaceholderSets(placeholders, refs);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result contains one map for each value ref input, each merged with given placeholders")
			.hasSize(refs.size())
			.contains(
					Map.of("a", "123", "b", "abc", "c", "---"),
					Map.of("a", "123", "b", "def", "c", "---"),
					Map.of("a", "234", "b", "geh", "c", "---")
					)
			;
		// @formatter:on
	}

	@Test
	public void evaluateExpressions_streamMetadata() {
		// GIVEN
		final var service = serviceWithExpressionSupport();

		final var userId = randomLong();
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var integrationId = randomLong();
		final var mappingId = randomLong();

		final var exprProp = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 0,
				Instant.now());
		exprProp.setEnabled(true);
		exprProp.setPropertyName("out");
		exprProp.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp.setValueReference("""
				metadata('/m/foo')
				""");

		final var datum = GeneralDatum.nodeDatum(nodeId, sourceId,
				Instant.now().truncatedTo(ChronoUnit.HOURS), new DatumSamples());

		final var foo = randomLong();
		given(datumStreamMetadataCache.get(new ObjectDatumStreamMetadataId(datum.getKind(),
				datum.getObjectId(), datum.getSourceId())))
						.willReturn(new GeneralDatumMetadata(Map.of("foo", foo)));

		// WHEN
		service.evaluateExpressions(null, null, List.of(exprProp), List.of(datum), mappingId,
				integrationId);

		// THEN
		then(datumStreamMetadataCache).shouldHaveNoMoreInteractions();
		then(datumStreamMappingDao).shouldHaveNoInteractions();

		// @formatter:off
		and.then(datum.getSampleLong(exprProp.getPropertyType(), exprProp.getPropertyName()))
			.as("Datum populated with value from metadata lazily resolved from cache")
			.isEqualTo(foo)
			;
		// @formatter:on
	}

	@Test
	public void evaluateExpressions_nodeMetadata() {
		// GIVEN
		final var service = serviceWithExpressionSupport();

		final var userId = randomLong();
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var integrationId = randomLong();
		final var mappingId = randomLong();

		final var exprProp = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 0,
				Instant.now());
		exprProp.setEnabled(true);
		exprProp.setPropertyName("out");
		exprProp.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp.setValueReference("""
				nodeMetadata('/m/foo')
				""");

		final var datum = GeneralDatum.nodeDatum(nodeId, sourceId,
				Instant.now().truncatedTo(ChronoUnit.HOURS), new DatumSamples());

		final var foo = randomLong();
		final var nodeMeta = new SolarNodeMetadata(nodeId);
		nodeMeta.setMeta(new GeneralDatumMetadata(Map.of("foo", foo)));
		given(nodeMetadataReadOnlyDao.get(nodeId)).willReturn(nodeMeta);

		// WHEN
		service.evaluateExpressions(null, null, List.of(exprProp), List.of(datum), mappingId,
				integrationId);

		// THEN
		then(nodeMetadataReadOnlyDao).shouldHaveNoMoreInteractions();
		then(datumStreamMetadataCache).shouldHaveNoInteractions();
		then(datumStreamMappingDao).shouldHaveNoInteractions();

		// @formatter:off
		and.then(datum.getSampleLong(exprProp.getPropertyType(), exprProp.getPropertyName()))
			.as("Datum populated with value from metadata lazily resolved from cache")
			.isEqualTo(foo)
			;
		// @formatter:on
	}

	@Test
	public void evaluateExpressions_latestMatchingFetch() {
		// GIVEN
		final var service = serviceWithExpressionSupport();
		service.setDatumDao(datumEntityDao);

		final var userId = randomLong();
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var integrationId = randomLong();
		final var datumStreamId = randomLong();
		final var mappingId = randomLong();

		final var datumStream = new CloudDatumStreamConfiguration(userId, datumStreamId, now());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);

		final var exprProp = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 0,
				Instant.now());
		exprProp.setEnabled(true);
		exprProp.setPropertyName("out");
		exprProp.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp.setValueReference("""
				sum(latestMatching('/met/*', timestamp).![watts])
				""");

		final var datum = GeneralDatum.nodeDatum(nodeId, sourceId, now().truncatedTo(HOURS),
				new DatumSamples());

		final var filter = new BasicQueryFilter();
		filter.setStartDate(now().truncatedTo(HOURS).minus(1, HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));

		final ObjectDatumStreamMetadata streamMeta = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", datumStream.getKind(), datumStream.getObjectId(), "/met/foo",
				new String[] { "watts" }, null, null, null);

		final var fetchedDatum1 = new DatumEntity(streamMeta.getStreamId(), filter.getStartDate(),
				filter.getStartDate(),
				propertiesOf(new BigDecimal[] { BigDecimal.ONE }, null, null, null));
		final var fetchedDatum2 = new DatumEntity(streamMeta.getStreamId(),
				filter.getStartDate().plus(30, MINUTES), filter.getStartDate().plus(30, MINUTES),
				propertiesOf(new BigDecimal[] { BigDecimal.TWO }, null, null, null));

		final var fetchResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta.getStreamId(), streamMeta),
				Arrays.asList(fetchedDatum1, fetchedDatum2));

		given(datumEntityDao.findFiltered(any())).willReturn(fetchResults);

		// WHEN
		service.evaluateExpressions(datumStream, filter, List.of(exprProp), List.of(datum), mappingId,
				integrationId);

		// THEN
		// @formatter:off
		then(datumEntityDao).should().findFiltered(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("DatumStream kind passed in datum query")
			.returns( datumStream.getKind(), from(DatumCriteria::getObjectKind))
			.as("DatumStream object ID passed in datum query")
			.returns(datumStream.getObjectId(), from(DatumCriteria::getObjectId))
			.satisfies(f -> {
				and.then(f.getSourceIds())
					.as("Source IDs extracted from expression passed in datum query")
					.contains("/met/*")
					;
			})
			.as("QueryFilter startDate passed in datum query")
			.returns(filter.getStartDate(), from(DatumCriteria::getStartDate))
			.as("QueryFilter endDate +1ms passed in datum query")
			.returns(filter.getEndDate().plusMillis(1), from(DatumCriteria::getEndDate))
			.as("DatumStream user ID passed in datum query")
			.returns(userId, from(DatumCriteria::getUserId))
			;

		then(datumStreamMetadataCache).shouldHaveNoInteractions();
		then(datumStreamMappingDao).shouldHaveNoInteractions();
		then(datumEntityDao).shouldHaveNoMoreInteractions();

		and.then(datum.getSampleLong(exprProp.getPropertyType(), exprProp.getPropertyName()))
			.as("Datum populated with value from datum fetched from DAO")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

	@Test
	public void evaluateExpressions_latestFetch() {
		// GIVEN
		final var service = serviceWithExpressionSupport();
		service.setDatumDao(datumEntityDao);

		final var userId = randomLong();
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var integrationId = randomLong();
		final var datumStreamId = randomLong();
		final var mappingId = randomLong();

		final var datumStream = new CloudDatumStreamConfiguration(userId, datumStreamId, now());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);

		final var exprProp = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 0,
				Instant.now());
		exprProp.setEnabled(true);
		exprProp.setPropertyName("out");
		exprProp.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp.setValueReference("""
				sum({latest('/met/1', timestamp).watts, latest('/met/2', timestamp).watts})
				""");

		final var datum = GeneralDatum.nodeDatum(nodeId, sourceId, now().truncatedTo(HOURS),
				new DatumSamples());

		final var filter = new BasicQueryFilter();
		filter.setStartDate(now().truncatedTo(HOURS).minus(1, HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));

		final ObjectDatumStreamMetadata streamMeta1 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", datumStream.getKind(), datumStream.getObjectId(), "/met/1",
				new String[] { "watts" }, null, null, null);

		final ObjectDatumStreamMetadata streamMeta2 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", datumStream.getKind(), datumStream.getObjectId(), "/met/2",
				new String[] { "watts" }, null, null, null);

		final var fetchedDatum1 = new DatumEntity(streamMeta1.getStreamId(), filter.getStartDate(),
				filter.getStartDate(),
				propertiesOf(new BigDecimal[] { BigDecimal.ONE }, null, null, null));
		final var fetchedDatum2 = new DatumEntity(streamMeta1.getStreamId(),
				filter.getStartDate().plus(30, MINUTES), filter.getStartDate().plus(30, MINUTES),
				propertiesOf(new BigDecimal[] { BigDecimal.TWO }, null, null, null));

		final var fetchedDatum3 = new DatumEntity(streamMeta2.getStreamId(),
				filter.getStartDate().plus(30, MINUTES), filter.getStartDate().plus(30, MINUTES),
				propertiesOf(new BigDecimal[] { BigDecimal.TEN }, null, null, null));

		final var fetchResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta1.getStreamId(), streamMeta1, streamMeta2.getStreamId(), streamMeta2),
				Arrays.asList(fetchedDatum1, fetchedDatum2, fetchedDatum3));

		given(datumEntityDao.findFiltered(any())).willReturn(fetchResults);

		// WHEN
		service.evaluateExpressions(datumStream, filter, List.of(exprProp), List.of(datum), mappingId,
				integrationId);

		// THEN
		// @formatter:off
		then(datumEntityDao).should().findFiltered(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("DatumStream kind passed in datum query")
			.returns( datumStream.getKind(), from(DatumCriteria::getObjectKind))
			.as("DatumStream object ID passed in datum query")
			.returns(datumStream.getObjectId(), from(DatumCriteria::getObjectId))
			.satisfies(f -> {
				and.then(f.getSourceIds())
					.as("Source IDs extracted from expression passed in datum query")
					.contains("/met/1", "/met/2")
					;
			})
			.as("QueryFilter startDate passed in datum query")
			.returns(filter.getStartDate(), from(DatumCriteria::getStartDate))
			.as("QueryFilter endDate +1ms passed in datum query")
			.returns(filter.getEndDate().plusMillis(1), from(DatumCriteria::getEndDate))
			.as("DatumStream user ID passed in datum query")
			.returns(userId, from(DatumCriteria::getUserId))
			;

		then(datumStreamMetadataCache).shouldHaveNoInteractions();
		then(datumStreamMappingDao).shouldHaveNoInteractions();
		then(datumEntityDao).shouldHaveNoMoreInteractions();

		and.then(datum.getSampleLong(exprProp.getPropertyType(), exprProp.getPropertyName()))
			.as("Datum populated with SUM of latest datum fetched from DAO")
			.isEqualTo(12L)
			;
		// @formatter:on
	}

}
