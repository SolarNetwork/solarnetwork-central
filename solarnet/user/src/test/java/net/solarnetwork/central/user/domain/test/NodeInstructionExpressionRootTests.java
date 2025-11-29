/* ==================================================================
 * NodeInstructionExpressionRootTests.java - 20/11/2025 9:39:36 am
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

package net.solarnetwork.central.user.domain.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Test cases for the {@link NodeInstructionExpressionRoot} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class NodeInstructionExpressionRootTests {

	private static final Locale DEFAULT_METADATA_LOCALE = Locale.ENGLISH;

	@Mock
	private HttpOperations httpOperations;

	@Mock
	private DatumStreamsAccessor datumStreamsAccessor;

	private NodeInstructionExpressionRoot newRoot(SolarNodeOwnership owner, NodeInstruction instruction,
			Map<String, ?> parameters, DatumMetadataOperations userMetadata,
			DatumMetadataOperations nodeMetadata, Map<String, DatumMetadataOperations> sourceMetadatas,
			BiFunction<DatumMetadataOperations, String, TariffSchedule> tariffScheduleProvider,
			BiFunction<Long, String, byte[]> secretProvider) {
		return new NodeInstructionExpressionRoot(owner, instruction, parameters, datumStreamsAccessor,
				httpOperations, (userId) -> {
				// @formatter:off
					and.then(userId)
						.as("User ID must be owner user ID")
						.isEqualTo(owner.getUserId())
						;
					// @formatter:on
					return userMetadata;
				}, (id) -> {
				// @formatter:off
					and.then(id)
						.as("Object kind must be node")
						.returns(ObjectDatumKind.Node, from(ObjectDatumStreamMetadataId::getKind))
						.as("Object ID must be from owner node ID")
						.returns(owner.getNodeId(), from(ObjectDatumStreamMetadataId::getObjectId))
						;
					// @formatter:on
					return (id.getSourceId() == null ? nodeMetadata
							: sourceMetadatas != null ? sourceMetadatas.get(id.getSourceId()) : null);
				}, tariffScheduleProvider, secretProvider);
	}

	private NodeInstructionExpressionRoot newRoot(SolarNodeOwnership owner,
			NodeInstruction instruction) {
		return newRoot(owner, instruction, null, null, null, null, null, null);
	}

	private NodeInstructionExpressionRoot newRoot(SolarNodeOwnership owner, NodeInstruction instruction,
			Map<String, ?> parameters) {
		return newRoot(owner, instruction, parameters, null, null, null, null, null);
	}

	@Test
	public void idProperties() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// WHEN
		Long userId = root.getUserId();
		Long nodeId = root.getNodeId();
		Long instrId = root.getInstructionId();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(userId)
			.as("User ID from owner returned")
			.isEqualTo(owner.getUserId())
			;
		and.then(nodeId)
			.as("Node ID from owner returned")
			.isEqualTo(owner.getNodeId())
			;
		and.then(instrId)
			.as("Instruction ID from instruction returned")
			.isEqualTo(instruction.getId())
			;
		// @formatter:on		
	}

	@Test
	public void instructionProperty() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setInstruction(new Instruction(randomString(), Instant.now()));
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// WHEN
		Instruction result = root.getInstruction();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Instruction from NodeInstruction returned")
			.isSameAs(instruction.getInstruction())
			;
		// @formatter:on		
	}

	@Test
	public void parameters_null() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// WHEN
		Map<String, ?> params = root.getParameters();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(params)
			.as("Empty parameters created in constructor")
			.isEmpty()
			;
		// @formatter:on		
	}

	@Test
	public void parameters() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		final Map<String, Object> parameters = Map.of("foo", "bar");

		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, parameters);

		// WHEN
		Map<String, ?> params = root.getParameters();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(params)
			.as("Equal to parameters provided in constructor")
			.isEqualTo(parameters)
			.as("Copy of parameters made")
			.isNotSameAs(parameters)
			;
		// @formatter:on		
	}

	@Test
	public void country_null() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		// WHEN
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(root)
			.as("Null contry returned when owner has no country")
			.returns(null, from(NodeInstructionExpressionRoot::getCountry))
			;
		// @formatter:on		
	}

	@Test
	public void country() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ", "UTC");
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		// WHEN
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(root)
			.as("Owner contry returned")
			.returns(owner.getCountry(), from(NodeInstructionExpressionRoot::getCountry))
			;
		// @formatter:on		
	}

	@Test
	public void zone_null() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		// WHEN
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(root)
			.as("Zone defaults to UTC")
			.returns(ZoneOffset.UTC, from(NodeInstructionExpressionRoot::getZone))
			;
		// @formatter:on		
	}

	@Test
	public void zone() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong(), "NZ",
				"America/Los_Angeles");
		final NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());

		// WHEN
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(owner)
			.as("Zone ID parsed")
			.returns(ZoneId.of("America/Los_Angeles"), from(SolarNodeOwnership::getZone))
			;
		and.then(root)
			.as("Owner zone returned")
			.returns(owner.getZone(), from(NodeInstructionExpressionRoot::getZone))
			;
		// @formatter:on		
	}

	@Test
	public void resolveLocale_null() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);

		// WHEN
		Locale result = root.resolveLocale(null, "/pm/foo/bar");

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Null metadata input results in default locale")
			.isEqualTo(DEFAULT_METADATA_LOCALE)
			;
		// @formatter:on		
	}

	@Test
	public void resolveLocale() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction);
		final String localeId = "en-GB";

		// WHEN
		Locale result = root.resolveLocale(new GeneralDatumMetadata(null,
		// @formatter:off
				Map.of("foo", Map.of(
						"bar", "baz", 
						"bar-locale", localeId
						)))
				, "/pm/foo/bar")
				;
				// @formatter:on

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Matching path results in resolved locale")
			.isEqualTo(Locale.forLanguageTag(localeId))
			;
		// @formatter:on		
	}

	@Test
	public void userMetadata() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final GeneralDatumMetadata userMeta = new GeneralDatumMetadata(Map.of("foo", "bar"));
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, userMeta, null,
				null, null, null);

		// WHEN
		DatumMetadataOperations result = root.userMetadata();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("User metadata resolved")
			.isSameAs(userMeta)
			;
		// @formatter:on		
	}

	@Test
	public void nodeMetadata() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final GeneralDatumMetadata nodeMeta = new GeneralDatumMetadata(Map.of("foo", "bar"));
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, null, nodeMeta,
				null, null, null);

		// WHEN
		DatumMetadataOperations result = root.nodeMetadata();

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Node metadata resolved")
			.isSameAs(nodeMeta)
			;
		// @formatter:on		
	}

	@Test
	public void sourceMetadata() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final GeneralDatumMetadata srcMeta = new GeneralDatumMetadata(Map.of("foo", "bar"));
		final String sourceId = randomString();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, null, null,
				Map.of(sourceId, srcMeta), null, null);

		// WHEN
		DatumMetadataOperations result = root.sourceMetadata(sourceId);
		DatumMetadataOperations result2 = root.sourceMetadata("not available");

		// THEN
		then(httpOperations).shouldHaveNoInteractions();
		then(datumStreamsAccessor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Source metadata resolved")
			.isSameAs(srcMeta)
			;
		and.then(result2)
			.as("Source metadata for unsupported soruce ID returns null")
			.isNull()
			;
		// @formatter:on		
	}

	@Test
	public void secret_noProvider() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, null, null, null,
				null, null);

		// WHEN
		String result = root.secret("foo");

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null resolved when no provider available")
			.isNull()
			;
		// @formatter:on		
	}

	@Test
	public void secret() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final String secretKey = randomString();
		final String secretValue = randomString();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, null, null, null,
				null, (userId, key) -> {
				// @formatter:off
					and.then(userId)
						.as("User ID from owner is passed to secret provider")
						.isEqualTo(owner.getUserId())
						;
					and.then(key)
						.as("Given key passed to secret provider")
						.isEqualTo(secretKey)
						;
					// @formatter:on
					return secretValue.getBytes(UTF_8);
				});

		// WHEN
		String result = root.secret(secretKey);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Secret resolved as UTF-8 data")
			.isEqualTo(secretValue)
			;
		// @formatter:on		
	}

	@Test
	public void secretData() {
		// GIVEN
		final SolarNodeOwnership owner = ownershipFor(randomLong(), randomLong());
		final NodeInstruction instruction = new NodeInstruction();
		final String secretKey = randomString();
		final String secretValue = randomString();
		final NodeInstructionExpressionRoot root = newRoot(owner, instruction, null, null, null, null,
				null, (userId, key) -> {
				// @formatter:off
					and.then(userId)
						.as("User ID from owner is passed to secret provider")
						.isEqualTo(owner.getUserId())
						;
					and.then(key)
						.as("Given key passed to secret provider")
						.isEqualTo(secretKey)
						;
					// @formatter:on
					return secretValue.getBytes(UTF_8);
				});

		// WHEN
		byte[] result = root.secretData(secretKey);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Secret resolved")
			.isEqualTo(secretValue.getBytes(UTF_8))
			;
		// @formatter:on		
	}

}
