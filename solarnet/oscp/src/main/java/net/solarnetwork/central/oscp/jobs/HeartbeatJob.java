/* ==================================================================
 * HeartbeatJob.java - 21/08/2022 3:58:02 pm
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

package net.solarnetwork.central.oscp.jobs;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.scheduler.JobSupport;
import oscp.v20.Heartbeat;

/**
 * Job to post OSCP heartbeat messages to configured systems.
 * 
 * <p>
 * This job is designed to support parallel execution, across multiple runtime
 * instances. The
 * {@link ExternalSystemConfigurationDao#processExternalSystemWithExpiredHeartbeat(java.util.function.Function)}
 * method is used to exclusively process individual pending external system
 * heartbeats by locking the heartbeat table row, making the heartbeat request,
 * and updating the locked row with the execution time. When querying for
 * pending heartbeat rows to process, locked rows are skipped. Thus the parallel
 * tasks compete for rows to process, until none remain or the maximum iteration
 * count is reached.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatJob extends JobSupport {

	private final OscpRole role;
	private final ExternalSystemConfigurationDao<?> dao;
	private final ExternalSystemClient client;
	private TransactionTemplate txTemplate;

	/**
	 * Construct with properties.
	 * 
	 * @param role
	 *        the role
	 * @param dao
	 *        the DAO to use
	 * @param client
	 *        the client to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HeartbeatJob(OscpRole role, ExternalSystemConfigurationDao<?> dao,
			ExternalSystemClient client) {
		super();
		this.role = requireNonNullArgument(role, "role");
		this.dao = requireNonNullArgument(dao, "dao");
		this.client = requireNonNullArgument(client, "client");
		setGroupId("OSCP");
		setId(this.role.toString() + "-Heartbeat");
		setMaximumWaitMs(1800000L);
	}

	/**
	 * Configure a transaction template.
	 * 
	 * @param txTemplate
	 *        the template
	 * @return this instance for method chaining
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HeartbeatJob withTxTemplate(TransactionTemplate txTemplate) {
		this.txTemplate = requireNonNullArgument(txTemplate, "txTemplate");
		return this;
	}

	@Override
	public void run() {
		executeParallelJob(getId());
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterataions) throws Exception {
		int totalProcessedCount = 0;
		Set<String> supportedOscpVersions = singleton(V20);
		final TransactionTemplate txTemplate = this.txTemplate;
		boolean processed = false;
		do {
			processed = false;
			if ( txTemplate != null ) {
				processed = txTemplate.execute((tx) -> {
					return exchange(supportedOscpVersions, remainingIterataions);
				});
			} else {
				processed = exchange(supportedOscpVersions, remainingIterataions);
			}
			if ( processed ) {
				totalProcessedCount += 1;
			}
		} while ( processed && remainingIterataions.get() > 0 );
		return totalProcessedCount;
	}

	private boolean exchange(Set<String> supportedOscpVersions, AtomicInteger remainingIterataions) {
		return dao.processExternalSystemWithExpiredHeartbeat((ctx) -> {
			remainingIterataions.decrementAndGet();
			Integer secs = ctx.config().getSettings().heartbeatSeconds();
			if ( secs == null ) {
				return null;
			}
			Instant expires = Instant.now().plusSeconds(secs.longValue() * 2L);
			try {
				client.systemExchange(ctx, HttpMethod.POST, () -> {
					ctx.verifySystemOscpVersion(supportedOscpVersions);
					return HEARTBEAT_URL_PATH;
				}, new Heartbeat(expires));
				return Instant.now();
			} catch ( RuntimeException e ) {
				// ignore and continue; assume event logged in client.systemExchange()
				return null;
			}
		});
	}

}
