/* ==================================================================
 * ContentCacheStatCounter.java - 2/10/2018 12:34:41 PM
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.util.StatCounter;

/**
 * Statistics for content cache processing.
 * 
 * @author matt
 * @version 1.1
 * @since 1.16
 */
public class ContentCacheStatCounter extends StatCounter {

	private static final Logger log = LoggerFactory.getLogger(ContentCacheStatCounter.class);

	/**
	 * Construct with a unique ID.
	 * 
	 * @param uid
	 *        the UID
	 * @param logFrequency
	 *        a frequency at which to log INFO level statistic messages
	 */
	public ContentCacheStatCounter(String uid, int logFrequency) {
		super("ContentCache", uid, log, logFrequency, ContentCacheStats.values());
	}

	/**
	 * Increment and get the current count value.
	 * 
	 * @param count
	 *        the count to increment and get
	 * @return the incremented count value
	 */
	public long incrementAndGet(ContentCacheStats count, boolean quiet) {
		long c = super.incrementAndGet(count, true);
		if ( !quiet && log.isInfoEnabled() && ((c % getLogFrequency()) == 0) ) {
			long hits = 0;
			long total = 0;
			switch (count) {
				case Hit:
					hits = c;
					total = hits + get(ContentCacheStats.Miss);
					break;

				case Miss:
					hits = get(ContentCacheStats.Hit);
					total = hits + c;
					break;

				default:
					hits = get(ContentCacheStats.Hit);
					total = hits + get(ContentCacheStats.Miss);
			}
			int hitRate = (total < 1 ? 0 : (int) (((double) hits / (double) total) * 100));
			log.info("Content cache {} {}: {} ({}% hit rate)", getUid(), count.description, c, hitRate);
		}
		return c;
	}

	/**
	 * Get the effective hit rate, as percentage of hits to total of hits and
	 * misses.
	 * 
	 * @return the hit rate
	 */
	public double getHitRate() {
		long hits = get(ContentCacheStats.Hit);
		long total = hits + get(ContentCacheStats.Miss);
		return (total < 1 ? 0.0 : (double) hits / (double) total);
	}

}
