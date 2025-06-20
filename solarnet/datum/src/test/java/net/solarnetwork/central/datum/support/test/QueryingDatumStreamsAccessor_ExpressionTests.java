/* ==================================================================
 * QueryingDatumStraemsAccessor_ExpressionTests.java - 12/06/2025 5:19:01 am
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

package net.solarnetwork.central.datum.support.test;

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatum.forStreamDatum;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.assertj.core.api.BDDAssertions.and;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.central.datum.support.BasicDatumStreamsAccessor;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link QueryingDatumStreamsAccessor} class when used in
 * expressions.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class QueryingDatumStreamsAccessor_ExpressionTests {

	private SpelExpressionService expressionService;
	private PathMatcher sourceIdPathMatcher;

	private List<ObjectDatum> allDatum;

	@BeforeEach
	public void setup() {
		expressionService = new SpelExpressionService();

		var pm = new AntPathMatcher();
		pm.setCachePatterns(false);
		pm.setCaseSensitive(false);
		sourceIdPathMatcher = pm;
	}

	private DatumStreamsAccessor abc(Long userId, Map<String, String> sourceIdToResources) {
		allDatum = new ArrayList<>(0);
		final ObjectDatumStreamMetadata parseMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);
		for ( Entry<String, String> e : sourceIdToResources.entrySet() ) {
			List<Datum> data = datumResourceToList(getClass(), e.getValue(),
					staticProvider(List.of(parseMeta)));

			ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, 1L, e.getKey(), null, new String[] { "wattHours" }, null);
			allDatum.addAll(
					data.stream()
							.map(d -> forStreamDatum(d, userId,
									nodeId(userId, meta.getSourceId(), d.getTimestamp()), parseMeta))
							.toList());
		}
		return new BasicDatumStreamsAccessor(sourceIdPathMatcher, allDatum);
	}

	private ObjectDatum datumAt(String source, Instant ts, List<ObjectDatum> list) {
		return list.stream()
				.filter(d -> source.equals(d.getSourceId()) && ts.compareTo(d.getTimestamp()) == 0)
				.findFirst().orElse(null);
	}

	@Test
	public void deltaSum() {
		// GIVEN
		final Long userId = randomLong();

		var datumStreamsAccessor = abc(userId, Map.of("A", "sample-data-A-01.csv", "inv/1",
				"sample-data-B-02.csv", "inv/2", "sample-data-C-02.csv"));

		final var datum = datumAt("A", Instant.parse("2017-07-04T09:00:00.000Z"), allDatum);

		final var root = new DatumExpressionRoot(userId, datum, null, null, null, datumStreamsAccessor,
				null, null, null, null);

		// WHEN
		BigDecimal result = expressionService.evaluateExpression("""
				wattHours + sum(datumAtMatching('inv/*', timestamp).![
					deltaAt(sourceId, timestamp, 'wattHours')
				])
				""", null, root, null, BigDecimal.class);

		// THEN
		and.then(result).as("Delta evaluated").isEqualByComparingTo(
				new BigDecimal(12476432001L).add(new BigDecimal(1500L)).add(new BigDecimal(25L)));
	}

	@Test
	public void deltaSum_dataGap_noC() {
		// GIVEN
		final Long userId = randomLong();

		var datumStreamsAccessor = abc(userId, Map.of("A", "sample-data-A-01.csv", "inv/1",
				"sample-data-B-02.csv", "inv/2", "sample-data-C-02.csv"));

		final var datum = datumAt("A", Instant.parse("2017-07-04T11:00:00.000Z"), allDatum);

		final var root = new DatumExpressionRoot(userId, datum, null, null, null, datumStreamsAccessor,
				null, null, null, null);

		// WHEN
		BigDecimal result = expressionService.evaluateExpression("""
				wattHours + sum(datumAtMatching('inv/*', timestamp).![
					deltaAt(sourceId, timestamp, 'wattHours')
				])
				""", null, root, null, BigDecimal.class);

		// THEN
		and.then(result).as("Delta evaluated")
				.isEqualByComparingTo(new BigDecimal(12476432707L).add(new BigDecimal(500L)));
	}

}
