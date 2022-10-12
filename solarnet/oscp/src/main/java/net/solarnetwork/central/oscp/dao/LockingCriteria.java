/* ==================================================================
 * LockingCriteria.java - 18/08/2022 1:09:44 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao;

/**
 * A result-locking criteria.
 * 
 * @author matt
 * @version 1.0
 */
public interface LockingCriteria {

	/**
	 * Test if locked results are desired.
	 * 
	 * @return {@literal true} if the results of the query should be locked such
	 *         that other transactions are not allowed to update them until the
	 *         current transaction completes
	 */
	boolean isLockResults();

	/**
	 * Test if already locked results should be skipped when querying, or wait
	 * for the lock(s) to be released..
	 * 
	 * @return {@literal true} if the results of the query should ignore any
	 *         already-locked rows that other transactions have already locked,
	 *         {@literal false} if this query should wait for the locks to be
	 *         released before continuing
	 */
	boolean isSkipLockedResults();

}
