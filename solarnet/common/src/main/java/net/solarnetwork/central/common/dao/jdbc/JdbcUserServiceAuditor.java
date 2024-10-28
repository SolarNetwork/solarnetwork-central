/* ==================================================================
 * JdbcUserServiceAuditor.java - 29/05/2024 4:35:42 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.util.StatTracker;

/**
 * JDBC based implementation of {@link UserServiceAuditor}.
 * 
 * <p>
 * This service coalesces updates per user/service/hour in memory and flushes
 * these to the database via a single "writer" thread after a small delay. This
 * design is meant to support better throughput of audit updates, but has the
 * potential to drop some count values if the service is restarted.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserServiceAuditor extends BaseJdbcDatumIdServiceAuditor implements UserServiceAuditor {

	/**
	 * The default value for the {@code nodeServiceIncrementSql} property.
	 */
	public static final String DEFAULT_USER_SERVICE_INCREMENT_SQL = "{call solardatm.audit_increment_user_count(?,?,?,?)}";

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserServiceAuditor(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4),
				Clock.tick(Clock.systemUTC(), Duration.ofHours(1)), new StatTracker("UserServiceAuditor",
						null, LoggerFactory.getLogger(JdbcUserServiceAuditor.class), 1000));
	}

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param userServiceCounters
	 *        the node source counters map
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserServiceAuditor(DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> userServiceCounters, Clock clock,
			StatTracker statCounter) {
		super(dataSource, userServiceCounters, clock, statCounter);
		setServiceIncrementSql(DEFAULT_USER_SERVICE_INCREMENT_SQL);
	}

	@Override
	public Clock getAuditClock() {
		return clock;
	}

	@Override
	public void auditUserService(Long userId, String service, int count) {
		if ( count == 0 ) {
			return;
		}
		addServiceCount(DatumId.nodeId(userId, service, clock.instant()), count);
	}

	@Override
	public String getPingTestName() {
		return "JDBC User Service Auditor";
	}

}
