/* ==================================================================
 * AuditingStreamDatumFilteredResultsProcessorTests.java - 1/11/2026 3:58:27 pm
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

package net.solarnetwork.central.query.support.test;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.CombinedAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.query.support.AuditingStreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for the {@link AuditingStreamDatumFilteredResultsProcessor} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AuditingStreamDatumFilteredResultsProcessorTests {

	private static final Long NODE_ID = 1L;
	private static final Long VIRTUAL_NODE_ID = 100L;
	private static final String SOURCE_ID = "real";
	private static final String VIRTUAL_SOURCE_ID = "virtual";
	private static final Instant AUDIT_DATE = Instant.parse("2026-06-01T00:00:00Z");

	@Mock
	private StreamDatumFilteredResultsProcessor delegate;

	@Mock
	private QueryAuditor auditor;

	private List<GeneralNodeDatumPK> audited;

	@BeforeEach
	public void setup() {
		audited = new ArrayList<>();
		given(auditor.getAuditClock()).willReturn(Clock.fixed(AUDIT_DATE, ZoneOffset.UTC));
	}

	private void givenAuditResultsRecorded() {
		willAnswer(invocation -> {
			Map<GeneralNodeDatumPK, Integer> results = invocation.getArgument(0);
			audited.addAll(results.keySet());
			return null;
		}).given(auditor).addNodeDatumAuditResults(any());
	}

	private static ObjectDatumStreamMetadata meta(Long nodeId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", ObjectDatumKind.Node, nodeId,
				sourceId, new String[] { "a" }, null, null);
	}

	private static StreamDatum datum(ObjectDatumStreamMetadata meta) {
		return new DatumEntity(meta.getStreamId(), AUDIT_DATE, null,
				DatumProperties.propertiesOf(null, null, null, null));
	}

	private static StreamDatum combinedDatum(ObjectDatumStreamMetadata meta, Instant ts,
			Set<NodeSourcePK> contributors) {
		return new CombinedAggregateDatumEntity(meta.getStreamId(), ts, Aggregation.Hour,
				DatumProperties.propertiesOf(null, null, null, null),
				DatumPropertiesStatistics.statisticsOf(null, null), contributors);
	}

	private void handleResults(ObjectDatumStreamMetadata meta, StreamDatum... datum) throws IOException {
		try (final var processor = new AuditingStreamDatumFilteredResultsProcessor(delegate, auditor)) {
			processor.start(null, null, null,
					singletonMap(METADATA_PROVIDER_ATTR, staticProvider(List.of(meta))));
			for ( StreamDatum d : datum ) {
				processor.handleResultItem(d);
				then(delegate).should().handleResultItem(d);
			}
		}
	}

	@Test
	public void plainResult() throws IOException {
		// GIVEN
		givenAuditResultsRecorded();

		final ObjectDatumStreamMetadata meta = meta(NODE_ID, SOURCE_ID);

		// WHEN
		handleResults(meta, datum(meta));

		// THEN
		// @formatter:off
		and.then(audited)
			.as("Result counted on its stream")
			.containsExactly(new GeneralNodeDatumPK(NODE_ID, AUDIT_DATE, SOURCE_ID))
			;
		// @formatter:on
	}

	@Test
	public void combined() throws IOException {
		// GIVEN
		givenAuditResultsRecorded();

		final ObjectDatumStreamMetadata meta = meta(VIRTUAL_NODE_ID, VIRTUAL_SOURCE_ID);

		// WHEN
		handleResults(meta,
				combinedDatum(meta, AUDIT_DATE,
						Set.of(new NodeSourcePK(NODE_ID, SOURCE_ID), new NodeSourcePK(2L, SOURCE_ID))),
				combinedDatum(meta, AUDIT_DATE.plusSeconds(3600),
						Set.of(new NodeSourcePK(NODE_ID, SOURCE_ID))));

		// THEN
		// @formatter:off
		and.then(audited)
			.as("Each result counted once on each contributing stream, and not on its virtual IDs")
			.containsExactlyInAnyOrder(
					new GeneralNodeDatumPK(NODE_ID, AUDIT_DATE, SOURCE_ID),
					new GeneralNodeDatumPK(2L, AUDIT_DATE, SOURCE_ID),
					new GeneralNodeDatumPK(NODE_ID, AUDIT_DATE, SOURCE_ID))
			;
		// @formatter:on
	}

	@Test
	public void combined_noContributors() throws IOException {
		// GIVEN
		final ObjectDatumStreamMetadata meta = meta(VIRTUAL_NODE_ID, VIRTUAL_SOURCE_ID);

		// WHEN
		handleResults(meta, combinedDatum(meta, AUDIT_DATE, Set.of()));

		// THEN
		then(auditor).should(never()).addNodeDatumAuditResults(any());
	}

	@Test
	public void combined_noMetadata() throws IOException {
		// GIVEN
		givenAuditResultsRecorded();

		final ObjectDatumStreamMetadata meta = meta(VIRTUAL_NODE_ID, VIRTUAL_SOURCE_ID);
		final StreamDatum d = combinedDatum(meta, AUDIT_DATE,
				Set.of(new NodeSourcePK(NODE_ID, SOURCE_ID)));

		try (final var processor = new AuditingStreamDatumFilteredResultsProcessor(delegate, auditor)) {

			// WHEN
			processor.start(null, null, null, null);
			processor.handleResultItem(d);

			// THEN
			// @formatter:off
			and.then(audited)
				.as("Contributors need no metadata to be counted")
				.containsExactly(new GeneralNodeDatumPK(NODE_ID, AUDIT_DATE, SOURCE_ID))
				;
			// @formatter:on
		}
	}

}
