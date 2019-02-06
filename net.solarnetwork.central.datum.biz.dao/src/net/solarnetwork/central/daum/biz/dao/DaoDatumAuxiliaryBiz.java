/* ==================================================================
 * DaoDatumAuxiliaryBiz.java - 4/02/2019 12:24:16 pm
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

import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumAuxiliaryDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;

/**
 * DAO based implementation of {@link DatumAuxiliaryBiz}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class DaoDatumAuxiliaryBiz implements DatumAuxiliaryBiz {

	private final GeneralNodeDatumAuxiliaryDao datumAuxiliaryDao;

	/**
	 * Constructor.
	 * 
	 * @param datumAuxiliaryDao
	 *        the DAO to use
	 */
	public DaoDatumAuxiliaryBiz(GeneralNodeDatumAuxiliaryDao datumAuxiliaryDao) {
		super();
		this.datumAuxiliaryDao = datumAuxiliaryDao;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public GeneralNodeDatumAuxiliary getGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		GeneralNodeDatumAuxiliary aux = datumAuxiliaryDao.get(id);
		if ( aux == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		return aux;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliary datum) {
		datumAuxiliaryDao.store(datum);

	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		GeneralNodeDatumAuxiliary aux = datumAuxiliaryDao.get(id);
		if ( aux == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		datumAuxiliaryDao.delete(aux);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> findGeneralNodeDatumAuxiliary(
			GeneralNodeDatumAuxiliaryFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return datumAuxiliaryDao.findFiltered(criteria, sortDescriptors, offset, max);
	}

}
