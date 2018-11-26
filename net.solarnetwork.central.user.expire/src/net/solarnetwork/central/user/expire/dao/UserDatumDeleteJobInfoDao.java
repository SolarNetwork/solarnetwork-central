/* ==================================================================
 * UserDatumDeleteJobInfoDao.java - 26/11/2018 7:00:56 AM
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

package net.solarnetwork.central.user.expire.dao;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.dao.ClaimableJobDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;

/**
 * DAO API for {@link DatumDeleteJobInfo} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDatumDeleteJobInfoDao extends GenericDao<DatumDeleteJobInfo, UserUuidPK>,
		ClaimableJobDao<GeneralNodeDatumFilter, Long, DatumDeleteJobState, DatumDeleteJobInfo, UserUuidPK> {

	/**
	 * Find all available job info entities for a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to get the entities for
	 * @param states
	 *        an optional set of states to restrict the results to, or
	 *        {@literal null} for any state
	 * @return the matching results, never {@literal null}
	 */
	List<DatumDeleteJobInfo> findForUser(Long userId, Set<DatumDeleteJobState> states);

	/**
	 * Delete job info entities for a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to get the entities for
	 * @param jobIds
	 *        if provided, a set of job IDs to delete; otherwise all jobs for
	 *        user are deleted
	 * @param states
	 *        an optional set of states to restrict the deletion to, or
	 *        {@literal null} for any state
	 * @return the number of deleted jobs
	 */
	int deleteForUser(Long userId, Set<UUID> jobIds, Set<DatumDeleteJobState> states);

}
