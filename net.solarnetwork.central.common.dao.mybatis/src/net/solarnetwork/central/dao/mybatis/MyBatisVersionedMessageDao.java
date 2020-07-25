/* ==================================================================
 * MyBatisVersionedMessageDao.java - 25/07/2020 10:46:20 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.domain.KeyValuePair;

/**
 * MyBatis implementation of {@link VersionedMessageDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public class MyBatisVersionedMessageDao extends BaseMyBatisDao implements VersionedMessageDao {

	/** Query name enumeration. */
	public enum QueryName {

		FindMessages("find-messages");

		private final String queryName;

		private QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 * 
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}
	}

	@Override
	public Properties findMessages(Instant version, String[] bundleNames, String locale) {
		Map<String, Object> params = new LinkedHashMap<>(3);
		params.put("version", version);
		params.put("bundles", bundleNames);
		params.put("locale", locale);
		List<KeyValuePair> data = selectList(QueryName.FindMessages.getQueryName(), params, null, null);
		if ( data == null || data.isEmpty() ) {
			return null;
		}
		Properties props = data.stream().reduce(new Properties(), (p, e) -> {
			p.put(e.getKey(), e.getValue());
			return p;
		}, (l, r) -> {
			return l;
		});
		return props;
	}

}
