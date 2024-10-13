/* ==================================================================
 * DatumWriteOnlyDao.java - 26/02/2024 11:52:10 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.Datum;

/**
 * API for a write-only datum DAO.
 *
 * @author matt
 * @version 1.1
 */
public interface DatumWriteOnlyDao
		extends GenericWriteOnlyDao<GeneralObjectDatum<? extends GeneralObjectDatumKey>, DatumPK> {

	/**
	 * Store a datum.
	 *
	 * @param datum
	 *        the datum to store
	 * @return the stored primary key
	 */
	DatumPK store(DatumEntity datum);

	/**
	 * Store a datum.
	 *
	 * @param datum
	 *        the datum to store
	 * @return the stored primary key
	 * @since 1.1
	 */
	DatumPK store(Datum datum);

}
