/* ==================================================================
 * IbatisOutboundMailDao.java - Jun 18, 2011 8:25:04 PM
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

import java.util.Map;

import net.solarnetwork.central.dras.dao.OutboundMailDao;
import net.solarnetwork.central.dras.dao.OutboundMailFilter;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.OutboundMail;

/**
 * Ibatis implementation of {@link OutboundMailDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisOutboundMailDao 
extends DrasIbatisFilterableDaoSupport<OutboundMail, Match, OutboundMailFilter>
implements OutboundMailDao {

	/**
	 * Default constructor.
	 */
	public IbatisOutboundMailDao() {
		super(OutboundMail.class, Match.class);
	}

	@Override
	protected Long handleUpdate(OutboundMail datum) {
		throw new UnsupportedOperationException("Updating OutboundMail is not allowed");
	}

	@Override
	protected void postProcessFilterProperties(OutboundMailFilter filter,
			Map<String, Object> sqlProps) {
		if ( filter.getQuery() != null ) {
			sqlProps.put("fts", filter.getQuery());
		}
	}
	
}
