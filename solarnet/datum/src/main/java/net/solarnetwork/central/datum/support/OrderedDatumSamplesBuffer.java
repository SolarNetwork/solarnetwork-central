/* ==================================================================
 * OrderedDatumSamplesBuffer.java - 6/06/2026 6:41:25 am
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

package net.solarnetwork.central.datum.support;

import static net.solarnetwork.central.datum.domain.DatumValidationType.TIME_GAP_VALIDATION_TYPE;
import static net.solarnetwork.central.domain.CommonUserEvents.CORRELATION_ID_DATA_KEY;
import static net.solarnetwork.central.domain.CommonUserEvents.DURATION_DATA_KEY;
import static net.solarnetwork.util.CollectionUtils.getMapLong;
import static net.solarnetwork.util.CollectionUtils.getMapString;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.domain.datum.BasicDatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * An ordered collection of datum samples, grouped by stream identity.
 *
 * @author matt
 * @version 1.0
 */
public class OrderedDatumSamplesBuffer {

	private final SortedMap<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> buffer;
	private final Function<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> streamBufferCreator;
	private final SortedMap<DatumStreamIdentity, SortedMap<Instant, List<DatumAuxiliaryRecord>>> auxiliary;
	private final Function<DatumStreamIdentity, SortedMap<Instant, List<DatumAuxiliaryRecord>>> streamAuxiliaryCreator;
	private final Function<Instant, List<DatumAuxiliaryRecord>> datumAuxiliaryCreator;
	private final @Nullable Lock lock;

	/**
	 * Get the least timestamp from a collection.
	 *
	 * @param timestamps
	 *        the timestamps
	 * @return the least timestamp, or {@code null} if {@code timestamps} is
	 *         empty
	 */
	public static @Nullable Instant leastTimestamp(Collection<Instant> timestamps) {
		Instant result = null;
		for ( Instant ts : timestamps ) {
			if ( result == null || result.compareTo(ts) > 0 ) {
				result = ts;
			}
		}
		return result;
	}

