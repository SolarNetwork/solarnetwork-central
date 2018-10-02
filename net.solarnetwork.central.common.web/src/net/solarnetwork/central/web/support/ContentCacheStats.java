/* ==================================================================
 * ContentCacheStats.java - 2/10/2018 12:34:41 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.support;

import java.util.concurrent.atomic.AtomicLongArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics for content cache processing.
 * 
 * @author matt
 * @version 1.0
 * @since 1.16
 */
public class ContentCacheStats {

	/** Counted fields. */
	public enum Counts {

	Hit(0, "cache hits"),

	Miss(1, "cache misses"),

	Stored(2, "cache puts");

		private final int index;
		private final String description;

		private Counts(int index, String description) {
			this.index = index;
			this.description = description;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(ContentCacheStats.class);

	private final AtomicLongArray counts;
	private int logFrequency;
	private String uid;

	/**
	 * Construct with a unique ID.
	 * 
	 * @param uid
	 *        the UID
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 */
	public ContentCacheStats(String uid, int logFrequency) {
		super();
		this.uid = uid;
		this.logFrequency = logFrequency;
		this.counts = new AtomicLongArray(Counts.values().length);
	}

	/**
	 * Set the log frequency.
	 * 
	 * @param logFrequency
	 *        the frequency
	 */
	public void setLogFrequency(int logFrequency) {
		this.logFrequency = logFrequency;
	}

	/**
	 * Set the unique ID.
	 * 
	 * @param uid
	 *        the unique ID
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Get a current count value.
	 * 
	 * @param count
	 *        the count to get
	 * @return the current count value
	 */
	public long get(Counts count) {
		return counts.get(count.index);
	}

	/**
	 * Increment and get the current count value.
	 * 
	 * @param count
	 *        the count to increment and get
	 * @return the incremented count value
	 */
	public long incrementAndGet(Counts count) {
		long c = counts.incrementAndGet(count.index);
		if ( log.isInfoEnabled() && ((c % logFrequency) == 0) ) {
			long hits = 0;
			long total = 0;
			switch (count) {
				case Hit:
					hits = c;
					total = hits + get(Counts.Miss);
					break;

				case Miss:
					hits = get(Counts.Hit);
					total = hits + c;

				default:
					hits = get(Counts.Hit);
					total = hits + get(Counts.Miss);
			}
			int hitRate = (total < 1 ? 0 : (int) (((double) hits / (double) total) * 100));
			log.info("Content cache {} {}: {} ({}% hit rate)", uid, count.description, c, hitRate);
		}
		return c;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("ContentCacheStats{\n");
		for ( Counts c : Counts.values() ) {
			buf.append(String.format("%30s: %d\n", c.description, get(c)));
		}
		buf.append("}");
		return buf.toString();
	}

}
