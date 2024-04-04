/* ==================================================================
 * MyBatisNetworkAssociationDao.java - Nov 10, 2014 12:59:57 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.domain.NetworkAssociation;

/**
 * MyBatis implementation of {@link NetworkAssociationDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisNetworkAssociationDao extends BaseMyBatisDao implements NetworkAssociationDao {

	/**
	 * The query used by {@link #getNetworkAssociationForConfirmationKey(String,
	 * String).
	 */
	public static final String QUERY_FOR_CONFIRMATION_CODE = "get-NetworkAssociation-for-code";

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NetworkAssociation getNetworkAssociationForConfirmationKey(String username,
			String confirmationCode) {
		Map<String, Object> params = new HashMap<String, Object>(1);
		params.put("key", confirmationCode);
		params.put("username", username);
		return selectFirst(QUERY_FOR_CONFIRMATION_CODE, params);
	}

}
