/* ==================================================================
 * GeneralNodeDatumBulkLoadingContext.java - 2/12/2020 12:07:05 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.dao.BulkLoadingDao.LoadingOptions;


/**
 * FIXME
 * 
 * <p>TODO</p>
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumBulkLoadingContext implements LoadingContext<GeneralNodeDatum> {

	/**
	 * 
	 */
	public GeneralNodeDatumBulkLoadingContext() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public LoadingOptions getOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void load(GeneralNodeDatum entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getLoadedCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCommittedCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GeneralNodeDatum getLastLoadedEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createCheckpoint() {
		// TODO Auto-generated method stub

	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rollback() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
