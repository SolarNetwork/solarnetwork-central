/* ==================================================================
 * MyBatisGeneralNodeDatumAuxiliaryDao.java - 4/02/2019 9:11:46 am
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

package net.solarnetwork.central.datum.dao.mybatis;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumAuxiliaryDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;

/**
 * MyBatis implementation of {@link GeneralNodeDatumAuxiliaryDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.19
 */
public class MyBatisGeneralNodeDatumAuxiliaryDao extends
		BaseMyBatisFilterableDao<GeneralNodeDatumAuxiliary, GeneralNodeDatumAuxiliaryFilterMatch, GeneralNodeDatumAuxiliaryFilter, GeneralNodeDatumAuxiliaryPK>
		implements GeneralNodeDatumAuxiliaryDao {

	/**
	 * Constructor.
	 */
	public MyBatisGeneralNodeDatumAuxiliaryDao() {
		super(GeneralNodeDatumAuxiliary.class, GeneralNodeDatumAuxiliaryPK.class,
				GeneralNodeDatumAuxiliaryMatch.class);
	}

}
