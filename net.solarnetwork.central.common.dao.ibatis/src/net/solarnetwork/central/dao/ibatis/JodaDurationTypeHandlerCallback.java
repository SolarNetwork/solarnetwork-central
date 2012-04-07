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

import java.sql.SQLException;
import java.sql.Types;

import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * Implementation of {@link TypeHandlerCallback} for dealing with Joda Time Duration 
 * objects.
 * 
 * <p>This implementation works by setting/getting String values of SQL INTERVAL
 * types, which are expected to be in standard ISO 8601 format.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class JodaDurationTypeHandlerCallback implements TypeHandlerCallback {

	/** The ISO PeriodFormatter. */
	protected static final PeriodFormatter PERIOD_FORMAT = ISOPeriodFormat.standard();

	public static String getDuration(Object parameter) {
		if ( parameter instanceof ReadableDuration ) {
			ReadableDuration d = (ReadableDuration)parameter;
			Period p = d.toPeriod();
			return PERIOD_FORMAT.print(p);
		} else if ( parameter instanceof ReadablePeriod ) {
			return PERIOD_FORMAT.print((ReadablePeriod)parameter);
		} else {
			throw new IllegalArgumentException("Unknown timestamp object type [" 
					+parameter.getClass() +"]: " +parameter);
		}
	}
	
	@Override
	public void setParameter(ParameterSetter setter, Object parameter)
			throws SQLException {
		if ( parameter == null ) {
			setter.setNull(Types.VARCHAR);
		} else {
			setter.setString(getDuration(parameter));
		}
	}
	
	@Override
	public Object getResult(ResultGetter getter) throws SQLException {
		Object o = getter.getObject();
		if ( o == null ) {
			return null;
		}
		return valueOf(o.toString());
	}

	@Override
	public Object valueOf(String s) {
		Period per = PERIOD_FORMAT.parsePeriod(s);
		return per.toStandardDuration();
	}

}