	/**
	 * Get the greatest timestamp from a collection.
	 *
	 * @param timestamps
	 *        the timestamps
	 * @return the greatest timestamp, or {@code null} if {@code timestamps} is
	 *         empty
	 */
	public static @Nullable Instant greatestTimestamp(Collection<Instant> timestamps) {
		Instant result = null;
		for ( Instant ts : timestamps ) {
			if ( result == null || result.compareTo(ts) < 0 ) {
				result = ts;
			}
		}
		return result;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * {@link TreeMap} will be used, and concurrent mode will be disabled.
	 * </p>
	 */
	public OrderedDatumSamplesBuffer() {
		this(false);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * {@link TreeMap} will be used.
	 * </p>
	 *
	 * @param concurrent
	 *        {@code true} to use locking for concurrent access
	 */
	public OrderedDatumSamplesBuffer(boolean concurrent) {
		this(new TreeMap<>(), _ -> new TreeMap<>(), new TreeMap<>(), _ -> new TreeMap<>(),
				_ -> new ArrayList<>(2), concurrent);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * For a thread-safe instance {@code buffer} could be
	 * {@link ConcurrentSkipListMap} and {@code streamBufferCreator} should then
	 * return instances of that as well.
	 * </p>
	 *
	 * @param buffer
	 *        the buffer to use
	 * @param streamBufferCreator
	 *        the stream-buffer creator function to use
	 * @param auxiliary
	 *        the auxiliary buffer to use
	 * @param streamAuxiliaryCreator
	 *        the stream-auxiliary creator function to use
	 * @param datumAuxiliaryCreator
	 *        the datum-auxiliary creator function to use
	 * @param concurrent
	 *        {@code true} to use locking for concurrent access
	 */
	public OrderedDatumSamplesBuffer(
			SortedMap<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> buffer,
			Function<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> streamBufferCreator,
			SortedMap<DatumStreamIdentity, SortedMap<Instant, List<DatumAuxiliaryRecord>>> auxiliary,
			Function<DatumStreamIdentity, SortedMap<Instant, List<DatumAuxiliaryRecord>>> streamAuxiliaryCreator,
			Function<Instant, List<DatumAuxiliaryRecord>> datumAuxiliaryCreator, boolean concurrent) {
		super();
		this.buffer = requireNonNullArgument(buffer, "buffer");
		this.streamBufferCreator = requireNonNullArgument(streamBufferCreator, "streamBufferCreator");
		this.auxiliary = requireNonNullArgument(auxiliary, "auxiliary");
		this.streamAuxiliaryCreator = requireNonNullArgument(streamAuxiliaryCreator,
				"streamAuxiliaryCreator");
		this.datumAuxiliaryCreator = requireNonNullArgument(datumAuxiliaryCreator,
				"datumAuxiliaryCreator");
		this.lock = (concurrent ? new ReentrantLock() : null);
	}

	/**
	 * Get the stream buffer for a specific stream identity.
	 *
	 * @param streamIdent
	 *        the stream identity
	 * @return the stream buffer, or {@code null} if one does not exist
	 */
	public @Nullable SortedMap<Instant, DatumSamples> streamBuffer(DatumStreamIdentity streamIdent) {
		return buffer.get(streamIdent);
	}

	/**
	 * Get or create the samples for a specific timestamp.
	 *
	 * @param streamIdent
	 *        the stream identity
	 * @param timestamp
	 *        the timestamp
	 * @return the samples instance
	 */
	public DatumSamples getOrCreate(DatumStreamIdentity streamIdent, Instant timestamp) {
		return getOrCreate(streamIdent, timestamp, null);
	}

	/**
	 * Get or create the samples for a specific timestamp.
	 *
	 * @param streamIdent
	 *        the stream identity
	 * @param timestamp
	 *        the timestamp
	 * @param isNew
	 *        a mutable boolean, will be set to {@code true} if a new
	 *        {@link DatumSamples} instance was created, or {@code false} if an
	 *        existing instance was returned
	 * @return the samples instance
	 */
	public DatumSamples getOrCreate(DatumStreamIdentity streamIdent, Instant timestamp,
			@Nullable MutableBoolean isNew) {
		if ( lock != null ) {
			lock.lock();
		}
		try {
			final SortedMap<Instant, DatumSamples> stream = buffer.computeIfAbsent(streamIdent,
					streamBufferCreator);
			if ( isNew != null ) {
				isNew.setFalse();
			}
			return stream.computeIfAbsent(timestamp, _ -> {
				if ( isNew != null ) {
					isNew.setTrue();
				}
				return new DatumSamples();
			});
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	/**
	 * Remove the samples for a timestamp.
	 *
	 * @param streamIdent
	 *        the stream identity
	 * @param timestamp
	 *        the timestamp to remove
	 * @param instance
	 *        an optional instance to match; the timestamp will only be removed
	 *        if it is currently associated with this instance
	 */
	public void removeTimestamp(DatumStreamIdentity streamIdent, Instant timestamp,
			@Nullable DatumSamples instance) {
		if ( lock != null ) {
			lock.lock();
		}
		try {
			final SortedMap<Instant, DatumSamples> stream = buffer.get(streamIdent);
			if ( stream != null ) {
				if ( instance != null ) {
					stream.remove(timestamp, instance);
				} else {
					stream.remove(timestamp);
				}
			}
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	/**
	 * Get the next-earlier timestamp for a given stream timestamp.
	 *
	 * @param streamIdent
	 *        the stream identity
	 * @param timestamp
	 *        the timestamp to get the next-earlier one for
	 * @return the next earlier timestamp within the stream, or {@code null} if
	 *         one is not available
	 */
	public @Nullable Instant previousTimestamp(DatumStreamIdentity streamIdent, Instant timestamp) {
		if ( lock != null ) {
			lock.lock();
		}
		try {
			final SortedMap<Instant, DatumSamples> stream = buffer.get(streamIdent);
			if ( stream == null || stream.isEmpty() ) {
				return null;
			}
			final SortedMap<Instant, DatumSamples> headMap = stream.headMap(timestamp);
			if ( headMap.isEmpty() ) {
				return null;
			}
			return headMap.lastKey();
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	/**
	 * Get a list of all datum in this buffer.
	 *
	 * <p>
	 * Only non-empty samples will be turned into datum instances.
	 * </p>
	 *
	 * @param <T>
	 *        the type of datum being created
	 * @param datumCreator
	 *        a function to create datum out of an identity and samples
	 * @return the list, never {@code null}
	 */
	public <T extends Datum> List<T> datum(BiFunction<DatumIdentity, DatumSamples, T> datumCreator) {
		final List<T> result = new ArrayList<>(16);
		if ( lock != null ) {
			lock.lock();
		}
		try {
			for ( Entry<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> streamEntry : buffer
					.entrySet() ) {
				final DatumStreamIdentity streamIdent = streamEntry.getKey();
				for ( Entry<Instant, DatumSamples> datumEntry : streamEntry.getValue().entrySet() ) {
					DatumSamples s = datumEntry.getValue();
					if ( s.isEmpty() ) {
						continue;
					}
					result.add(datumCreator.apply(streamIdent.datumIdentity(datumEntry.getKey()), s));
				}
			}
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
		return result;
	}

	/**
	 * Get the greatest timestamp per stream.
	 *
	 * @return the greatest timestamp per stream
	 */
	public SortedMap<DatumStreamIdentity, Instant> greatestTimestampPerStream() {
		final SortedMap<DatumStreamIdentity, Instant> result = new TreeMap<>();
		if ( lock != null ) {
			lock.lock();
		}
		try {
			for ( Entry<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> e : buffer.entrySet() ) {
				if ( !e.getValue().isEmpty() ) {
					result.put(e.getKey(), e.getValue().lastKey());
				}
			}
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
		return result;
	}

	/**
	 * Add auxiliary records to a datum stream.
	 *
	 * @param streamId
	 *        the stream ID to add the records to
	 * @param records
	 *        the records to add
	 */
	public void addAuxiliary(DatumStreamIdentity streamId, List<DatumAuxiliaryRecord> records) {
		if ( records == null || records.isEmpty() ) {
			return;
		}
		if ( lock != null ) {
			lock.lock();
		}
		try {
			final SortedMap<Instant, List<DatumAuxiliaryRecord>> streamMap = auxiliary
					.computeIfAbsent(streamId, streamAuxiliaryCreator);
			for ( DatumAuxiliaryRecord record : records ) {
				streamMap.computeIfAbsent(record.getTimestamp(), datumAuxiliaryCreator).add(record);
			}
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	private static final String TIME_GAP_DURATION_META_PATH = "/pm/%s/%s"
			.formatted(DatumValidationType.TIME_GAP_VALIDATION_TYPE, DURATION_DATA_KEY);

	private static final String TIME_GAP_CORRELATION_ID_META_PATH = "/pm/%s/%s"
			.formatted(DatumValidationType.TIME_GAP_VALIDATION_TYPE, CORRELATION_ID_DATA_KEY);

	/**
	 * Test if any auxiliary records are available.
	 *
	 * @return {@code true} if {@link #auxiliary()} would return a non-empty
	 *         list.
	 *         </p>
	 */
	public boolean hasAuxiliary() {
		if ( lock != null ) {
			lock.lock();
		}
		try {
			for ( SortedMap<Instant, List<DatumAuxiliaryRecord>> streamRecords : auxiliary.values() ) {
				for ( List<DatumAuxiliaryRecord> tsRecords : streamRecords.values() ) {
					if ( !tsRecords.isEmpty() ) {
						return true;
					}
				}
			}
			return false;
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	/**
	 * Get an optional auxiliary list.
	 *
	 * @return the auxiliary returned by {@link #auxiliary()}, or {@code null}
	 *         if that returns an empty list
	 */
	public @Nullable List<DatumAuxiliaryRecord> auxiliaryOrNull() {
		final List<DatumAuxiliaryRecord> result = auxiliary();
		return (!result.isEmpty() ? result : null);
	}

	/**
	 * Get the list of available auxiliary records.
	 *
	 * <p>
	 * Multiple {@code Mark} auxiliary records per datum identity will be
	 * consolidated into a single auxiliary record.
	 * </p>
	 *
	 * @return the list of auxiliary records, never {@code null}
	 */
	public List<DatumAuxiliaryRecord> auxiliary() {
		if ( lock != null ) {
			lock.lock();
		}
		if ( auxiliary.isEmpty() ) {
			return List.of();
		}
		final List<DatumAuxiliaryRecord> result = new ArrayList<>(2);
		try {
			for ( Entry<DatumStreamIdentity, SortedMap<Instant, List<DatumAuxiliaryRecord>>> streamEntry : auxiliary
					.entrySet() ) {
				final DatumStreamIdentity streamId = streamEntry.getKey();
				final SortedMap<Instant, List<DatumAuxiliaryRecord>> streamAux = streamEntry.getValue();
				if ( streamAux.isEmpty() ) {
					continue;
				}

				// keep track of de-duplicated time-gap record events
				final Set<String> correlationIdsToIgnore = new HashSet<>(4);

				for ( Entry<Instant, List<DatumAuxiliaryRecord>> tsEntry : streamAux.entrySet() ) {
					final Instant ts = tsEntry.getKey();
					final List<DatumAuxiliaryRecord> tsRecords = tsEntry.getValue();
					if ( tsRecords.isEmpty() ) {
						continue;
					}

					// have to consolidate all Mark records into a single record;
					final StringBuilder tsNotes = new StringBuilder();
					final GeneralDatumMetadata tsMeta = new GeneralDatumMetadata();
					final Set<String> tsSubTypes = new TreeSet<>();
					final Map<String, DatumAuxiliaryRecord> tsRecordsByType = new TreeMap<>();

					// extract shortest duration time-gap record
					Long tsTimeGapDuration = null;

					for ( DatumAuxiliaryRecord record : tsRecords ) {
						final GeneralDatumMetadata meta = record.getMetadata();
						if ( DatumAuxiliaryType.Mark != record.getType() ) {
							// not Mark, so no further processing done
							result.add(record);
							continue;
						}
						if ( meta == null || meta.isEmpty() ) {
							// no metadata, so skip
							continue;
						}
						final Object subTypesVal = meta.getInfo(DatumAuxiliary.SUB_TYPES_META_KEY);
						if ( !(subTypesVal instanceof List<?> subTypes) ) {
							// no sub-types, so ignore
							continue;
						}

						// consolidate sub-type data
						for ( Object subType : subTypes ) {
							final Map<String, Map<String, Object>> pm = meta.getPropertyInfo();
							if ( pm == null || pm.isEmpty() ) {
								continue;
							}
							if ( TIME_GAP_VALIDATION_TYPE.equals(subType) ) {
								final Map<String, Object> timeGapMeta = pm.get(TIME_GAP_VALIDATION_TYPE);
								final String correlationId = getMapString(CORRELATION_ID_DATA_KEY,
										timeGapMeta);
								if ( correlationId == null ) {
									throw new IllegalStateException(
											"%s %d [%s] auxiliary @ %s time gap correlation ID metadata missing at [%s] path."
													.formatted(record.getKind(), record.getObjectId(),
															record.getSourceId(), record.getTimestamp(),
															TIME_GAP_CORRELATION_ID_META_PATH));
								} else if ( correlationIdsToIgnore.contains(correlationId) ) {
									continue;
								}
								final String position = getMapString(DatumAuxiliary.POSITION_META_KEY,
										timeGapMeta);
								if ( DatumAuxiliary.START_POSITION.equals(position) ) {
									// for time-gap start, accept the shortest-duration gap only
									final Long timeGapDuration = getMapLong(DURATION_DATA_KEY,
											timeGapMeta);
									if ( timeGapDuration == null ) {
										// no time gap duration
										throw new IllegalStateException(
												"%s %d [%s] auxiliary @ %s time gap duration metadata missing at [%s] path."
														.formatted(record.getKind(),
																record.getObjectId(),
																record.getSourceId(),
																record.getTimestamp(),
																TIME_GAP_DURATION_META_PATH));
									}
									if ( tsTimeGapDuration == null
											|| timeGapDuration < tsTimeGapDuration ) {
										if ( tsRecordsByType.containsKey(TIME_GAP_VALIDATION_TYPE) ) {
											final String discardedCorrelationId = nonnull(
													getMapString(CORRELATION_ID_DATA_KEY, nonnull(
															nonnull(nonnull(
																	tsRecordsByType.get(
																			TIME_GAP_VALIDATION_TYPE),
																	"Time gap validation").getMetadata(),
																	"Metadata").getPropertyInfo(),
															"Property info")
																	.get(TIME_GAP_VALIDATION_TYPE)),
													"Correlation ID");
											correlationIdsToIgnore.add(discardedCorrelationId);
										}
										tsTimeGapDuration = timeGapDuration;
										tsRecordsByType.put(subType.toString(), record);
									}
								}
							}
							if ( tsRecordsByType.containsKey(subType) ) {
								// ignore duplicate sub type
								continue;
							}

							tsRecordsByType.put(subType.toString(), record);
						}
					}

					// process time-gap data, include only the shortest duration
					for ( Entry<String, DatumAuxiliaryRecord> auxEntry : tsRecordsByType.entrySet() ) {
						final String subType = auxEntry.getKey();
						final DatumAuxiliaryRecord record = auxEntry.getValue();

						tsSubTypes.add(subType);

						// consolidate property info
						Map<String, Map<String, Object>> tsPm = tsMeta.getPropertyInfo();
						if ( tsPm == null ) {
							tsPm = new TreeMap<>();
							tsMeta.setPropertyInfo(tsPm);
						}
						tsPm.put(subType,
								nonnull(nonnull(record.getMetadata(), "Metadata").getPropertyInfo(),
										"Property info").get(subType));

						// consolidate notes
						if ( record.getNotes() != null && !record.getNotes().isEmpty() ) {
							if ( !tsNotes.isEmpty() ) {
								tsNotes.append(' ');
							}
							tsNotes.append(record.getNotes());
						}
					}

					if ( tsSubTypes.isEmpty() || tsMeta.isEmpty() ) {
						continue;
					}
					tsMeta.putInfoValue(DatumAuxiliary.TYPE_META_KEY,
							DatumAuxiliary.DATA_VALIDATION_TYPE);
					tsMeta.putInfoValue(DatumAuxiliary.GENERATED_BY_META_KEY,
							DatumAuxiliary.GENERATED_BY_SOLARNETWORK);
					tsMeta.putInfoValue(DatumAuxiliary.SUB_TYPES_META_KEY, List.copyOf(tsSubTypes));
					result.add(new BasicDatumAuxiliaryRecord(DatumAuxiliaryType.Mark,
							streamId.datumIdentity(ts), (!tsNotes.isEmpty() ? tsNotes.toString() : null),
							null, null, tsMeta));
				}
			}
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}

		return result;
	}

}
