/* ==================================================================
 * BasicFilter.java - 5/08/2023 12:20:00 pm
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

package net.solarnetwork.central.dnp3.dao;

import net.solarnetwork.central.common.dao.BasicCoreCriteria;

/**
 * Basic implementation of DNP3 query filter.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicFilter extends BasicCoreCriteria implements CertificateFilter {

	private String[] subjectDns;

	/**
	 * Set the certificate subject DN.
	 * 
	 * @param subjectDn
	 *        the subject DN to set
	 */
	public void setSubjectDn(String subjectDn) {
		setSubjectDns(new String[] { subjectDn });
	}

	/**
	 * Get the certificate subject DNs.
	 * 
	 * @return the subject DNs
	 */
	@Override
	public String[] getSubjectDns() {
		return subjectDns;
	}

	/**
	 * Set the certificate subject DNs.
	 * 
	 * @param subjectDns
	 *        the subject DNs to set
	 */
	public void setSubjectDns(String[] subjectDns) {
		this.subjectDns = subjectDns;
	}

}
