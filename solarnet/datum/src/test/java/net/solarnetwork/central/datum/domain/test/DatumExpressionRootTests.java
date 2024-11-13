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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Test cases for the {@link DatumExpressionRoot} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumExpressionRootTests {

	private SpelExpressionService expressionService;

	@BeforeEach
	public void setup() {
		expressionService = new SpelExpressionService();
	}

	private static DatumExpressionRoot createTestRoot(Long nodeId, String sourceId,
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

		return new DatumExpressionRoot(d, s, p, metadata, metadataProvider, tariffScheduleProvider);
	}

	@Test
	public void metadata() {
		// GIVEN
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", "two");
		meta.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta.putInfoValue("deviceInfo", "Name", "Thingy");
		meta.putInfoValue("deviceInfo", "Capacity", 3000);

		// WHEN
		DatumExpressionRoot root = createTestRoot(nodeId, sourceId, meta, null, null);
		String result1 = expressionService.evaluateExpression("metadata()?.info?.b", null, root, null,
				String.class);
		String result2 = expressionService.evaluateExpression("metadata('/m/b')", null, root, null,
				String.class);
		Integer result3 = expressionService.evaluateExpression(
				"metadata()?.getInfoNumber('deviceInfo', 'Capacity')", null, root, null, Integer.class);
		Integer result4 = expressionService.evaluateExpression("metadata('/pm/deviceInfo/Capacity')",
				null, root, null, Integer.class);

		// THEN
		then(result1).as("Metadata info traversal").isEqualTo("two");
		then(result2).as("Metadata info path traversal").isEqualTo("two");
		then(result3).as("Metadata property info traversal").isEqualTo(3000);
		then(result4).as("Metadata property info path traversal").isEqualTo(3000);
	}

	@Test
	public void nodeMetadata() {
		// GIVEN
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", "two");
		meta.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta.putInfoValue("deviceInfo", "Name", "Thingy");
		meta.putInfoValue("deviceInfo", "Capacity", 3000);

		// WHEN
		DatumExpressionRoot root = createTestRoot(nodeId, sourceId, null, (id) -> {
			if ( id != null && id.getKind() == ObjectDatumKind.Node && nodeId.equals(id.getObjectId())
					&& id.getSourceId() == null ) {
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
		then(result1).as("Node metadata info traversal").isEqualTo("two");
		then(result2).as("Node metadata info path traversal").isEqualTo("two");
		then(result3).as("Node metadata property info traversal").isEqualTo(3000);
		then(result4).as("Node metadata property info path traversal").isEqualTo(3000);
	}

}
