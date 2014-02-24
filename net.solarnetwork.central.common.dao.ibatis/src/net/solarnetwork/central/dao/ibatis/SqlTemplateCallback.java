/* ==================================================================
 * SqlTemplateCallback.java - Feb 25, 2014 11:48:16 AM
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

package net.solarnetwork.central.dao.ibatis;

import org.springframework.orm.ibatis.SqlMapClientTemplate;

/**
 * Callback API to help standardize error handling.
 * 
 * @author matt
 * @version 1.0
 */
public interface SqlTemplateCallback<T> {

	/**
	 * Perform some task with a {@link SqlMapClientTemplate}.
	 * 
	 * @param template
	 *        the template object
	 * @return some object (possibly <em>null</em>)
	 */
	T doWithSqlTemplate(SqlMapClientTemplate template);

}
