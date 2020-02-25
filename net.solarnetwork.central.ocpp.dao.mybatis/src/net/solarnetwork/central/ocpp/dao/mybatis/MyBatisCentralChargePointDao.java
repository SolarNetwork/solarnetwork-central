/* ==================================================================
 * MyBatisCentralChargePointDao.java - 25/02/2020 2:16:55 pm
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
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;

/**
 * MyBatis implementation of {@link CentralChargePointDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralChargePointDao extends BaseMyBatisGenericDaoSupport<ChargePoint, Long>
		implements CentralChargePointDao {

	/**
	 * Constructor.
	 */
	public MyBatisCentralChargePointDao() {
		super(CentralChargePoint.class, Long.class);
	}

	@Override
	public ChargePoint getForIdentifier(String identifier) {
		throw new UnsupportedOperationException(
				"Must call getForIdentifier(userId,identifier) instead.");
	}

	@Override
	public ChargePoint getForIdentifier(Long userId, String identifier) {
		return selectFirst(getQueryForAll(), singletonMap(FILTER_PROPERTY,
				new CentralChargePoint(null, userId, null, new ChargePointInfo(identifier))));
	}

}
