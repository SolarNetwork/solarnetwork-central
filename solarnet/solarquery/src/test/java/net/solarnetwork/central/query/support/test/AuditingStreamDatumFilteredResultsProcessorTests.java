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
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.query.support.AuditingStreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for the {@link AuditingStreamDatumFilteredResultsProcessor} class.
 *
 * @author matt
 * @version 1.0
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

	private void handleResults(StreamDatumFilterCommand filter, ObjectDatumStreamMetadata... metas)
			throws IOException {
		final var processor = new AuditingStreamDatumFilteredResultsProcessor(delegate, auditor, filter);
		processor.start(null, null, null,
				singletonMap(METADATA_PROVIDER_ATTR, staticProvider(List.of(metas))));
		for ( ObjectDatumStreamMetadata meta : metas ) {
			final StreamDatum d = datum(meta);
			processor.handleResultItem(d);
			then(delegate).should().handleResultItem(d);
		}
	}

	@Test
	public void noMappings() throws IOException {
		// GIVEN
		givenAuditResultsRecorded();

		// WHEN
		handleResults(new StreamDatumFilterCommand(), meta(NODE_ID, SOURCE_ID));

		// THEN
		// @formatter:off
		and.then(audited)
			.as("Result counted on its stream")
			.containsExactly(new GeneralNodeDatumPK(NODE_ID, AUDIT_DATE, SOURCE_ID))
			;
		// @formatter:on
	}

	@Test
	public void virtualIds() throws IOException {
		// GIVEN
		givenAuditResultsRecorded();

		final StreamDatumFilterCommand filter = new StreamDatumFilterCommand();
		filter.setNodeIdMappings(Map.of(VIRTUAL_NODE_ID, Set.of(NODE_ID)));
		filter.setSourceIdMappings(Map.of(VIRTUAL_SOURCE_ID, Set.of(SOURCE_ID)));

		// WHEN
		handleResults(filter, meta(VIRTUAL_NODE_ID, VIRTUAL_SOURCE_ID),
				meta(VIRTUAL_NODE_ID, "unmapped"), meta(2L, VIRTUAL_SOURCE_ID), meta(2L, "unmapped"));

		// THEN
		// @formatter:off
		and.then(audited)
			.as("Only the result with no virtual ID is counted")
			.containsExactly(new GeneralNodeDatumPK(2L, AUDIT_DATE, "unmapped"))
			;
		// @formatter:on
	}

}
