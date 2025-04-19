/* ==================================================================
 * RateLimitExpiredCleanupJob.java - 19/04/2025 1:29:37 pm
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

package net.solarnetwork.central.web.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to remove expired rate limit bucket entries.
 *
 * @author matt
 * @version 1.0
 */
public class RateLimitExpiredCleanupJob extends JobSupport {

	private final ExpiredEntriesCleaner proxyManager;
	private final int maxRemovePerTransaction;
	private final int continueRemovingThreshold;

	/**
	 * Constructor.
	 *
	 * @param proxyManager
	 *        the proxy manager
	 * @param maxRemovePerTransaction
	 *        the maximum number of expired rows to remove in a single
	 *        transaction
	 * @param continueRemovingThreshold
	 *        the threshold of remaining expired rows to allow before giving up
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public RateLimitExpiredCleanupJob(ExpiredEntriesCleaner proxyManager, int maxRemovePerTransaction,
			int continueRemovingThreshold) {
		super();
		this.proxyManager = requireNonNullArgument(proxyManager, "proxyManager");
		this.maxRemovePerTransaction = maxRemovePerTransaction;
		this.continueRemovingThreshold = continueRemovingThreshold;
		setId("RateLimitExpiredCleanup");
		setGroupId("RateLimit");
	}

	@Override
	public void run() {
		int removedCount;
		do {
			removedCount = proxyManager.removeExpired(maxRemovePerTransaction);
			if ( removedCount > 0 ) {
				log.info("Removed {} expired buckets", removedCount);
			} else {
				log.debug("There are no expired buckets to remove.");
			}
		} while ( removedCount > continueRemovingThreshold );
	}

}
