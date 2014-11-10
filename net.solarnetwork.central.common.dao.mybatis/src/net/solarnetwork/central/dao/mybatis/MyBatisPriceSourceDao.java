/* ==================================================================
 * MyBatisPriceSourceDao.java - Nov 10, 2014 1:19:03 PM
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

import java.util.Map;
import net.solarnetwork.central.dao.PriceSourceDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.PriceSource;
import net.solarnetwork.central.domain.SourceLocation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis implementation of {@link PriceSourceDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisPriceSourceDao extends
		BaseMyBatisFilterableDaoSupport<PriceSource, EntityMatch, SourceLocation, Long> implements
		PriceSourceDao {

	/** The query name used for {@link #getPriceSourceForName(String)}. */
	public static final String QUERY_FOR_NAME = "get-PriceSource-for-name";

	/**
	 * Default constructor.
	 */
	public MyBatisPriceSourceDao() {
		super(PriceSource.class, Long.class, EntityMatch.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public PriceSource getPriceSourceForName(String sourceName) {
		return selectFirst(QUERY_FOR_NAME, sourceName);
	}

	@Override
	protected void postProcessFilterProperties(SourceLocation filter, Map<String, Object> sqlProps) {
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
