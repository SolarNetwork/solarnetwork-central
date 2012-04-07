/* ===================================================================
 * IbatisSolarNodeDao.java
 * 
 * Created Aug 13, 2008 5:02:58 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id: IbatisSolarNodeDao.java 615 2009-12-11 08:43:00Z msqr $
 * ===================================================================
 */

package net.solarnetwork.central.dao.ibatis;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;

/**
 * iBATIS implementation of {@link SolarNodeDao}.
 *
 * @author matt
 * @version $Revision: 615 $ $Date: 2009-12-11 21:43:00 +1300 (Fri, 11 Dec 2009) $
 */
public class IbatisSolarNodeDao extends IbatisGenericDaoSupport<SolarNode>
implements SolarNodeDao {
	
	/** The query name used for {@link #getUnusedNodeId(String)}. */
	public static final String QUERY_FOR_NEXT_NODE_ID = "get-next-node-id";
	
	/**
	 * Default constructor.
	 */
	public IbatisSolarNodeDao() {
		super(SolarNode.class);
	}

	public Long getUnusedNodeId() {
		return (Long)getSqlMapClientTemplate().queryForObject(
				QUERY_FOR_NEXT_NODE_ID);
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
				getSqlMapClientTemplate().insert(getInsert(), datum);
			} else {
				// update here
				getSqlMapClientTemplate().update(getUpdate(), datum);
			}
			return datum.getId();
		}
		
		// assign new ID now
		Long id = getUnusedNodeId();
		datum.setId(id);
		preprocessInsert(datum);
		getSqlMapClientTemplate().insert(getInsert(), datum);
		return id;
	}

}
