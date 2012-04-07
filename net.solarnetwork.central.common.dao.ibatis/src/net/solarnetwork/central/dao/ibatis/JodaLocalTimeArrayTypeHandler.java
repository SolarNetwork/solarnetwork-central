/* ==================================================================
 * JodaLocalTimeArrayTypeHandler.java - Jun 21, 2011 8:45:24 PM
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

/**
 * Array handler for LocalTime objects stored as java.sql.Time.
 * 
 * @author matt
 * @version $Revision$
 */
public class JodaLocalTimeArrayTypeHandler extends ArrayTypeHandler {

	public JodaLocalTimeArrayTypeHandler() {
		super("time");
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter,
			String jdbcType) throws SQLException {
		if ( parameter == null ) {
            ps.setNull(i, Types.ARRAY);
        } else {
            Object[] input = (Object[])parameter;
            Time[] output = new Time[input.length];
            for ( int j = 0, len = input.length; j < len; j++ ) {
            	output[j] = JodaLocalTimeTypeHandlerCallback.getTime(input[j]);
            }
            
            Connection conn = ps.getConnection();
            Array loc = conn.createArrayOf(elementJdbcType, output);
            ps.setArray(i, loc);
        }
	}
	
	
	
}
