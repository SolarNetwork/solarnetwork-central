/* ==================================================================
 * SqsStats.java - 16/06/2020 7:43:25 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dest.sqs;

import java.util.concurrent.atomic.AtomicLongArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics for SQS processing.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsStats {

	/**
	 * An SQS statistic API.
	 */
	public interface SqsStat {

		int getIndex();

		String getDescription();
	}

	/** Basic counted fields. */
	public enum BasicCount implements SqsStat {

		NodeEventsReceived(0, "node events received"),

		NodeEventsPublished(1, "node events published"),

		NodeEventsPublishFailed(2, "node publish failures");

		private final int index;
		private final String description;

		private BasicCount(int index, String description) {
			this.index = index;
			this.description = description;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public String getDescription() {
			return description;
		}

	}

	private static final Logger log = LoggerFactory.getLogger(SqsStats.class);

	private final AtomicLongArray counts;
	private final SqsStat[] countStats;
	private int logFrequency;
	private String uid;

	/**
	 * Constructor.
	 */
	public SqsStats() {
		this("", 0, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 */
	public SqsStats(int logFrequency) {
		this("", logFrequency, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param countStats
	 *        the number of statistics to track (on top of the
	 *        {@link BasicCount}
	 */
	public SqsStats(SqsStat[] countStats) {
		this("", 0, countStats);
	}

	/**
	 * Constructor.
	 * 
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 * @param countStats
	 *        the number of statistics to track (on top of the
	 *        {@link BasicCount}
	 */
	public SqsStats(int logFrequency, SqsStat[] countStats) {
		this("", logFrequency, countStats);
	}

	/**
	 * Constructor.
	 * 
	 * @param uid
	 *        the UID
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 */
	public SqsStats(String uid, int logFrequency) {
		this(uid, logFrequency, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param uid
	 *        the UID
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 * @param countStats
	 *        the number of statistics to track (on top of the
	 *        {@link BasicCount}
	 */
	public SqsStats(String uid, int logFrequency, SqsStat[] countStats) {
		super();
		this.uid = uid;
		this.logFrequency = logFrequency;
		this.countStats = countStats;
		this.counts = new AtomicLongArray(
				BasicCount.values().length + (countStats != null ? countStats.length : 0));
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
	 * Get the log frequency.
	 * 
	 * @return the frequency
	 */
	public int getLogFrequency() {
		return logFrequency;
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

	private int countStatIndex(SqsStat stat) {
		int idx = stat.getIndex();
		if ( !(stat instanceof BasicCount) ) {
			idx += BasicCount.values().length;
		}
		return idx;
	}

	/**
	 * Get a current count value.
	 * 
	 * @param stat
	 *        the statistic to get the count for
	 * @return the current count value
	 */
	public long get(SqsStat stat) {
		return counts.get(countStatIndex(stat));
	}

	/**
	 * Increment and get the current count value.
	 * 
	 * @param stat
	 *        the count to increment and get
	 * @return the incremented count value
	 */
	public long incrementAndGet(SqsStat stat) {
		long c = counts.incrementAndGet(countStatIndex(stat));
		if ( log.isInfoEnabled() && logFrequency > 0 && ((c % logFrequency) == 0) ) {
			log.info("SQS Publisher {} {}: {}", uid, stat.getDescription(), c);
		}
		return c;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("CollectorStats{\n");
		for ( SqsStat c : BasicCount.values() ) {
			buf.append(String.format("%30s: %d\n", c.getDescription(), get(c)));
		}
		if ( countStats != null ) {
			for ( SqsStat c : countStats ) {
				buf.append(String.format("%30s: %d\n", c.getDescription(), get(c)));
			}
		}
		buf.append("}");
		return buf.toString();
	}

}
