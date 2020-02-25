/* ==================================================================
 * MyBatisCentralChargePointConnectorDao.java - 26/02/2020 8:43:16 am
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
import java.util.Collection;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * MyBatis implementation of {@link CentralChargePointConnectorDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralChargePointConnectorDao
		extends BaseMyBatisGenericDaoSupport<ChargePointConnector, ChargePointConnectorKey>
		implements CentralChargePointConnectorDao {

	/**
	 * The SQL query name to update just the status of charge point connectors.
	 */
	public static final String UPDATE_STATUS = "update-CentralChargePointConnector-status";

	/**
	 * Constructor.
	 */
	public MyBatisCentralChargePointConnectorDao() {
		super(CentralChargePointConnector.class, ChargePointConnectorKey.class);
	}

	@Override
	public ChargePointConnectorKey saveStatusInfo(long chargePointId, StatusNotification info) {
		CentralChargePointConnector cpc = new CentralChargePointConnector(
				new ChargePointConnectorKey(chargePointId, info.getConnectorId()));
		cpc.setInfo(info);
		return save(cpc);
	}

	@Override
	public int updateChargePointStatus(long chargePointId, int connectorId, ChargePointStatus status) {
		CentralChargePointConnector filter = new CentralChargePointConnector(
				new ChargePointConnectorKey(chargePointId, connectorId));
		filter.setInfo(
				StatusNotification.builder().withConnectorId(connectorId).withStatus(status).build());
		return getSqlSession().update(UPDATE_STATUS, filter);
	}

	@Override
	public Collection<ChargePointConnector> findByChargePointId(long chargePointId) {
		return selectList(getQueryForAll(),
				singletonMap(FILTER_PROPERTY,
						new ChargePointConnector(new ChargePointConnectorKey(chargePointId, 0))),
				null, null);
	}

}
