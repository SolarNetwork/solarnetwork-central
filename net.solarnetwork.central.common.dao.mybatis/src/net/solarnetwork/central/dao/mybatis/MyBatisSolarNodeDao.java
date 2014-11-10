/* ==================================================================
 * MyBatisSolarNodeDao.java - Nov 10, 2014 1:54:17 PM
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

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.domain.SolarNode;

/**
 * MyBatis implementation of {@link SolarNodeDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisSolarNodeDao extends BaseMyBatisGenericDao<SolarNode, Long> implements SolarNodeDao {

	/** The query name used for {@link #getUnusedNodeId(String)}. */
	public static final String QUERY_FOR_NEXT_NODE_ID = "get-next-node-id";

	/**
	 * Default constructor.
	 */
	public MyBatisSolarNodeDao() {
		super(SolarNode.class, Long.class);
	}

	@Override
	public Long getUnusedNodeId() {
		return getSqlSession().selectOne(QUERY_FOR_NEXT_NODE_ID);
	}

	@Override
	public Long store(SolarNode datum) {
		// because we allow the node ID to be pre-assigned (i.e. from a
		// previous call to getUnusedNodeId() we have to test if the node
		// ID exists in the database yet, and if so perform an update, 
		// otherwise perform an insert

		if ( datum.getId() != null ) {
			SolarNode entity = get(datum.getId());
			if ( entity == null ) {
				// insert here
				preprocessInsert(datum);
				getSqlSession().insert(getInsert(), datum);
			} else {
				// update here
				getSqlSession().update(getUpdate(), datum);
			}
			return datum.getId();
		}

		// assign new ID now
		Long id = getUnusedNodeId();
		datum.setId(id);
		preprocessInsert(datum);
		getSqlSession().insert(getInsert(), datum);
		return id;
	}

}
