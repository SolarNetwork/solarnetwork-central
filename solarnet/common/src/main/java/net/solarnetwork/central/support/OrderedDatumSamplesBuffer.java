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

package net.solarnetwork.central.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.util.ObjectUtils;

/**
 * An ordered collection of datum samples, grouped by stream identity.
 * 
 * @author matt
 * @version 1.0
 */
public class OrderedDatumSamplesBuffer {

	private final SortedMap<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> buffer;
	private final Function<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> streamBufferCreator;
	private final @Nullable Lock lock;

	/**
	 * Create a thread-safe buffer.
	 * 
	 * <p>
	 * This will use {@link ConcurrentSkipListMap} implementations for the
	 * buffer.
	 * </p>
	 * 
	 * @return the new buffer instance
	 */
	public static OrderedDatumSamplesBuffer newConcurrentBuffer() {
		return new OrderedDatumSamplesBuffer(new ConcurrentSkipListMap<>(),
				_ -> new ConcurrentSkipListMap<>());
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * {@link TreeMap} will be used, so the instance will not be thread-safe.
	 * </p>
	 */
	public OrderedDatumSamplesBuffer() {
		this(new TreeMap<>(), _ -> new TreeMap<>());
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
	 */
	public OrderedDatumSamplesBuffer(
			SortedMap<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> buffer,
			Function<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> streamBufferCreator) {
		super();
		this.buffer = ObjectUtils.requireNonNullArgument(buffer, "buffer");
		this.streamBufferCreator = ObjectUtils.requireNonNullArgument(streamBufferCreator,
				"streamBufferCreator");
		this.lock = (buffer instanceof ConcurrentMap<?, ?> ? new ReentrantLock() : null);
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
		for ( Entry<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> e : buffer.entrySet() ) {
			if ( !e.getValue().isEmpty() ) {
				result.put(e.getKey(), e.getValue().lastKey());
			}
		}
		return result;
	}

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

}
