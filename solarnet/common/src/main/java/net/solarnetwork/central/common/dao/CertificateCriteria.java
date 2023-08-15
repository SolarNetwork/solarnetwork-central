/* ==================================================================
 * CertificateCriteria.java - 5/08/2023 12:15:02 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

/**
 * Search criteria for certificate related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface CertificateCriteria {

	/**
	 * Get the first subject DN.
	 * 
	 * <p>
	 * This returns the first available subject DN from the
	 * {@link #getSubjectDns()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first subject DN, or {@literal null} if not available
	 */
	default String getSubjectDn() {
		final String[] array = getSubjectDns();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of subject DNs.
	 * 
	 * @return array of subject DNs (may be {@literal null})
	 */
	String[] getSubjectDns();

	/**
	 * Test if this filter has any user certificate criteria.
	 * 
	 * @return {@literal true} if the subject DN is non-null
	 */
	default boolean hasCertificateCriteria() {
		return getSubjectDn() != null;
	}

}
