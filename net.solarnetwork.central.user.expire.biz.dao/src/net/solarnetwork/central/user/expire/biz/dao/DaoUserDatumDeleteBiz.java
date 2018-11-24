/* ==================================================================
 * DaoUserDatumDeleteBiz.java - 24/11/2018 9:55:53 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.biz.dao;

import java.util.Set;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;

/**
 * DAO implementation of {@link UserDatumDeleteBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumDeleteBiz implements UserDatumDeleteBiz {

	private final UserNodeDao userNodeDao;
	private final GeneralNodeDatumDao datumDao;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param datumDao
	 *        the datum DAO to use
	 */
	public DaoUserDatumDeleteBiz(UserNodeDao userNodeDao, GeneralNodeDatumDao datumDao) {
		super();
		this.userNodeDao = userNodeDao;
		this.datumDao = datumDao;
	}

	private GeneralNodeDatumFilter prepareFilter(GeneralNodeDatumFilter filter) {
		if ( filter == null ) {
			throw new IllegalArgumentException("GeneralNodeDatumFilter is required");
		}
		if ( filter.getUserId() == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( filter.getNodeId() == null ) {
			DatumFilterCommand f = new DatumFilterCommand(filter);
			Set<Long> nodes = userNodeDao.findNodeIdsForUser(filter.getUserId());
			f.setNodeIds(nodes.toArray(new Long[nodes.size()]));
			filter = f;
		}
		return filter;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter) {
		filter = prepareFilter(filter);
		if ( filter.getNodeId() == null ) {
			DatumRecordCounts counts = new DatumRecordCounts();
			counts.setDate(new DateTime());
			return counts;
		}
		return datumDao.countDatumRecords(filter);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long deleteFiltered(GeneralNodeDatumFilter filter) {
		filter = prepareFilter(filter);
		if ( filter.getNodeId() == null ) {
			return 0L;
		}
		return datumDao.deleteFiltered(filter);
	}

}
