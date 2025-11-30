/* ==================================================================
 * BasicInstructionsExpressionService_SpelTests.java - 27/11/2025 2:10:55 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.support.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomDecimal;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.biz.InstructionsExpressionService.USER_SECRET_TOPIC_ID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.Expression;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.dao.UserMetadataReadOnlyDao;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.user.dao.UserSecretAccessDao;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.central.user.support.BasicInstructionsExpressionService;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;

/**
 * Test cases for the {@link BasicInstructionsExpressionService} class using the
 * {@link SpelExpressionService}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class BasicInstructionsExpressionService_SpelTests {

	@Mock
	private SolarNodeMetadataReadOnlyDao nodeMetadataDao;

	@Mock
	private UserMetadataReadOnlyDao userMetadataDao;

	@Mock
	private HttpOperations httpOperations;

	@Mock
	private DatumStreamsAccessor datumStreamsAccessor;

	@Mock
	private UserSecretAccessDao userSecretAccessDao;

	private final MutableClock clock = MutableClock.of(now().truncatedTo(ChronoUnit.HOURS), UTC);

	private SimpleCache<String, Expression> expressionCache;
	private BasicInstructionsExpressionService service;

	@BeforeEach
	public void setup() {
		expressionCache = new SimpleCache<>("expression-cache");

		service = new BasicInstructionsExpressionService(new SpelExpressionService());
		service.setExpressionCache(expressionCache);
		service.setNodeMetadataDao(nodeMetadataDao);
		service.setUserMetadataDao(userMetadataDao);
		service.setUserSecretAccessDao(userSecretAccessDao);
	}

	private NodeInstruction instruction(Long nodeId, String topic) {
		var result = new NodeInstruction(topic, clock.instant(), nodeId);
		result.setId(randomLong());
		// TODO parameters
		return result;
	}

	@Test
	public void constructorProperties() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());

		// WHEN
		NodeInstructionExpressionRoot result = service.createNodeInstructionExpressionRoot(owner,
				instruction, parameters, datumStreamsAccessor, httpOperations);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Root instance provided")
			.isNotNull()
			.as("User ID from ownership returned")
			.returns(owner.getUserId(), from(NodeInstructionExpressionRoot::getUserId))
			.as("Node ID from ownership returned")
			.returns(owner.getNodeId(), from(NodeInstructionExpressionRoot::getNodeId))
			.as("Country from ownership returned")
			.returns(owner.getCountry(), from(NodeInstructionExpressionRoot::getCountry))
			.as("Time zone from ownership returned")
			.returns(owner.getZone(), from(NodeInstructionExpressionRoot::getZone))
			.as("Instruction ID from instruction")
			.returns(instruction.getId(), from(NodeInstructionExpressionRoot::getInstructionId))
			.as("Parameters merged with node ID")
			.satisfies(r -> {
				and.then(r.getInstruction())
					.as("Same instruction as provided to constructor")
					.isSameAs(instruction.getInstruction())
					;
				
				and.then(r.getParameters())
					.as("Parameters same as provided to constructor")
					.isEqualTo(parameters)
					.as("Copy of parameters created")
					.isNotSameAs(parameters)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void evaluate_nodeMetadata() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());

		final var nodeMeta = new GeneralDatumMetadata();
		nodeMeta.putInfoValue("setpoint", 0.5);

		final var nodeMetadata = new SolarNodeMetadata(owner.getNodeId());
		nodeMetadata.setMeta(nodeMeta);

		given(nodeMetadataDao.get(owner.getNodeId())).willReturn(nodeMetadata);

		// WHEN
		final NodeInstructionExpressionRoot root = service.createNodeInstructionExpressionRoot(owner,
				instruction, null, datumStreamsAccessor, httpOperations);
		final Double result = service.evaulateExpression("nodeMetadata('/m/setpoint') + 0.5", root, null,
				Double.class);

		// THEN
		// @formatter:off
		then(datumStreamsAccessor).shouldHaveNoInteractions();
		then(httpOperations).shouldHaveNoInteractions();
		then(nodeMetadataDao).shouldHaveNoMoreInteractions();
		then(userMetadataDao).shouldHaveNoInteractions();
		then(userSecretAccessDao).shouldHaveNoInteractions();
		
		and.then(result)
			.as("Expression evaulated, using node metadata value")
			.isEqualTo(1.0)
			;
		
		and.then(expressionCache)
			.as("Expression cached")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void evaluate_userMetadata() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());

		final var userMeta = new GeneralDatumMetadata();
		userMeta.putInfoValue("setpoint", 0.5);

		final var userMetadata = new UserMetadataEntity(owner.getUserId(), clock.instant());
		userMetadata.setMeta(userMeta);

		given(userMetadataDao.get(owner.getUserId())).willReturn(userMetadata);

		// WHEN
		final NodeInstructionExpressionRoot root = service.createNodeInstructionExpressionRoot(owner,
				instruction, null, datumStreamsAccessor, httpOperations);
		final Double result = service.evaulateExpression("userMetadata('/m/setpoint') + 0.25", root,
				null, Double.class);

		// THEN
		// @formatter:off
		then(datumStreamsAccessor).shouldHaveNoInteractions();
		then(httpOperations).shouldHaveNoInteractions();
		then(nodeMetadataDao).shouldHaveNoInteractions();
		then(userMetadataDao).shouldHaveNoMoreInteractions();
		then(userSecretAccessDao).shouldHaveNoInteractions();
		
		and.then(result)
			.as("Expression evaulated, using user metadata value")
			.isEqualTo(0.75)
			;
		
		and.then(expressionCache)
			.as("Expression cached")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void userSecret() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());

		final UserSecretEntity userSecret = new UserSecretEntity(
				new UserStringStringCompositePK(owner.getUserId(), USER_SECRET_TOPIC_ID, "foo"),
				randomBytes());
		given(userSecretAccessDao.getUserSecret(owner.getUserId(), USER_SECRET_TOPIC_ID, "foo"))
				.willReturn(userSecret);

		final String decryptedSecretValue = randomString();
		given(userSecretAccessDao.decryptSecretValue(same(userSecret)))
				.willReturn(decryptedSecretValue.getBytes(UTF_8));

		// WHEN
		final NodeInstructionExpressionRoot root = service.createNodeInstructionExpressionRoot(owner,
				instruction, null, datumStreamsAccessor, httpOperations);
		final String result = service.evaulateExpression("secret('foo')", root, null, String.class);

		// THEN
		// @formatter:off
		then(datumStreamsAccessor).shouldHaveNoInteractions();
		then(httpOperations).shouldHaveNoInteractions();
		then(nodeMetadataDao).shouldHaveNoInteractions();
		then(userMetadataDao).shouldHaveNoInteractions();
		then(userSecretAccessDao).shouldHaveNoMoreInteractions();
		
		and.then(result)
			.as("UserSecretBiz used with hard-coded topic to decrypt secret value from UTF-8 bytes")
			.isEqualTo(decryptedSecretValue)
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
	public void evaluate_nodeMetadataTariffSchedule() throws IOException {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());

		final var nodeMeta = new GeneralDatumMetadata();
		nodeMeta.putInfoValue("tariffs", "foo", TEST_TARIFF_CSV);

		final var nodeMetadata = new SolarNodeMetadata(owner.getNodeId());
		nodeMetadata.setMeta(nodeMeta);

		given(nodeMetadataDao.get(owner.getNodeId())).willReturn(nodeMetadata);

		// WHEN
		final NodeInstructionExpressionRoot root = service.createNodeInstructionExpressionRoot(owner,
				instruction, null, datumStreamsAccessor, httpOperations);
		final BigDecimal result = service.evaulateExpression("""
				(resolveTariffScheduleRate(nodeMetadata(), '/pm/tariffs/foo') ?: 1) * 100
				""", root, null, BigDecimal.class);

		// THEN
		// @formatter:off
		then(datumStreamsAccessor).shouldHaveNoInteractions();
		then(httpOperations).shouldHaveNoInteractions();
		then(nodeMetadataDao).shouldHaveNoMoreInteractions();
		then(userMetadataDao).shouldHaveNoInteractions();
		then(userSecretAccessDao).shouldHaveNoInteractions();


		final TariffSchedule schedule = TariffUtils.parseCsvTemporalRangeSchedule(Locale.ENGLISH, true, true, null,
				TEST_TARIFF_CSV);
		BigDecimal expectedResult = schedule.resolveTariff(LocalDateTime.now(UTC), null)
			.getRates().get("E").getAmount()
			.multiply(new BigDecimal(100));

		and.then(result)
			.as("Expression evaulated, using node metadata tariff schedule")
			.isEqualTo(expectedResult)
			;
		// @formatter:on
	}

	@Test
	public void evaluate_latestLocationDatumLookup() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");

		final NodeInstruction instruction = instruction(owner.getNodeId(), randomString());
		final String sourceId = randomString();
		final String streamName = randomString();
		final String tag = randomString();

		// look up stream meta
		final var streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Location, randomLong(), sourceId, new String[] { "price" }, null, null);
		given(datumStreamsAccessor.findStreams(ObjectDatumKind.Location, streamName, sourceId, tag))
				.willReturn(List.of(streamMeta));

		// find datum for stream
		final BigDecimal price = randomDecimal();
		final GeneralDatum datum = GeneralDatum.locationDatum(streamMeta.getObjectId(),
				streamMeta.getSourceId(), Instant.now(),
				new DatumSamples(Map.of("price", price), null, null));
		given(datumStreamsAccessor.offset(eq(streamMeta.getKind()), eq(streamMeta.getObjectId()),
				eq(streamMeta.getSourceId()), any(), eq(0))).willReturn(datum);

		// create datum expression root
		final var nodeMetadata = new SolarNodeMetadata(owner.getNodeId());

		given(nodeMetadataDao.get(owner.getNodeId())).willReturn(nodeMetadata);

		// WHEN
		final NodeInstructionExpressionRoot root = service.createNodeInstructionExpressionRoot(owner,
				instruction, null, datumStreamsAccessor, httpOperations);
		final BigDecimal result = service.evaulateExpression("""
				datumNear(findLocDatumStream('%s', '%s', '%s'), timestamp())?.price
				""".formatted(streamName, sourceId, tag), root, null, BigDecimal.class);

		// THEN
		// @formatter:off
		then(datumStreamsAccessor).shouldHaveNoMoreInteractions();
		then(httpOperations).shouldHaveNoInteractions();
		then(nodeMetadataDao).shouldHaveNoMoreInteractions();
		then(userMetadataDao).shouldHaveNoInteractions();
		then(userSecretAccessDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Expression evaulated, using datum property value")
			.isEqualTo(price)
			;
		// @formatter:on
	}

}
