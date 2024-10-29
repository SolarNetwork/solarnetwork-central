/* ==================================================================
 * JdbNodeServiceAuditor.java - 21/01/2023 5:06:05 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.NodeServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.util.StatTracker;

/**
 * JDBC based implementation of {@link NodeServiceAuditor}.
 * 
 * <p>
 * This service coalesces updates per node/service/hour in memory and flushes
 * these to the database via a single "writer" thread after a small delay. This
 * design is meant to support better throughput of audit updates, but has the
 * potential to drop some count values if the service is restarted.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcNodeServiceAuditor extends BaseJdbcDatumIdServiceAuditor implements NodeServiceAuditor {

	/**
	 * The default value for the {@code nodeServiceIncrementSql} property.
	 */
	public static final String DEFAULT_NODE_SERVICE_INCREMENT_SQL = "{call solardatm.audit_increment_node_count(?,?,?,?)}";

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcNodeServiceAuditor(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4),
				Clock.tick(Clock.systemUTC(), Duration.ofHours(1)), new StatTracker("NodeServiceAuditor",
						null, LoggerFactory.getLogger(JdbcNodeServiceAuditor.class), 1000));
	}

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param serviceCounters
	 *        the service counters map
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcNodeServiceAuditor(DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> serviceCounters, Clock clock,
			StatTracker statCounter) {
		super(dataSource, serviceCounters, clock, statCounter);
		setServiceIncrementSql(DEFAULT_NODE_SERVICE_INCREMENT_SQL);
	}

	@Override
	public Clock getAuditClock() {
		return clock;
	}

	@Override
	public void auditNodeService(Long nodeId, String service, int count) {
		if ( count == 0 ) {
			return;
		}
		addServiceCount(DatumId.nodeId(nodeId, service, clock.instant()), count);

	}

	@Override
	public String getPingTestName() {
		return "JDBC Node Service Auditor";
	}

}
