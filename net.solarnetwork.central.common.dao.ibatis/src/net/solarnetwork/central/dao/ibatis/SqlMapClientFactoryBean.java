/* ==================================================================
 * SqlMapClientFactoryBean.java - Dec 3, 2013 10:41:10 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.springframework.core.io.Resource;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;

/**
 * Custom factory for {@link SqlMapClient} that supports more efficient query
 * results.
 * 
 * @author matt
 * @version 1.0
 */
public class SqlMapClientFactoryBean extends org.springframework.orm.ibatis.SqlMapClientFactoryBean {

	@Override
	protected SqlMapClient buildSqlMapClient(Resource[] configLocations, Resource[] mappingLocations,
			Properties properties) throws IOException {
		SqlMapClient client = super.buildSqlMapClient(configLocations, mappingLocations, properties);
		if ( client instanceof SqlMapClientImpl ) {
			SqlMapClientImpl impl = (SqlMapClientImpl) client;
			impl.delegate = new SqlMapExecutorDelegate(impl.delegate);
		}
		return client;
	}

	private static class SqlMapExecutorDelegate extends
			com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate {

		@SuppressWarnings("unchecked")
		private SqlMapExecutorDelegate(com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate other) {
			super();
			setCacheModelsEnabled(other.isCacheModelsEnabled());
			setEnhancementEnabled(other.isEnhancementEnabled());
			setForceMultipleResultSetSupport(other.isForceMultipleResultSetSupport());
			setLazyLoadingEnabled(other.isLazyLoadingEnabled());
			setResultObjectFactory(other.getResultObjectFactory());
			setStatementCacheEnabled(other.isStatementCacheEnabled());
			setTxManager(other.getTxManager());
			setUseColumnLabel(other.isUseColumnLabel());
			Iterator<String> itr = other.getCacheModelNames();
			if ( itr != null ) {
				while ( itr.hasNext() ) {
					addCacheModel(other.getCacheModel(itr.next()));
				}
			}
			itr = other.getMappedStatementNames();
			if ( itr != null ) {
				while ( itr.hasNext() ) {
					addMappedStatement(other.getMappedStatement(itr.next()));
				}
			}
			itr = other.getResultMapNames();
			if ( itr != null ) {
				while ( itr.hasNext() ) {
					addResultMap(other.getResultMap(itr.next()));
				}
			}
			itr = other.getParameterMapNames();
			if ( itr != null ) {
				while ( itr.hasNext() ) {
					addParameterMap(other.getParameterMap(itr.next()));
				}
			}
			this.sqlExecutor = new net.solarnetwork.central.dao.ibatis.SqlExecutor();
		}
	}

}
