/* ==================================================================
 * DuplicateMessageFilter.java - 8/03/2025 10:56:54 am
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

package net.solarnetwork.central.support;

import java.io.Serial;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.mutable.MutableInt;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filter out duplicated log messages, taking into consideration parameter
 * values.
 * 
 * <p>
 * This filter is designed to help eliminate log messages that are repeated
 * frequently. At most {@code cacheSize} messages with at least the configured
 * {@code level} are cached, taking into consideration parameter values.
 * </p>
 * 
 * <p>
 * Adapted from {@code ch.qos.logback.classic.turbo.DuplicateMessageFilter}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DuplicateMessageFilter extends Filter<ILoggingEvent> {

	/**
	 * The default cache size.
	 */
	public static final int DEFAULT_CACHE_SIZE = 100;

	/**
	 * The default number of allows repetitions.
	 */
	public static final int DEFAULT_ALLOWED_REPETITIONS = 0;

	/** The default expiration time, in milliseconds. */
	public static final long DEFAULT_EXPIRATION_MS = 300_000;

	/** The default level. */
	public static final String DEFAULT_LEVEL = Level.INFO.toString();

	private int allowedRepetitions = DEFAULT_ALLOWED_REPETITIONS;
	private int cacheSize = DEFAULT_CACHE_SIZE;
	private long expiration = DEFAULT_EXPIRATION_MS;
	private Level level = Level.toLevel(DEFAULT_LEVEL);

	private LRUMessageCache msgCache;

	/**
	 * Constructor.
	 */
	public DuplicateMessageFilter() {
		super();
	}

	@Override
	public void start() {
		msgCache = new LRUMessageCache(cacheSize, expiration);
		super.start();
	}

	@Override
	public void stop() {
		msgCache.clear();
		msgCache = null;
		super.stop();
	}

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if ( !isStarted() || (level != null && !event.getLevel().isGreaterOrEqual(level)) ) {
			return FilterReply.NEUTRAL;
		}
		int count = msgCache.getMessageCountAndThenIncrement(event.getMessage(),
				event.getArgumentArray());
		if ( count <= allowedRepetitions ) {
			return FilterReply.NEUTRAL;
		} else {
			return FilterReply.DENY;
		}
	}

	@SuppressWarnings("ArrayRecordComponent")
	private static record MessageEntry(String format, Object[] params) {

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(format);
			result = prime * result + Arrays.deepHashCode(params);
			return result;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof MessageEntry m && Objects.equals(format, m.format)
					&& Arrays.deepEquals(params, m.params);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MessageEntry{format=");
			builder.append(format);
			builder.append(", ");
			if ( params != null ) {
				builder.append("params=");
				builder.append(Arrays.toString(params));
			}
			builder.append("}");
			return builder.toString();
		}

	}

	private static record MessageMeta(long timestamp, MutableInt count) {

	}

	private static class LRUMessageCache extends LinkedHashMap<MessageEntry, MessageMeta> {

		@Serial
		private static final long serialVersionUID = -2575882992309256168L;

		private final int cacheSize;
		private final long expiration;

		private LRUMessageCache(int cacheSize, long expiration) {
			super((int) (cacheSize * (4.0f / 3)), 0.75f, true);
			if ( cacheSize < 1 ) {
				throw new IllegalArgumentException("Cache size cannot be smaller than 1");
			}
			this.cacheSize = cacheSize;
			this.expiration = expiration;
		}

		private int getMessageCountAndThenIncrement(String template, Object[] params) {
			// don't insert null elements
			if ( template == null ) {
				return 0;
			}

			final var msg = new MessageEntry(template, params);

			MessageMeta m;
			synchronized ( this ) {
				m = super.get(msg);
				if ( m != null && m.timestamp + expiration < System.currentTimeMillis() ) {
					// expired
					m = null;
					super.remove(msg);
				}
				if ( m == null ) {
					m = new MessageMeta(System.currentTimeMillis(), new MutableInt(0));
					super.put(msg, m);
				} else {
					m.count.increment();
				}
			}
			return m.count.intValue();
		}

		// called indirectly by get() or put() which are already supposed to be
		// called from within the synchronized block above
		@Override
		protected boolean removeEldestEntry(Map.Entry<MessageEntry, MessageMeta> eldest) {
			return (size() > cacheSize);
		}

		@Override
		public synchronized void clear() {
			super.clear();
		}
	}

	/**
	 * Get the allowed message repetitions.
	 * 
	 * @return the count; defaults to {@link #DEFAULT_ALLOWED_REPETITIONS}
	 */
	public final int getAllowedRepetitions() {
		return allowedRepetitions;
	}

	/**
	 * Set the allowed message repetitions.
	 * 
	 * @param allowedRepetitions
	 *        the count to set
	 */
	public final void setAllowedRepetitions(int allowedRepetitions) {
		this.allowedRepetitions = allowedRepetitions;
	}

	/**
	 * Get the cache size.
	 * 
	 * @return the maximum number of cache entries; defaults to
	 *         {@link #DEFAULT_CACHE_SIZE}
	 */
	public final int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Set the cache size.
	 * 
	 * @param cacheSize
	 *        the maximum number of cache entries to set
	 */
	public final void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * Get the expiration time, in milliseconds.
	 * 
	 * @return the expiration milliseconds; defaults to
	 *         {@link #DEFAULT_EXPIRATION_MS}
	 */
	public final long getExpiration() {
		return expiration;
	}

	/**
	 * Set the expiration time, in milliseconds.
	 * 
	 * @param expiration
	 *        the expiration milliseconds to set
	 */
	public final void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	/**
	 * Get the minimum filter level.
	 * 
	 * @return the level, or {@code null}
	 */
	public String getLevel() {
		return (level != null ? level.toString() : null);
	}

	/**
	 * Set a minimum filter level.
	 * 
	 * @param level
	 *        the minimum level, or {@code null} for no minimum
	 */
	public void setLevel(String level) {
		this.level = Level.toLevel(level);
	}

}
