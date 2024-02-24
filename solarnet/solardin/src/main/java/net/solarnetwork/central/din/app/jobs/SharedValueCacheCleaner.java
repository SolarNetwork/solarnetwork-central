/* ==================================================================
 * SharedValueCacheCleaner.java - 24/02/2024 11:49:54 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.app.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.support.SharedValueCache;

/**
 * Shared value cache cleaner job.
 *
 * @author matt
 * @version 1.0
 */
public class SharedValueCacheCleaner extends JobSupport {

	private final SharedValueCache<?, ?, ?> cache;

	/**
	 * Constructor.
	 *
	 * @param cache
	 *        the cache
	 * @param cacheName
	 *        the cache name
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SharedValueCacheCleaner(SharedValueCache<?, ?, ?> cache, String cacheName) {
		super();
		this.cache = requireNonNullArgument(cache, "cache");
		setGroupId(SolarDinJobs.JOBS_GROUP);
		setId("SharedValueCacheCleaner-" + requireNonNullArgument(cacheName, "cacheName"));
	}

	@Override
	public void run() {
		cache.prune();
	}

}
