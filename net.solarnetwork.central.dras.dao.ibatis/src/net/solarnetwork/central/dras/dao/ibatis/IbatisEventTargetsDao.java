/* ==================================================================
 * IbatisEventTargetsDao.java - Jun 6, 2011 8:21:41 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao.ibatis;

import net.solarnetwork.central.dras.dao.EventTargetsDao;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;

/**
 * Ibatis implementation of {@link EventTargetsDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventTargetsDao extends DrasIbatisGenericDaoSupport<EventTargets>
implements EventTargetsDao {

	/**
	 * Default constructor.
	 */
	public IbatisEventTargetsDao() {
		super(EventTargets.class);
	}
	
	@Override
	protected Long handleUpdate(EventTargets datum) {
		Long result = super.handleUpdate(datum);
		storeRelatedSet(result, EventTarget.class, datum.getTargets(), null);
		return result;
	}

	@Override
	protected Long handleInsert(EventTargets datum) {
		Long result = super.handleInsert(datum);
		storeRelatedSet(result, EventTarget.class, datum.getTargets(), null);
		return result;
	}

}
