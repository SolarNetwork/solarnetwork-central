/* ==================================================================
 * DatumExpressionRootTests.java - 13/11/2024 10:23:19 am
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

package net.solarnetwork.central.datum.domain.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;
import tools.jackson.databind.JsonNode;

/**
 * Test cases for the {@link DatumExpressionRoot} class.
 *
 * @author matt
 * @version 1.5
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DatumExpressionRootTests {

	@Mock
	private DatumStreamsAccessor datumStreamsAccessor;

	@Mock
	private HttpOperations httpOperations;

	@Mock
	private BiFunction<Long, String, byte[]> userSecretProvider;

	@Captor
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpHeaders> httpHeadersCaptor;

	private SpelExpressionService expressionService;

	@BeforeEach
	public void setup() {
		expressionService = new SpelExpressionService();
	}

	private DatumExpressionRoot createTestRoot(Long userId, Long nodeId, String sourceId) {
		return createTestRoot(userId, nodeId, sourceId, null, null, null);
	}

	private DatumExpressionRoot createTestRoot(Long userId, Long nodeId, String sourceId,
			DatumMetadataOperations metadata,
			Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider,
			BiFunction<DatumMetadataOperations, ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleProvider) {
		DatumSamples ds = new DatumSamples();
		ds.putSampleValue(Instantaneous, "a", 3);
		ds.putSampleValue(Instantaneous, "b", 5);
		ds.putSampleValue(Accumulating, "c", 7);
		ds.putSampleValue(Accumulating, "d", 9);
		GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(), ds);

		DatumSamples s = new DatumSamples();
		d.putSampleValue(Instantaneous, "b", 21);
		d.putSampleValue(Instantaneous, "c", 23);
		d.putSampleValue(Accumulating, "e", 25);
		d.putSampleValue(Accumulating, "f", 25);

		Map<String, Object> p = new HashMap<>();
		p.put("d", 31);
		p.put("c", 33);
		p.put("f", 35);
		p.put("g", 35);

		return new DatumExpressionRoot(userId, d, s, p, metadata, datumStreamsAccessor, metadataProvider,
				tariffScheduleProvider, httpOperations, userSecretProvider);
	}

	@Test
	public void metadata() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", "two");
		meta.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta.putInfoValue("deviceInfo", "Name", "Thingy");
		meta.putInfoValue("deviceInfo", "Capacity", 3000);

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId, meta, null, null);
		String result1 = expressionService.evaluateExpression("metadata()?.info?.b", null, root, null,
				String.class);
		String result1a = expressionService.evaluateExpression("metadata()?.info['b']", null, root, null,
				String.class);
		String result2 = expressionService.evaluateExpression("metadata('/m/b')", null, root, null,
				String.class);
		Integer result3 = expressionService.evaluateExpression(
				"metadata()?.getInfoNumber('deviceInfo', 'Capacity')", null, root, null, Integer.class);
		Integer result4 = expressionService.evaluateExpression("metadata('/pm/deviceInfo/Capacity')",
				null, root, null, Integer.class);

		// THEN
		and.then(result1).as("Metadata info traversal").isEqualTo("two");
		and.then(result1a).as("Metadata info traversal").isEqualTo("two");
		and.then(result2).as("Metadata info path traversal").isEqualTo("two");
		and.then(result3).as("Metadata property info traversal").isEqualTo(3000);
		and.then(result4).as("Metadata property info path traversal").isEqualTo(3000);
	}

	@Test
	public void nodeMetadata() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", "two");
		meta.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta.putInfoValue("deviceInfo", "Name", "Thingy");
		meta.putInfoValue("deviceInfo", "Capacity", 3000);

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId, null, (id) -> {
			if ( id != null && id.getKind() == ObjectDatumKind.Node && nodeId.equals(id.getObjectId())
					&& id.getSourceId().isEmpty() ) {
				return meta;
			}
			return null;
		}, null);
		String result1 = expressionService.evaluateExpression("nodeMetadata()?.info?.b", null, root,
				null, String.class);
		String result2 = expressionService.evaluateExpression("nodeMetadata('/m/b')", null, root, null,
				String.class);
		Integer result3 = expressionService.evaluateExpression(
				"nodeMetadata()?.getInfoNumber('deviceInfo', 'Capacity')", null, root, null,
				Integer.class);
		Integer result4 = expressionService.evaluateExpression("nodeMetadata('/pm/deviceInfo/Capacity')",
				null, root, null, Integer.class);

		// THEN
		and.then(result1).as("Node metadata info traversal").isEqualTo("two");
		and.then(result2).as("Node metadata info path traversal").isEqualTo("two");
		and.then(result3).as("Node metadata property info traversal").isEqualTo(3000);
		and.then(result4).as("Node metadata property info path traversal").isEqualTo(3000);
	}

	@Test
	public void latestDatum() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final GeneralDatum latestDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, 0)).willReturn(latestDatum);

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		Boolean hasResult = expressionService.evaluateExpression("hasLatest('%s')".formatted(sourceId),
				null, root, null, Boolean.class);

		Integer result = expressionService.evaluateExpression("latest('%s')?.foo".formatted(sourceId),
				null, root, null, Integer.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum evaluated")
				.isEqualTo(latestDatum.getSampleInteger(Instantaneous, "foo"));
	}

	@Test
	public void latestDatum_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum latestDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 0))
				.willReturn(latestDatum);

		// WHEN

		Boolean hasResult = expressionService.evaluateExpression(
				"hasLatest('%s', timestamp)".formatted(sourceId), null, root, null, Boolean.class);

		Integer result = expressionService.evaluateExpression(
				"latest('%s', timestamp)?.foo".formatted(sourceId), null, root, null, Integer.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum evaluated")
				.isEqualTo(latestDatum.getSampleInteger(Instantaneous, "foo"));
	}

	@Test
	public void latestProp() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final GeneralDatum latestDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, 0)).willReturn(latestDatum);

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		// WHEN
		final Integer result = expressionService.evaluateExpression(
				"latestProp('%s', 'foo')".formatted(sourceId), null, root, null, Integer.class);

		final Integer noDatum = expressionService.evaluateExpression(
				"latestProp('does not exist', 'foo')", null, root, null, Integer.class);

		final Integer fallback = expressionService.evaluateExpression(
				"latestProp('%s', 'nah', -1)".formatted(sourceId), null, root, null, Integer.class);

		// THEN
		and.then(result).as("Latest prop evaluated")
				.isEqualTo(latestDatum.getSampleInteger(Instantaneous, "foo"));
		and.then(noDatum).as("No datum latest prop evaluated to null").isNull();
		and.then(fallback).as("Unknown latest prop evaluated to fallback").isEqualTo(-1);
	}

	@Test
	public void latestProp_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum latestDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 0))
				.willReturn(latestDatum);

		// WHEN

		final Integer result = expressionService.evaluateExpression(
				"latestProp('%s', timestamp, 'foo')".formatted(sourceId), null, root, null,
				Integer.class);

		final Integer noDatum = expressionService.evaluateExpression(
				"latestProp('does not exist', timestamp)", null, root, null, Integer.class);

		final Integer fallback = expressionService.evaluateExpression(
				"latestProp('%s', timestamp, 'nah', -1)".formatted(sourceId), null, root, null,
				Integer.class);

		// THEN
		and.then(result).as("Latest datum evaluated")
				.isEqualTo(latestDatum.getSampleInteger(Instantaneous, "foo"));
		and.then(noDatum).as("No datum latest prop evaluated to null").isNull();
		and.then(fallback).as("Unknown latest prop evaluated to fallback").isEqualTo(-1);
	}

	@Test
	public void offsetDatum() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final int offset = randomInt();
		final GeneralDatum offsetDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, offset)).willReturn(offsetDatum);

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		Boolean hasResult = expressionService.evaluateExpression(
				"hasOffset('%s', %d)".formatted(sourceId, offset), null, root, null, Boolean.class);

		Integer result = expressionService.evaluateExpression(
				"offset('%s', %d)?.foo".formatted(sourceId, offset), null, root, null, Integer.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum evaluated")
				.isEqualTo(offsetDatum.getSampleInteger(Instantaneous, "foo"));
	}

	@Test
	public void offsetDatum_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final int offset = randomInt();
		final GeneralDatum offsetDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), offset))
				.willReturn(offsetDatum);

		// WHEN

		Boolean hasResult = expressionService.evaluateExpression(
				"hasOffset('%s', %d, timestamp)".formatted(sourceId, offset), null, root, null,
				Boolean.class);

		Integer result = expressionService.evaluateExpression(
				"offset('%s', %d, timestamp)?.foo".formatted(sourceId, offset), null, root, null,
				Integer.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum evaluated")
				.isEqualTo(offsetDatum.getSampleInteger(Instantaneous, "foo"));
	}

	@Test
	public void offsetProp() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final int offset = randomInt();
		final GeneralDatum offsetDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, offset)).willReturn(offsetDatum);

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		// WHEN
		final Integer result = expressionService.evaluateExpression(
				"offsetProp('%s', %d, 'foo')".formatted(sourceId, offset), null, root, null,
				Integer.class);

		final Integer noDatum = expressionService.evaluateExpression(
				"offsetProp('does not exist', 0, 'foo')", null, root, null, Integer.class);

		final Integer fallback = expressionService.evaluateExpression(
				"offsetProp('%s', %d, 'nah', -1)".formatted(sourceId, offset), null, root, null,
				Integer.class);

		// THEN
		and.then(result).as("Latest datum evaluated")
				.isEqualTo(offsetDatum.getSampleInteger(Instantaneous, "foo"));
		and.then(noDatum).as("Missing datum evaluated as null").isNull();
		and.then(fallback).as("Missing property evaluated to fallback").isEqualTo(-1);
	}

	@Test
	public void offsetProp_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final int offset = randomInt();
		final GeneralDatum offsetDatum = GeneralDatum.nodeDatum(nodeId, sourceId, Instant.now(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), offset))
				.willReturn(offsetDatum);

		// WHEN

		final Integer result = expressionService.evaluateExpression(
				"offsetProp('%s', %d, timestamp, 'foo')".formatted(sourceId, offset), null, root, null,
				Integer.class);

		final Integer noDatum = expressionService.evaluateExpression(
				"offsetProp('nah', 0, timestamp, 'foo')", null, root, null, Integer.class);

		final Integer fallback = expressionService.evaluateExpression(
				"offsetProp('%s', %d, timestamp, 'nah', -1)".formatted(sourceId, offset), null, root,
				null, Integer.class);

		// THEN
		and.then(result).as("Latest datum evaluated")
				.isEqualTo(offsetDatum.getSampleInteger(Instantaneous, "foo"));
		and.then(noDatum).as("Missing datum evaluated as null").isNull();
		and.then(fallback).as("Missing property evaluated to fallback").isEqualTo(-1);
	}

	@Test
	public void latestMatching() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, randomString(), now,
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, randomString(), now.minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offsetMatching(Node, nodeId, "*", 0)).willReturn(List.of(d1, d2));

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		Boolean hasResult = expressionService.evaluateExpression("hasLatestMatching('*')", null, root,
				null, Boolean.class);

		Long result = expressionService.evaluateExpression("sum(latestMatching('*').![foo])", null, root,
				null, Long.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum matching evaluated").isEqualTo(
				d1.getSampleLong(Instantaneous, "foo") + d2.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void latestMatching_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, randomString(), now,
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, randomString(), now.minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.offsetMatching(Node, nodeId, "*", root.getTimestamp(), 0))
				.willReturn(List.of(d1, d2));

		// WHEN

		Boolean hasResult = expressionService.evaluateExpression("hasLatestMatching('*', timestamp)",
				null, root, null, Boolean.class);

		Long result = expressionService.evaluateExpression("sum(latestMatching('*', timestamp).![foo])",
				null, root, null, Long.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum matching evaluated").isEqualTo(
				d1.getSampleLong(Instantaneous, "foo") + d2.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void offsetMatching() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, randomString(), now,
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, randomString(), now.minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final int offset = randomInt();
		given(datumStreamsAccessor.offsetMatching(Node, nodeId, "*", offset))
				.willReturn(List.of(d1, d2));

		// WHEN
		DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		Boolean hasResult = expressionService.evaluateExpression(
				"hasOffsetMatching('*', %d)".formatted(offset), null, root, null, Boolean.class);

		Long result = expressionService.evaluateExpression(
				"sum(offsetMatching('*', %d).![foo])".formatted(offset), null, root, null, Long.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum matching evaluated").isEqualTo(
				d1.getSampleLong(Instantaneous, "foo") + d2.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void offsetMatching_time() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, randomString(), now,
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, randomString(), now.minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final int offset = randomInt();
		given(datumStreamsAccessor.offsetMatching(Node, nodeId, "*", root.getTimestamp(), offset))
				.willReturn(List.of(d1, d2));

		// WHEN

		Boolean hasResult = expressionService.evaluateExpression(
				"hasOffsetMatching('*', %d, timestamp)".formatted(offset), null, root, null,
				Boolean.class);

		Long result = expressionService.evaluateExpression(
				"sum(offsetMatching('*', %d, timestamp).![foo])".formatted(offset), null, root, null,
				Long.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Latest datum matching evaluated").isEqualTo(
				d1.getSampleLong(Instantaneous, "foo") + d2.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void httpGet() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final String uri = "http://example.com/" + randomString();
		final var params = Map.of("foo", "b&a?r", "bim", 1);
		final var headers = Map.of("x-foo", "bar");

		final JsonNode res = JsonUtils.getObjectFromJSON("""
				{"yee":"haw"}
				""", JsonNode.class);
		final var httpRes = new Result<>(res);
		given(httpOperations.httpGet(eq(uri), eq(params), eq(headers), eq(JsonNode.class), eq(userId),
				any())).willReturn(httpRes);

		// WHEN
		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);
		Result<Map<String, Object>> result = root.httpGet(uri, params, headers);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.as("Json result returned as Map")
			.returns(Map.of("yee", "haw"), from(Result::getData))
			;
		// @formatter:on
	}

	@Test
	public void userSecret_data() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final String secretName = randomString();
		final byte[] secretValue = randomBytes();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		given(userSecretProvider.apply(eq(userId), eq(secretName))).willReturn(secretValue);

		// WHEN
		byte[] result = root.secretData(secretName);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Secret bytes returned")
			.isEqualTo(secretValue)
			;
		// @formatter:on
	}

	@Test
	public void userSecret_string() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final String secretName = randomString();
		final byte[] secretValue = randomBytes();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		given(userSecretProvider.apply(eq(userId), eq(secretName))).willReturn(secretValue);

		// WHEN
		String result = root.secret(secretName);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Secret bytes returned as UTF-8 string")
			.isEqualTo(new String(secretValue, UTF_8))
			;
		// @formatter:on
	}

	@Test
	public void datumAt() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final String otherSourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum datum = GeneralDatum.nodeDatum(nodeId, otherSourceId,
				Instant.now().plusSeconds(1), new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.at(Node, nodeId, otherSourceId, root.getTimestamp()))
				.willReturn(datum);

		// WHEN
		Boolean hasResult = expressionService.evaluateExpression(
				"hasDatumAt('%s', timestamp)".formatted(otherSourceId), null, root, null, Boolean.class);

		Integer result = expressionService.evaluateExpression(
				"datumAt('%s', timestamp)?.foo".formatted(otherSourceId), null, root, null,
				Integer.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Datum evaluated").isEqualTo(datum.getSampleInteger(Instantaneous, "foo"));
	}

	@Test
	public void datumAtMatching() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, randomString(), now,
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, randomString(), now.minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		given(datumStreamsAccessor.atMatching(Node, nodeId, "foo/*", root.getTimestamp()))
				.willReturn(List.of(d1, d2));

		// WHEN
		Boolean hasResult = expressionService.evaluateExpression(
				"hasDatumAtMatching('%s', timestamp)".formatted("foo/*"), null, root, null,
				Boolean.class);

		BigDecimal result = expressionService.evaluateExpression(
				"sum(datumAtMatching('%s', timestamp).![foo])".formatted("foo/*"), null, root, null,
				BigDecimal.class);

		// THEN
		and.then(hasResult).as("Does have result").isTrue();

		and.then(result).as("Datum matching evaluated")
				.isEqualByComparingTo(d1.getSampleBigDecimal(Instantaneous, "foo")
						.add(d2.getSampleBigDecimal(Instantaneous, "foo")));
	}

	@Test
	public void deltaAt() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId,
				d.getTimestamp().minusSeconds(1),
				new DatumSamples(Map.of("foo", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 1))
				.willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo')".formatted(sourceId), null, root, null, Long.class);

		and.then(result).as("Delta evaluated").isEqualTo(
				d.getSampleLong(Instantaneous, "foo") - d1.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void deltaAt_notFound() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(null);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo')".formatted(sourceId), null, root, null, Long.class);

		and.then(result).as("Delta evaluated to zero when no datum found").isEqualTo(0L);
	}

	@Test
	public void deltaAt_propNotFound() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("not", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo')".formatted(sourceId), null, root, null, Long.class);

		and.then(result).as("Delta evaluated to zero when datum property not found").isEqualTo(0L);
	}

	@Test
	public void deltaAt_fallback() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 1))
				.willReturn(null);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo')".formatted(sourceId), null, root, null, Long.class);

		and.then(result).as("Delta evaluated").isEqualTo(d.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void deltaAt_fallbackToZero() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 1))
				.willReturn(null);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo', true)".formatted(sourceId), null, root, null,
				Long.class);

		and.then(result).as("Delta evaluated").isEqualTo(0L);
	}

	@Test
	public void deltaAt_deltaPropNotFound_fallback() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId,
				d.getTimestamp().minusSeconds(1),
				new DatumSamples(Map.of("not", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 1))
				.willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo')".formatted(sourceId), null, root, null, Long.class);

		and.then(result).as("Delta evaluated as fallback when offset property not available")
				.isEqualTo(d.getSampleLong(Instantaneous, "foo"));
	}

	@Test
	public void deltaAt_deltaPropNotFound_fallbackToZero() {
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final DatumExpressionRoot root = createTestRoot(userId, nodeId, sourceId);

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, root.getTimestamp(),
				new DatumSamples(Map.of("foo", randomInt()), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId,
				d.getTimestamp().minusSeconds(1),
				new DatumSamples(Map.of("not", randomInt()), null, null));

		// get end datum
		given(datumStreamsAccessor.at(Node, nodeId, sourceId, root.getTimestamp())).willReturn(d);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, root.getTimestamp(), 1))
				.willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(
				"deltaAt('%s', timestamp, 'foo', true)".formatted(sourceId), null, root, null,
				Long.class);

		and.then(result).as("Delta evaluated as fallback when offset property not available")
				.isEqualTo(0L);
	}

	private static final String SIMPLE_VIRTUAL_METER = """
			has('irradiance') && hasOffset(1, timestamp) && offset(1, timestamp).props['irradianceHours'] != null
			? offset(1, timestamp).irradianceHours + round((secondsBetween(offset(1, timestamp).timestamp, timestamp) / 3600.0) * avg({offset(1, timestamp).props['irradiance'] ?: 0, irradiance}))
			: offset(1, timestamp).props['irradianceHours'] != null
			? offset(1, timestamp).irradianceHours
			: 0
			""";

	private static final String SIMPLE_VIRTUAL_METER2 = """
			has('irradiance') && offsetProp(1, timestamp, 'irradianceHours') != null
			? offset(1, timestamp).irradianceHours + round(
				(secondsBetween(offset(1, timestamp).timestamp, timestamp) / 3600.0)
					* avg({offsetProp(1, timestamp, 'irradiance', 0), irradiance}))
			: offsetProp(1, timestamp, 'irradianceHours', 0)
			""";

	@ParameterizedTest
	@ValueSource(strings = { SIMPLE_VIRTUAL_METER, SIMPLE_VIRTUAL_METER2 })
	public void simpleVirtualMeter_start(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now();

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(Map.of("irradiance", 20), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId, ts.minus(1, HOURS),
				new DatumSamples(Map.of("irradiance", 10), null, null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(), Map.of(),
				null, datumStreamsAccessor, null, null, httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, ts, 1)).willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("irradianceHours calculated at start as 0").isEqualTo(0L);
	}

	@ParameterizedTest
	@ValueSource(strings = { SIMPLE_VIRTUAL_METER, SIMPLE_VIRTUAL_METER2 })
	public void simpleVirtualMeter_middle(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now();

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(Map.of("irradiance", 20), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId, ts.minus(1, HOURS),
				new DatumSamples(Map.of("irradiance", 10), Map.of("irradianceHours", 5L), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(), Map.of(),
				null, datumStreamsAccessor, null, null, httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, ts, 1)).willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("irradianceHours is previous + (30/2) == 20").isEqualTo(20L);
	}

	@ParameterizedTest
	@ValueSource(strings = { SIMPLE_VIRTUAL_METER, SIMPLE_VIRTUAL_METER2 })
	public void simpleVirtualMeter_currNoIrradiance(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now();

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(Map.of("foo", 20), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId, ts.minus(1, HOURS),
				new DatumSamples(Map.of("irradiance", 10), Map.of("irradianceHours", 5L), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(), Map.of(),
				null, datumStreamsAccessor, null, null, httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, ts, 1)).willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("irradianceHours is previous as no irradiance available").isEqualTo(5L);
	}

	@ParameterizedTest
	@ValueSource(strings = { SIMPLE_VIRTUAL_METER, SIMPLE_VIRTUAL_METER2 })
	public void simpleVirtualMeter_prevNoIrradiance(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now();

		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(Map.of("irradiance", 20), null, null));
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, sourceId, ts.minus(1, HOURS),
				new DatumSamples(Map.of("foo", 10), Map.of("irradianceHours", 5L), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(), Map.of(),
				null, datumStreamsAccessor, null, null, httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, sourceId, ts, 1)).willReturn(d1);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("irradianceHours is previous + 20/2 == 15L").isEqualTo(15L);
	}

	// original hard-coded source IDs expression
	private static final String AGG_VIRTUAL_METER = """
			sourceId.contains('/GEN/')
			? sum({
				(hasLatest('/INV/1', timestamp) && latest('/INV/1', timestamp).props['wattHours'] != null)
					? latest('/INV/1', timestamp).props['wattHours']
					: 0,
				(hasLatest('/INV/2', timestamp) && latest('/INV/2', timestamp).props['wattHours'] != null)
					? latest('/INV/2', timestamp).props['wattHours']
					: 0,
				(hasLatest('/INV/3', timestamp) && latest('/INV/3', timestamp).props['wattHours'] != null)
					? latest('/INV/3', timestamp).props['wattHours']
					: 0
				})
			: null
			""";

	// take hard-coded list of source IDs and project latest wattHours prop from each using
	private static final String AGG_VIRTUAL_METER2 = """
			sourceId.contains('/GEN/')
			? sum({'/INV/1', '/INV/2', '/INV/3'}.![
				#root.hasLatest(#this, #root.timestamp) && #root.latest(#this, #root.timestamp).props['wattHours'] != null
					? #root.latest(#this, #root.timestamp).props['wattHours']
					: 0])
			: null
			""";

	// take hard-coded list of source IDs and project latest wattHours prop from each using latestProp()
	private static final String AGG_VIRTUAL_METER3 = """
			sourceId.contains('/GEN/')
			? sum({'/INV/1', '/INV/2', '/INV/3'}.![
				#root.latestProp(#this, #root.timestamp, 'wattHours', 0)
				])
			: null
			""";

	// take input parameter list of source IDs, filter by "INV", and project latest wattHours prop from each using latestProp()
	private static final String AGG_VIRTUAL_METER4 = """
			sourceId.contains('/GEN/')
			? sum(allSourceIds.?[#this.contains("INV")].![#root.latestProp(#this, #root.timestamp, 'wattHours', 0)])
			: null
			""";

	private static final List<String> DS_SOURCES = List.of("/GEN/100", "/INV/1", "/INV/2", "/INV/3",
			"/WEA/1", "/BAT/1");

	@ParameterizedTest
	@ValueSource(
			strings = { AGG_VIRTUAL_METER, AGG_VIRTUAL_METER2, AGG_VIRTUAL_METER3, AGG_VIRTUAL_METER4 })
	public void aggVirtualMeter_wrongSource(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final Instant ts = Instant.now();

		// our virtual source
		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, "/OTHER/1", ts,
				new DatumSamples(null, null, null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(),
				Map.of("allSourceIds", DS_SOURCES), null, datumStreamsAccessor, null, null,
				httpOperations, userSecretProvider);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("Other source resolves to null").isNull();
	}

	@ParameterizedTest
	@ValueSource(
			strings = { AGG_VIRTUAL_METER, AGG_VIRTUAL_METER2, AGG_VIRTUAL_METER3, AGG_VIRTUAL_METER4 })
	public void aggVirtualMeter_allSourcesAvailable(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final Instant ts = Instant.now();

		// our virtual source
		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, "/GEN/1", ts,
				new DatumSamples(null, null, null));

		// sources to aggregate
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, "/INV/1", ts,
				new DatumSamples(null, Map.of("wattHours", 20), null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, "/INV/2", ts.minus(1, MINUTES),
				new DatumSamples(null, Map.of("wattHours", 10), null));
		final GeneralDatum d3 = GeneralDatum.nodeDatum(nodeId, "/INV/3", ts.minus(1, SECONDS),
				new DatumSamples(null, Map.of("wattHours", 5), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(),
				Map.of("allSourceIds", DS_SOURCES), null, datumStreamsAccessor, null, null,
				httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/1", ts, 0)).willReturn(d1);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/2", ts, 0)).willReturn(d2);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/3", ts, 0)).willReturn(d3);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("wattHours calculated as sum").isEqualTo(35L);
	}

	@ParameterizedTest
	@ValueSource(
			strings = { AGG_VIRTUAL_METER, AGG_VIRTUAL_METER2, AGG_VIRTUAL_METER3, AGG_VIRTUAL_METER4 })
	public void aggVirtualMeter_someSourceMissing(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final Instant ts = Instant.now();

		// our virtual source
		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, "/GEN/1", ts,
				new DatumSamples(null, null, null));

		// sources to aggregate
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, "/INV/1", ts,
				new DatumSamples(null, Map.of("wattHours", 20), null));
		final GeneralDatum d3 = GeneralDatum.nodeDatum(nodeId, "/INV/3", ts.minus(1, SECONDS),
				new DatumSamples(null, Map.of("wattHours", 5), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(),
				Map.of("allSourceIds", DS_SOURCES), null, datumStreamsAccessor, null, null,
				httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/1", ts, 0)).willReturn(d1);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/2", ts, 0)).willReturn(null);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/3", ts, 0)).willReturn(d3);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("wattHours calculated as sum even with missing source").isEqualTo(25L);
	}

	@ParameterizedTest
	@ValueSource(
			strings = { AGG_VIRTUAL_METER, AGG_VIRTUAL_METER2, AGG_VIRTUAL_METER3, AGG_VIRTUAL_METER4 })
	public void aggVirtualMeter_somePropMissing(final String expr) {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final Instant ts = Instant.now();

		// our virtual source
		final GeneralDatum d = GeneralDatum.nodeDatum(nodeId, "/GEN/1", ts,
				new DatumSamples(null, null, null));

		// sources to aggregate
		final GeneralDatum d1 = GeneralDatum.nodeDatum(nodeId, "/INV/1", ts,
				new DatumSamples(null, Map.of("whatIsThis", 20), null));
		final GeneralDatum d2 = GeneralDatum.nodeDatum(nodeId, "/INV/2", ts.minus(1, MINUTES),
				new DatumSamples(null, Map.of("wattHours", 10), null));
		final GeneralDatum d3 = GeneralDatum.nodeDatum(nodeId, "/INV/3", ts.minus(1, SECONDS),
				new DatumSamples(null, Map.of("wattHours", 5), null));

		final DatumExpressionRoot root = new DatumExpressionRoot(userId, d, d.getSamples(),
				Map.of("allSourceIds", DS_SOURCES), null, datumStreamsAccessor, null, null,
				httpOperations, userSecretProvider);

		// get offset earlier datum
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/1", ts, 0)).willReturn(d1);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/2", ts, 0)).willReturn(d2);
		given(datumStreamsAccessor.offset(Node, nodeId, "/INV/3", ts, 0)).willReturn(d3);

		// WHEN
		Long result = expressionService.evaluateExpression(expr, null, root, null, Long.class);

		// THEN
		and.then(result).as("wattHours calculated as sum even when property missing").isEqualTo(15L);
	}

}
