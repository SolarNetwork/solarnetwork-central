/* ==================================================================
 * MyBatisDatumAppEventAcceptor.java - 5/06/2020 9:52:37 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dao.mybatis;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.DatumAppEvent;

/**
 * MyBatis implementation of {@link DatumAppEventAcceptor} that creates user
 * datum tasks out of datum events.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumAppEventAcceptor extends BaseMyBatisDao implements DatumAppEventAcceptor {

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void offerDatumEvent(DatumAppEvent event) {
		getSqlSession().insert("create-tasks-from-event", event);
	}

}
