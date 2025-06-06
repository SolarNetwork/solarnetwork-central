/* ==================================================================
 * BasicCloudIntegrationsExpressionService_SpelTests.java - 13/11/2024 1:55:42 pm
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService.USER_SECRET_TOPIC_ID;
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenObject;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.Expression;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.support.HttpOperations;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.user.dao.UserSecretAccessDao;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Test cases for the {@link BasicCloudIntegrationsExpressionService} class
 * using the {@link SpelExpressionService}.
 *
 * @author matt
 * @version 1.2
 */
@ExtendWith(MockitoExtension.class)
public class BasicCloudIntegrationsExpressionService_SpelTests {

	@Mock
	SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private SolarNodeMetadataReadOnlyDao metadataDao;

	@Mock
	private HttpOperations httpOperations;

	@Mock
	private UserSecretAccessDao userSecretAccessDao;

	private SimpleCache<String, Expression> expressionCache;
	private SimpleCache<ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleCache;
	private BasicCloudIntegrationsExpressionService service;

	@BeforeEach
	public void setup() {
		expressionCache = new SimpleCache<>("expression-cache");
		tariffScheduleCache = new SimpleCache<>("tariff-schedule-cache");

		service = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao,
				new SpelExpressionService());
		service.setExpressionCache(expressionCache);
		service.setTariffScheduleCache(tariffScheduleCache);
		service.setMetadataDao(metadataDao);
		service.setUserSecretAccessDao(userSecretAccessDao);
	}

	private static GeneralDatum createNodeDatum(Long nodeId, String sourceId) {
		DatumSamples ds = new DatumSamples();
		ds.putSampleValue(Instantaneous, "a", 3);
		ds.putSampleValue(Instantaneous, "b", 5);
		ds.putSampleValue(Accumulating, "c", 7);
		ds.putSampleValue(Accumulating, "d", 9);
		return GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(), ds);
	}

	@Test
	public void createRoot() {
		// GIVEN
		final Long userId = randomLong();
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final Map<String, Object> parameters = Map.of("foo", "bar");

		// WHEN
		DatumExpressionRoot result = service.createDatumExpressionRoot(userId, integrationId, datum,
				parameters, null, null, null);

		// THEN
		// @formatter:off
		thenObject(result)
			.isNotNull()
			.satisfies(r -> {
				then(r.getDatum())
					.as("Provided datum given")
					.isSameAs(datum)
					;
				then(r.getSamples())
					.as("Datum samples given")
					.isSameAs(datum.getSamples())
					;
				then(r.getParameters())
					.as("Provided parameters given")
					.isSameAs(parameters)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void evaluate_nodeMetadata() {
		// GIVEN
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final var config = new CloudDatumStreamPropertyConfiguration(randomLong(), randomLong(), 0,
				Instant.now());
		config.setValueType(CloudDatumStreamValueType.SpelExpression);
		config.setValueReference("nodeMetadata('/m/setpoint') * (a * b + 1)");

		final var nodeMeta = new GeneralDatumMetadata();
		nodeMeta.putInfoValue("setpoint", 0.5);

		final var nodeMetadata = new SolarNodeMetadata(nodeId);
		nodeMetadata.setMeta(nodeMeta);

		given(metadataDao.get(nodeId)).willReturn(nodeMetadata);

		// WHEN
		final DatumExpressionRoot root = service.createDatumExpressionRoot(config.getUserId(),
				integrationId, datum, parameters, null, null, null);
		final Double result = service.evaluateDatumPropertyExpression(config, root, null, Double.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Expression evaulated, using node metadata value")
			.isEqualTo(8.0)
			;
		// @formatter:on
	}

	private static final String TEST_TARIFF_CSV = """
			Month,Day Range,Day of Week Range,Hour of Day Range,E
			January-December,,Mon-Fri,0-8,10.48
			January-December,,Mon-Fri,8-24,11.00
			January-December,,Sat-Sun,0-8,9.19
			January-December,,Sat-Sun,8-24,11.21
			""";

	@Test
	public void evaluate_nodeMetadataTariffSchedule() {
		// GIVEN
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final String tariffSchedulePath = "/pm/tariffs/foo";
		final var config = new CloudDatumStreamPropertyConfiguration(randomLong(), randomLong(), 0,
				Instant.now());
		config.setValueType(CloudDatumStreamValueType.SpelExpression);
		config.setValueReference("(resolveTariffScheduleRate(nodeMetadata(), '%s') ?: 1) * a"
				.formatted(tariffSchedulePath));

		final var nodeMeta = new GeneralDatumMetadata();
		nodeMeta.putInfoValue("setpoint", 0.5);
		nodeMeta.putInfoValue("tariffs", "foo", TEST_TARIFF_CSV);

		final var nodeMetadata = new SolarNodeMetadata(nodeId);
		nodeMetadata.setMeta(nodeMeta);

		given(metadataDao.get(nodeId)).willReturn(nodeMetadata);

		// WHEN
		final DatumExpressionRoot root = service.createDatumExpressionRoot(config.getUserId(),
				integrationId, datum, parameters, null, null, null);
		final BigDecimal result = service.evaluateDatumPropertyExpression(config, root, null,
				BigDecimal.class);

		// THEN
		// TariffSchedule will have been parsed
		TariffSchedule schedule = tariffScheduleCache
				.get(new ObjectDatumStreamMetadataId(ObjectDatumKind.Node, nodeId, tariffSchedulePath));
		// @formatter:off
		then(schedule)
			.as("Schedule parsed from node metadata and cached")
			.isNotNull()
			;

		BigDecimal expectedResult = schedule.resolveTariff(LocalDateTime.now(UTC), null)
			.getRates().get("E").getAmount()
			.multiply(new BigDecimal(3));

		then(result)
			.as("Expression evaulated, using node metadata tariff schedule")
			.isEqualTo(expectedResult)
			;
		// @formatter:on
	}

	@Test
	public void evaluate_nodeMetadataTariffSchedule_noSchedule() {
		// GIVEN
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final String tariffSchedulePath = "/pm/tariffs/foo";
		final var config = new CloudDatumStreamPropertyConfiguration(randomLong(), randomLong(), 0,
				Instant.now());
		config.setValueType(CloudDatumStreamValueType.SpelExpression);
		config.setValueReference("(resolveTariffScheduleRate(nodeMetadata(), '%s') ?: 1) * a"
				.formatted(tariffSchedulePath));

		final var nodeMeta = new GeneralDatumMetadata();
		nodeMeta.putInfoValue("setpoint", 0.5);

		final var nodeMetadata = new SolarNodeMetadata(nodeId);
		nodeMetadata.setMeta(nodeMeta);

		given(metadataDao.get(nodeId)).willReturn(nodeMetadata);

		// WHEN
		final DatumExpressionRoot root = service.createDatumExpressionRoot(config.getUserId(),
				integrationId, datum, parameters, null, null, null);
		final BigDecimal result = service.evaluateDatumPropertyExpression(config, root, null,
				BigDecimal.class);

		// THEN
		// TariffSchedule will have been parsed
		TariffSchedule schedule = tariffScheduleCache
				.get(new ObjectDatumStreamMetadataId(ObjectDatumKind.Node, nodeId, tariffSchedulePath));
		// @formatter:off
		then(schedule)
			.as("No schedule parsed from node metadata and cached")
			.isNull()
			;

		BigDecimal expectedResult = new BigDecimal(3);

		then(result)
			.as("Expression evaulated, using node metadata tariff schedule")
			.isEqualTo(expectedResult)
			;
		// @formatter:on
	}

	@Test
	public void nodeTz() {
		// GIVEN
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final BasicSolarNodeOwnership nodeOwnership = BasicSolarNodeOwnership.privateOwnershipFor(nodeId,
				randomLong(), "NZ", "Pacific/Auckland");
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(nodeOwnership);

		final var config = new CloudDatumStreamPropertyConfiguration(randomLong(), randomLong(), 0,
				Instant.now());
		config.setValueType(CloudDatumStreamValueType.SpelExpression);
		config.setValueReference("now(node.zone)");

		final Map<String, Object> parameters = Map.of("foo", "bar");

		// WHEN
		final DatumExpressionRoot root = service.createDatumExpressionRoot(config.getUserId(),
				integrationId, datum, parameters, null, null, null);

		final LocalDateTime start = LocalDateTime.now(nodeOwnership.getZone());
		final LocalDateTime result = service.evaluateDatumPropertyExpression(config, root, null,
				LocalDateTime.class);

		// THEN
		then(ChronoUnit.SECONDS.between(start, result)).isLessThanOrEqualTo(2);
	}

	@Test
	public void userSecret() {
		// GIVEN
		final Long integrationId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final GeneralDatum datum = createNodeDatum(nodeId, sourceId);

		final var config = new CloudDatumStreamPropertyConfiguration(randomLong(), randomLong(), 0,
				Instant.now());
		config.setValueType(CloudDatumStreamValueType.SpelExpression);
		config.setValueReference("secret('foo')");

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final UserSecretEntity userSecret = new UserSecretEntity(
				new UserStringStringCompositePK(config.getUserId(), USER_SECRET_TOPIC_ID, "foo"),
				randomBytes());
		given(userSecretAccessDao.getUserSecret(config.getUserId(), USER_SECRET_TOPIC_ID, "foo"))
				.willReturn(userSecret);

		final String decryptedSecretValue = randomString();
		given(userSecretAccessDao.decryptSecretValue(same(userSecret)))
				.willReturn(decryptedSecretValue.getBytes(UTF_8));

		// WHEN
		final DatumExpressionRoot root = service.createDatumExpressionRoot(config.getUserId(),
				integrationId, datum, parameters, null, null, null);
		final String result = service.evaluateDatumPropertyExpression(config, root, null, String.class);

		// THEN
		then(result)
				.as("UserSecretBiz used with hard-coded topic to decrypt secret value from UTF-8 bytes")
				.isEqualTo(decryptedSecretValue);
	}

}
