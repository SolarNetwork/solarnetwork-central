/* ==================================================================
 * MyBatisExceptionTranslator.java - 16/05/2020 4:25:40 pm
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

package net.solarnetwork.central.dao.mybatis.support;

import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;

/**
 * Extension of {@link org.mybatis.spring.MyBatisExceptionTranslator} to handle
 * connection pool exceptions like
 * <code>org.springframework.jdbc.CannotGetJdbcConnectionException</code> that
 * are returned themselves.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class MyBatisExceptionTranslator extends org.mybatis.spring.MyBatisExceptionTranslator {

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the data source
	 * @param exceptionTranslatorLazyInit
	 *        the lazy status
	 */
	public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
		super(dataSource, exceptionTranslatorLazyInit);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException e) {
		if ( e != null && e.getCause() instanceof DataAccessException ) {
			// could be something like CannotGetJdbcConnectionException, so use that directly
			return (DataAccessException) e.getCause();
		}
		return super.translateExceptionIfPossible(e);
	}

}
