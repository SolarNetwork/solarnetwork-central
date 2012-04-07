/* ==================================================================
 * IbatisEventRuleDao.java - Jun 6, 2011 5:01:55 PM
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

import net.solarnetwork.central.dras.dao.EventRuleDao;
import net.solarnetwork.central.dras.domain.EventRule;

/**
 * Ibatis implementation of {@link EventRuleDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventRuleDao extends DrasIbatisGenericDaoSupport<EventRule>
implements EventRuleDao {

	private static interface Enumeration {
		// marker only, for query name handling
	}
	
	private static interface Schedule {
		// marker only, for query name handling
	}
	
	/**
	 * Default constructor.
	 */
	public IbatisEventRuleDao() {
		super(EventRule.class);
	}
	
	@Override
	protected Long handleUpdate(EventRule rule) {
		Long result = super.handleUpdate(rule);
		storeRelatedSet(result, Enumeration.class, rule.getEnumeration(), null);
		storeRelatedSet(result, Schedule.class, rule.getSchedule(), null);
		return result;
	}

	@Override
	protected Long handleInsert(EventRule rule) {
		Long result = super.handleInsert(rule);
		storeRelatedSet(result, Enumeration.class, rule.getEnumeration(), null);
		storeRelatedSet(result, Schedule.class, rule.getSchedule(), null);
		return result;
	}

}
