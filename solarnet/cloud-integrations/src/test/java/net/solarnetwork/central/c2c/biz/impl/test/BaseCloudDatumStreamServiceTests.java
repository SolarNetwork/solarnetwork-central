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

import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.VIRTUAL_SOURCE_IDS_SETTING;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
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
		final var datumStreamId = randomLong();
		final var mappingId = randomLong();

		final var datumStream = new CloudDatumStreamConfiguration(userId, datumStreamId, Instant.now());

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
		service.evaluateExpressions(datumStream, List.of(exprProp), List.of(datum), mappingId,
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
		final var datumStreamId = randomLong();
		final var mappingId = randomLong();

		final var datumStream = new CloudDatumStreamConfiguration(userId, datumStreamId, Instant.now());

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
		service.evaluateExpressions(datumStream, List.of(exprProp), List.of(datum), mappingId,
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
	public void evaluateExpressions_virtualSourceId() {
		// GIVEN
		final var service = serviceWithExpressionSupport();

		final var userId = randomLong();
		final var nodeId = randomLong();
		final var integrationId = randomLong();
		final var datumStreamId = randomLong();
		final var mappingId = randomLong();

		// configure a single /gen/1 source ID
		final var siteId = randomString();
		final var virtualSourceId = "/{site}/gen/1";
		final var datumStream = new CloudDatumStreamConfiguration(userId, datumStreamId, Instant.now());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setServiceProps(Map.of(VIRTUAL_SOURCE_IDS_SETTING, virtualSourceId,
				PLACEHOLDERS_SERVICE_PROPERTY, Map.of("site", siteId)));

		final var exprProp1 = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 0,
				Instant.now());
		exprProp1.setEnabled(true);
		exprProp1.setPropertyName("v1");
		exprProp1.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp1.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp1.setValueReference("""
				sourceId.contains('/gen/') ? sum(latestMatching('/inv/*', timestamp).![w]) : null
				""");

		final var exprProp2 = new CloudDatumStreamPropertyConfiguration(userId, mappingId, 1,
				Instant.now());
		exprProp2.setEnabled(true);
		exprProp2.setPropertyName("v2");
		exprProp2.setPropertyType(DatumSamplesType.Instantaneous);
		exprProp2.setValueType(CloudDatumStreamValueType.SpelExpression);
		exprProp2.setValueReference("""
				sourceId.contains('/gen/') ? sum(latestMatching('/inv/*', timestamp).![a]) : null
				""");

		// create 2 datum streams, each with 2 datum
		final var inv1Datum1 = GeneralDatum.nodeDatum(nodeId, "/inv/1",
				Instant.now().truncatedTo(ChronoUnit.HOURS),
				new DatumSamples(Map.of("w", 123, "a", 321), null, null));
		final var inv1Datum2 = GeneralDatum.nodeDatum(nodeId, "/inv/1",
				inv1Datum1.getTimestamp().plusSeconds(1),
				new DatumSamples(Map.of("w", 234, "a", 432), null, null));

		final var inv2Datum1 = GeneralDatum.nodeDatum(nodeId, "/inv/2", inv1Datum1.getTimestamp(),
				new DatumSamples(Map.of("w", 345, "a", 543), null, null));
		final var inv2Datum2 = GeneralDatum.nodeDatum(nodeId, "/inv/2", inv1Datum2.getTimestamp(),
				new DatumSamples(Map.of("w", 456, "a", 654), null, null));

		// WHEN
		var result = service.evaluateExpressions(datumStream, List.of(exprProp1, exprProp2),
				List.of(inv1Datum1, inv1Datum2, inv2Datum1, inv2Datum2), mappingId, integrationId);

		// THEN
		then(nodeMetadataReadOnlyDao).shouldHaveNoMoreInteractions();
		then(datumStreamMetadataCache).shouldHaveNoInteractions();
		then(datumStreamMappingDao).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Input datum + (virtual datum per timestamp) returned")
			.hasSize(6)
			;
		and.then(result).element(0)
			.as("Datum order preserved")
			.isEqualTo(inv1Datum1)
			.as("Property 'w' unchanged")
			.returns(123, from(d -> d.getSampleInteger(Instantaneous, "w")))
			.as("Property 'out' not created on input datum")
			.returns(null, from(d -> d.getSampleInteger(Instantaneous, "out")))
			;
		and.then(result).element(1)
			.as("Datum order preserved")
			.isEqualTo(inv1Datum2)
			.as("Property 'w' unchanged")
			.returns(234, from(d -> d.getSampleInteger(Instantaneous, "w")))
			.as("Property 'out' not created on input datum")
			.returns(null, from(d -> d.getSampleInteger(Instantaneous, "out")))
			;
		and.then(result).element(2)
			.as("Datum order preserved")
			.isEqualTo(inv2Datum1)
			.as("Property 'w' unchanged")
			.returns(345, from(d -> d.getSampleInteger(Instantaneous, "w")))
			.as("Property 'out' not created on input datum")
			.returns(null, from(d -> d.getSampleInteger(Instantaneous, "out")))
			;
		and.then(result).element(3)
			.as("Datum order preserved")
			.isEqualTo(inv2Datum2)
			.as("Property 'w' unchanged")
			.returns(456, from(d -> d.getSampleInteger(Instantaneous, "w")))
			.as("Property 'out' not created on input datum")
			.returns(null, from(d -> d.getSampleInteger(Instantaneous, "out")))
			;

		and.then(result).element(4)
			.as("Virtual datum added for 1st timestamp")
			.returns(new DatumId(datumStream.getKind(), datumStream.getObjectId(), "/%s/gen/1".formatted(siteId), inv1Datum1.getTimestamp()),
					from(GeneralDatum::getId))
			.as("Samples are sum() of inv1d1 + inv1d2")
			.returns(new DatumSamples(Map.of("v1", 468, "v2", 864), null, null), from(GeneralDatum::getSamples))
			;
		and.then(result).element(5)
			.as("Virtual datum added for 2nd timestamp")
			.returns(new DatumId(datumStream.getKind(), datumStream.getObjectId(), "/%s/gen/1".formatted(siteId), inv1Datum2.getTimestamp()),
					from(GeneralDatum::getId))
			.as("Samples are sum() of inv1d2 + inv2d2")
			.returns(new DatumSamples(Map.of("v1", 690, "v2", 1086), null, null), from(GeneralDatum::getSamples))
			;
		// @formatter:on
	}

}
