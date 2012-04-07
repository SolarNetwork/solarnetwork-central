/* ==================================================================
 * JodaDurationTypeHandlerCallback.java - Jun 6, 2011 6:39:16 PM
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

import org.joda.time.Period;

import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * Implementation of {@link TypeHandlerCallback} for dealing with Joda Time 
 * Period objects.
 * 
 * <p>This implementation works by setting/getting String values of SQL INTERVAL
 * types, which are expected to be in standard ISO 8601 format.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class JodaPeriodTypeHandlerCallback extends JodaDurationTypeHandlerCallback {

	@Override
	public Object valueOf(String s) {
		Period p = PERIOD_FORMAT.parsePeriod(s);
		return p;
	}

}
