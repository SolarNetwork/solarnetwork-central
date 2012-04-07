/* ===================================================================
 * JodaLocalDateTypeHandlerCallback.java
 * 
 * Created Sep 24, 2009 10:38:23 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.dao.ibatis;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * Implementation of {@link TypeHandlerCallback} for dealing with Joda Time LocalDateTime objects.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class JodaLocalDateTimeTypeHandlerCallback implements TypeHandlerCallback {

	private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.dateTimeParser();
	
	/* (non-Javadoc)
	 * @see com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#getResult(com.ibatis.sqlmap.client.extensions.ResultGetter)
	 */
	public Object getResult(ResultGetter getter) throws SQLException {
		Timestamp t = getter.getTimestamp();
		if ( t == null ) {
			return null;
		}
		LocalDateTime dt = new LocalDateTime(t.getTime());
		return dt;
	}

	/* (non-Javadoc)
	 * @see com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#setParameter(com.ibatis.sqlmap.client.extensions.ParameterSetter, java.lang.Object)
	 */
	public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
		if ( parameter == null ) {
			setter.setNull(Types.TIMESTAMP);
		} else if ( parameter instanceof ReadableInstant ) {
			ReadableInstant p = (ReadableInstant)parameter;
			Timestamp t = new Timestamp(p.getMillis());
			setter.setTimestamp(t);
		} else {
			throw new IllegalArgumentException("Unknown timestamp object type [" 
					+parameter.getClass() +"]: " +parameter);
		}

	}

	/* (non-Javadoc)
	 * @see com.ibatis.sqlmap.client.extensions.TypeHandlerCallback#valueOf(java.lang.String)
	 */
	public Object valueOf(String s) {
		LocalDateTime date = DATE_FORMAT.parseDateTime(s).toLocalDateTime();
		return date;
	}

}
