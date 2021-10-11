/* ==================================================================
 * DaoAuditDatumBiz.java - 12/07/2018 5:25:10 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.biz.dao;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO based implementation of {@link AuditDatumBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoAuditDatumBiz implements AuditDatumBiz {

	private final AuditDatumDao auditDatumDao;

	/**
	 * Constructor.
	 * 
	 * @param auditDatumDao
	 *        the DAO to use
	 */
	public DaoAuditDatumBiz(AuditDatumDao auditDatumDao) {
		super();
		this.auditDatumDao = auditDatumDao;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter) {
		return auditDatumDao.findAuditDatumFiltered(filter);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter) {
		return auditDatumDao.findAccumulativeAuditDatumFiltered(filter);
	}

}
