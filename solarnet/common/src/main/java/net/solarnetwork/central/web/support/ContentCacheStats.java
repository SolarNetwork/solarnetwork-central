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

import net.solarnetwork.util.StatCounter;

/**
 * Content cache statistics.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public enum ContentCacheStats implements StatCounter.Stat {

	/** Cache hit. */
	Hit(0, "cache hits"),

	/** Cache miss. */
	Miss(1, "cache misses"),

	/** Count of items added to cache. */
	Stored(2, "cache puts"),

	/** Number of entries in the cache. */
	EntryCount(3, "number of entries"),

	/** Total bytes size of all entries in the cache. */
	ByteSize(4, "byte size of entries"),

	;

	final int index;
	final String description;

	private ContentCacheStats(int index, String description) {
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
