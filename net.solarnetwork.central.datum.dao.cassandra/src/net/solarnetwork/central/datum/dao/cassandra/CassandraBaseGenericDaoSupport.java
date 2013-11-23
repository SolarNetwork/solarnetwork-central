/* ==================================================================
 * CassandraConsumptionDatumDao.java - Nov 22, 2013 1:28:15 PM
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

package net.solarnetwork.central.datum.dao.cassandra;

import java.io.Serializable;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.domain.Entity;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

/**
 * Base implementation of {@link GenericDao} using Apache Cassandra.
 * 
 * <p>
 * TODO
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class CassandraBaseGenericDaoSupport<T extends Entity<PK>, PK extends Serializable>
		extends SqlMapClientDaoSupport implements GenericDao<T, PK> {

}
