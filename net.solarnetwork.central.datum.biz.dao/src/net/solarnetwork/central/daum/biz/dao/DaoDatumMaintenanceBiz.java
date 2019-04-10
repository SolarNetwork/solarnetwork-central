/* ==================================================================
 * DaoDatumMaintenanceBiz.java - 10/04/2019 9:03:02 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.daum.biz.dao;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;

/**
 * DAO based implementation of {@link DatumMaintenanceBiz}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public class DaoDatumMaintenanceBiz implements DatumMaintenanceBiz {

	private final GeneralNodeDatumDao datumDao;

	/**
	 * Constructor.
	 * 
	 * @param datumDao
	 *        the datum DAO to use
	 */
	public DaoDatumMaintenanceBiz(GeneralNodeDatumDao datumDao) {
		super();
		this.datumDao = datumDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void markDatumAggregatesStale(GeneralNodeDatumFilter criteria) {
		datumDao.markDatumAggregatesStale(criteria);

	}

}
