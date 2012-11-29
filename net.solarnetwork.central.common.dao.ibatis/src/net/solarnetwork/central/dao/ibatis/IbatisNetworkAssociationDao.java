/* ==================================================================
 * IbatisNetworkAssociationDao.java - Nov 29, 2012 10:52:08 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.domain.NetworkAssociation;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * iBATIS implementation of {@link NetworkAssociationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisNetworkAssociationDao extends SqlMapClientDaoSupport implements NetworkAssociationDao {

	private final String queryForConfirmationCode = "get-NetworkAssociation-for-code";

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NetworkAssociation getNetworkAssociationForConfirmationKey(String username,
			String confirmationCode) {
		Map<String, Object> params = new HashMap<String, Object>(1);
		params.put("key", confirmationCode);
		params.put("username", username);
		@SuppressWarnings("unchecked")
		List<NetworkAssociation> results = getSqlMapClientTemplate().queryForList(
				queryForConfirmationCode, params);
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

}
