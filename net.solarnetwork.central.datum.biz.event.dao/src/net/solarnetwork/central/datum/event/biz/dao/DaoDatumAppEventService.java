/* ==================================================================
 * DaoDatumAppEventService.java - 29/05/2020 5:48:01 pm
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

package net.solarnetwork.central.datum.event.biz.dao;

import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.dao.DatumAppEventDao;
import net.solarnetwork.central.datum.dao.DatumAppEventEntity;
import net.solarnetwork.central.datum.domain.DatumAppEvent;

/**
 * DAO based implementation of datum event services.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoDatumAppEventService implements DatumAppEventAcceptor {

	private final DatumAppEventDao eventDao;

	/**
	 * Constructor.
	 * 
	 * @param eventDao
	 *        the DAO
	 * @throws IllegalArgumentException
	 *         if {@code eventDao} is {@literal null}
	 */
	public DaoDatumAppEventService(DatumAppEventDao eventDao) {
		super();
		if ( eventDao == null ) {
			throw new IllegalArgumentException("The eventDao parameter must not be null.");
		}
		this.eventDao = eventDao;
	}

	@Override
	public void offerDatumEvent(DatumAppEvent event) {
		if ( event == null ) {
			return;
		}
		eventDao.save(new DatumAppEventEntity(event));
	}

}
