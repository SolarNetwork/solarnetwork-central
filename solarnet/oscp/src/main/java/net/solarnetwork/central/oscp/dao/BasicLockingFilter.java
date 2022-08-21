/* ==================================================================
 * BasicLockingFilter.java - 21/08/2022 5:38:27 pm
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

import java.util.List;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Basic implementation of {@link LockingFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public record BasicLockingFilter(Integer max, Integer offset, boolean lockResults,
		boolean skipLockedResults) implements LockingFilter {

	/** A locking filter for selecting just one with result rows locked. */
	public static final LockingFilter ONE_FOR_UPDATE = new BasicLockingFilter(1, null, true, false);

	/**
	 * A locking filter for selecting just one with result rows locked and
	 * skipping already locked rows.
	 */
	public static final LockingFilter ONE_FOR_UPDATE_SKIP = new BasicLockingFilter(1, null, true, true);

	@Override
	public boolean isLockResults() {
		return lockResults;
	}

	@Override
	public boolean isSkipLockedResults() {
		return skipLockedResults;
	}

	@Override
	public List<SortDescriptor> getSorts() {
		return null;
	}

	@Override
	public Integer getOffset() {
		return offset;
	}

	@Override
	public Integer getMax() {
		return max;
	}

}
