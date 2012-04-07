/* ==================================================================
 * DefaultResultObjectFactory.java - Jun 6, 2011 8:58:12 PM
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

package net.solarnetwork.central.dao.ibatis;

import java.util.SortedSet;
import java.util.TreeSet;

import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;

/**
 * {@link ResultObjectFactory} with support for additional types.
 * 
 * <p>The additional result types supported are:</p>
 * 
 * <dl>
 *   <dt>{@link SortedSet}</dt>
 *   <dd>Returns a {@link TreeSet}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class DefaultResultObjectFactory implements ResultObjectFactory {

	@Override
	public Object createInstance(String statementId, @SuppressWarnings("rawtypes") Class clazz)
			throws InstantiationException, IllegalAccessException {
		if ( clazz == SortedSet.class ) {
			return new TreeSet<Object>();
		}
		return null;
	}

	@Override
	public void setProperty(String name, String value) {
		// none supported for now
	}

}
