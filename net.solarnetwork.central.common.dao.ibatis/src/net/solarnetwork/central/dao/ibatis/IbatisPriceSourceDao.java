/* ==================================================================
 * IbatisPriceSourceDao.java - Apr 16, 2012 1:19:48 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dao.ibatis;

import java.util.List;
import java.util.Map;

import net.solarnetwork.central.dao.PriceSourceDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.PriceSource;
import net.solarnetwork.central.domain.SourceLocation;

/**
 * Ibatis implementation of {@link PriceSourceDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisPriceSourceDao 
extends IbatisFilterableDaoSupport<PriceSource, EntityMatch, SourceLocation>
implements PriceSourceDao {

	/** The query name used for {@link #getPriceSourceForName(String)}. */
	public static final String QUERY_FOR_NAME = "get-PriceSource-for-name";

	/**
	 * Default constructor.
	 */
	public IbatisPriceSourceDao() {
		super(PriceSource.class, EntityMatch.class);
	}
	
	@Override
	public PriceSource getPriceSourceForName(String sourceName) {
		@SuppressWarnings("unchecked")
		List<PriceSource> results = getSqlMapClientTemplate().queryForList(
				QUERY_FOR_NAME, sourceName, 0, 1);
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	protected void postProcessFilterProperties(SourceLocation filter,
			Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getSource(), fts);
		if ( filter.getLocation() != null ) {
			spaceAppend(filter.getLocation().getName(), fts);
		}
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}

}
