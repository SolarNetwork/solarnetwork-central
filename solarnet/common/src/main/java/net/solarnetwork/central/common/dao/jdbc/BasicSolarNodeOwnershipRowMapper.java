/* ==================================================================
 * BasicSolarNodeOwnershipRowMapper.java - 6/10/2021 11:25:58 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * Map datum rows into {@link BasicSolarNodeOwnership} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>node_id</li>
 * <li>user_id</li>
 * <li>country (optional)</li>
 * <li>time zone (optional)</li>
 * <li>private (optional)</li>
 * <li>archived (optional)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSolarNodeOwnershipRowMapper implements RowMapper<SolarNodeOwnership> {

	/** A default instance. */
	public static final RowMapper<SolarNodeOwnership> INSTANCE = new BasicSolarNodeOwnershipRowMapper();

	@Override
	public SolarNodeOwnership mapRow(ResultSet rs, int rowNum) throws SQLException {
		final int colCount = rs.getMetaData().getColumnCount();
		Long nodeId = (Long) rs.getObject(1);
		Long userId = (Long) rs.getObject(2);
		String country = null;
		if ( colCount >= 3 ) {
			country = rs.getString(3);
		}
		ZoneId zone = ZoneOffset.UTC;
		if ( colCount >= 4 ) {
			try {
				zone = ZoneId.of(rs.getString(4));
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		boolean priv = false;
		if ( colCount >= 5 ) {
			priv = rs.getBoolean(5);
		}
		boolean arch = false;
		if ( colCount >= 6 ) {
			arch = rs.getBoolean(6);
		}
		return new BasicSolarNodeOwnership(nodeId, userId, country, zone, priv, arch);
	}

}
