/* ==================================================================
 * AuditingStreamDatumFilterdResultsProcessor.java - 2/05/2022 7:03:16 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * {@link StreamDatumFilteredResultsProcessor} that audits the filtered results.
 * 
 * <p>
 * <b>Note</b> this class extends {@link AbstractMap} only as an optimization to
 * reduce the amount of garbage generated while auditing results. This class is
 * not meant to be used as a {@link Map} outside its own implementation.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class AuditingStreamDatumFilterdResultsProcessor extends AbstractMap<GeneralNodeDatumPK, Integer>
		implements StreamDatumFilteredResultsProcessor, Entry<GeneralNodeDatumPK, Integer> {

	private final StreamDatumFilteredResultsProcessor delegate;
	private final QueryAuditor auditor;
	private final Instant auditDate;
	private final Set<Entry<GeneralNodeDatumPK, Integer>> keySet;

	// the following is used to reduce the amount of garbage collected while auditing the response
	private final Map<UUID, GeneralNodeDatumPK> auditDatumKeys;

	private ObjectDatumStreamMetadataProvider metadataProvider;
	private GeneralNodeDatumPK currentKey;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 * @param auditor
	 *        the auditor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AuditingStreamDatumFilterdResultsProcessor(StreamDatumFilteredResultsProcessor delegate,
			QueryAuditor auditor) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.auditor = requireNonNullArgument(auditor, "auditor");
		this.auditDatumKeys = new HashMap<>(16);
		this.auditDate = Instant.now(auditor.getAuditClock());
		this.keySet = new AbstractSet<Map.Entry<GeneralNodeDatumPK, Integer>>() {

			@Override
			public Iterator<Entry<GeneralNodeDatumPK, Integer>> iterator() {
				return new Iterator<Map.Entry<GeneralNodeDatumPK, Integer>>() {

					private boolean hasNext = true;

					@Override
					public boolean hasNext() {
						return hasNext;
					}

					@Override
					public Entry<GeneralNodeDatumPK, Integer> next() {
						hasNext = false;
						return AuditingStreamDatumFilterdResultsProcessor.this;
					}
				};
			}

			@Override
			public int size() {
				return 1;
			}
		};
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public void flush() throws IOException {
		delegate.flush();
	}

	@Override
	public void start(Long totalResultCount, Integer startingOffset, Integer expectedResultCount,
			Map<String, ?> attributes) throws IOException {
		if ( attributes != null && (attributes
				.get(METADATA_PROVIDER_ATTR) instanceof ObjectDatumStreamMetadataProvider) ) {
			this.metadataProvider = (ObjectDatumStreamMetadataProvider) attributes
					.get(StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR);
		}
		delegate.start(totalResultCount, startingOffset, expectedResultCount, attributes);
	}

	@Override
	public void handleResultItem(StreamDatum d) throws IOException {
		if ( metadataProvider != null && d != null && d.getStreamId() != null ) {
			ObjectDatumStreamMetadata meta = metadataProvider.metadataForStreamId(d.getStreamId());
			if ( meta != null && meta.getKind() == ObjectDatumKind.Node ) {
				currentKey = auditDatumKeys.computeIfAbsent(d.getStreamId(), k -> {
					return new GeneralNodeDatumPK(meta.getObjectId(), auditDate, meta.getSourceId());
				});
				auditor.addNodeDatumAuditResults(this);
			}
		}
		delegate.handleResultItem(d);
	}

	// AbstractMap

	@Override
	public Set<Entry<GeneralNodeDatumPK, Integer>> entrySet() {
		return keySet;
	}

	// Entry

	@Override
	public GeneralNodeDatumPK getKey() {
		return currentKey;
	}

	@Override
	public Integer getValue() {
		return Integer.valueOf(1);
	}

	@Override
	public Integer setValue(Integer value) {
		throw new UnsupportedOperationException();
	}

}
