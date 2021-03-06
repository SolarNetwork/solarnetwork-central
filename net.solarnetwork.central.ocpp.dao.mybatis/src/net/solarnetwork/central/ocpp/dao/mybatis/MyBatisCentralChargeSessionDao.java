/* ==================================================================
 * MyBatisCentralChargeSessionDao.java - 26/02/2020 12:10:50 pm
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

package net.solarnetwork.central.ocpp.dao.mybatis;

import static java.util.Collections.singletonMap;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.SampledValue;

/**
 * MyBatis implementation of {@link CentralChargeSessionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralChargeSessionDao extends BaseMyBatisGenericDaoSupport<ChargeSession, UUID>
		implements CentralChargeSessionDao {

	/** Query name enumeration. */
	public enum QueryName {

		/**
		 * Delete entities with a {@code posted} date older than a given date.
		 */
		DeleteByPosted("delete-CentralChargeSession-for-posted"),

		/** Find entities with a {@literal null} {@code ended} value. */
		FindByIncomplete("findall-CentralChargeSession-incomplete"),

		/** Find all sampled value readings for a charge session ID. */
		FindReadingBySession("findall-SampledValue-for-session"),

		/** Insert a sampled value reading. */
		InsertReading("insert-CentralChargeSession-reading");

		private final String queryName;

		private QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 * 
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}
	}

	/**
	 * Constructor.
	 */
	public MyBatisCentralChargeSessionDao() {
		super(CentralChargeSession.class, UUID.class);
	}

	@Override
	public ChargeSession getIncompleteChargeSessionForTransaction(long chargePointId,
			int transactionId) {
		return selectFirst(QueryName.FindByIncomplete.getQueryName(), singletonMap(FILTER_PROPERTY,
				CentralChargeSession.forTransaction(chargePointId, transactionId)));
	}

	@Override
	public ChargeSession getIncompleteChargeSessionForConnector(long chargePointId, int connectorId) {
		return selectFirst(QueryName.FindByIncomplete.getQueryName(), singletonMap(FILTER_PROPERTY,
				CentralChargeSession.forConnector(chargePointId, connectorId)));
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessionsForChargePoint(long chargePointId) {
		return selectList(QueryName.FindByIncomplete.getQueryName(),
				singletonMap(FILTER_PROPERTY, CentralChargeSession.forChargePoint(chargePointId)), null,
				null);
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessions() {
		return selectList(QueryName.FindByIncomplete.getQueryName(), null, null, null);
	}

	@Override
	public void addReadings(Iterable<SampledValue> readings) {
		if ( readings == null ) {
			return;
		}
		SqlSession sqlSession = getSqlSession();
		for ( SampledValue reading : readings ) {
			sqlSession.insert(QueryName.InsertReading.getQueryName(), reading);
		}
		sqlSession.flushStatements();
	}

	@Override
	public List<SampledValue> findReadingsForSession(UUID sessionId) {
		return selectList(QueryName.FindReadingBySession.getQueryName(), sessionId, null, null);
	}

	@Override
	public int deletePostedChargeSessions(Instant expirationDate) {
		return getSqlSession().delete(QueryName.DeleteByPosted.getQueryName(), expirationDate);
	}

}
