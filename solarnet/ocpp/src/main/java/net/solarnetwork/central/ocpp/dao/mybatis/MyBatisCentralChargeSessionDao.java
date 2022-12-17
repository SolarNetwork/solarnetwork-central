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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.ChargeSessionFilter;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.util.ObjectUtils;

/**
 * MyBatis implementation of {@link CentralChargeSessionDao}.
 * 
 * @author matt
 * @version 1.2
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

		/** Get a charge session for a given user ID and session ID. */
		GetForUserAndId("get-CentralChargeSession-for-user-and-id"),

		/** Insert a sampled value reading. */
		InsertReading("insert-CentralChargeSession-reading"),

		/** Find sessions based on a filter. */
		FindFiltered("find-CentralChargeSession-for-filter"),

		/** End an active session. */
		EndSession("update-CentralChargeSession-end-session"),

		;

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
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setChargePointId(chargePointId);
		filter.setTransactionId(transactionId);
		return selectFirst(QueryName.FindByIncomplete.getQueryName(),
				singletonMap(FILTER_PROPERTY, filter));
	}

	@Override
	public ChargeSession getIncompleteChargeSessionForConnector(long chargePointId, int connectorId) {
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setChargePointId(chargePointId);
		filter.setConnectorId(connectorId);
		return selectFirst(QueryName.FindByIncomplete.getQueryName(),
				singletonMap(FILTER_PROPERTY, filter));
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessionsForChargePoint(long chargePointId) {
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setChargePointId(chargePointId);
		return selectList(QueryName.FindByIncomplete.getQueryName(),
				singletonMap(FILTER_PROPERTY, filter), null, null);
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessions() {
		return selectList(QueryName.FindByIncomplete.getQueryName(), null, null, null);
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessionsForUserForChargePoint(long userId,
			long chargePointId) {
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setChargePointId(chargePointId);
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		params.put(FILTER_PROPERTY, filter);
		return selectList(QueryName.FindByIncomplete.getQueryName(), params, null, null);
	}

	@Override
	public ChargeSession get(UUID id, Long userId) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("userId", userId);
		params.put("id", id);
		return selectFirst(QueryName.GetForUserAndId.getQueryName(), params);
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

	@Override
	public FilterResults<ChargeSession, UUID> findFiltered(ChargeSessionFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		List<ChargeSession> results = selectList(QueryName.FindFiltered.getQueryName(), filter, null,
				null);
		return new BasicFilterResults<>(results, null, offset != null ? offset.intValue() : 0,
				results.size());
	}

	@Override
	public boolean endSession(Long userId, UUID sessionId, ChargeSessionEndReason reason,
			String endAuthId) {
		Map<String, Object> params = new LinkedHashMap<>(4);
		params.put("id", sessionId);
		params.put("userId", ObjectUtils.requireNonNullArgument(userId, "userId"));
		params.put("endReason", ObjectUtils.requireNonNullArgument(reason, "reason"));
		if ( endAuthId != null ) {
			params.put("endAuthId", endAuthId);
		}
		int count = getSqlSession().update(QueryName.EndSession.getQueryName(), params);
		return (getLastUpdateCount(count) > 0 ? true : false);
	}

}
