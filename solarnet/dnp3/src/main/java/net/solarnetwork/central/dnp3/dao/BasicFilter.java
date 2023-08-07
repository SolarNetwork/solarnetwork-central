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
import net.solarnetwork.central.common.dao.CertificateCriteria;
import net.solarnetwork.central.common.dao.EnabledCriteria;
import net.solarnetwork.central.common.dao.IndexCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of DNP3 query filter.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicFilter extends BasicCoreCriteria implements CertificateFilter, ServerFilter {

	private Integer[] indexes;
	private String[] subjectDns;
	private Long[] serverIds;

	@Override
	public BasicFilter clone() {
		return (BasicFilter) super.clone();
	}

	/**
	 * Constructor.
	 */
	public BasicFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof EnabledCriteria f ) {
			setEnabled(f.getEnabled());
		}
		if ( criteria instanceof IndexCriteria f ) {
			setIndexes(f.getIndexes());
		}
		if ( criteria instanceof CertificateCriteria f ) {
			setSubjectDns(f.getSubjectDns());
		}
		if ( criteria instanceof ServerCriteria f ) {
			setServerIds(f.getServerIds());
		}
	}

	/**
	 * Set the index.
	 * 
	 * @param index
	 *        the index to set
	 */
	public void setIndex(Integer index) {
		setIndexes(index != null ? new Integer[] { index } : null);
	}

	/**
	 * Get the indexes
	 * 
	 * @return the indexes
	 */
	@Override
	public Integer[] getIndexes() {
		return indexes;
	}

	/**
	 * Set the indexes.
	 * 
	 * @param indexes
	 *        the indexes to set
	 */
	public void setIndexes(Integer[] indexes) {
		this.indexes = indexes;
	}

	/**
	 * Set the certificate subject DN.
	 * 
	 * @param subjectDn
	 *        the subject DN to set
	 */
	public void setSubjectDn(String subjectDn) {
		setSubjectDns(subjectDn != null ? new String[] { subjectDn } : null);
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

	/**
	 * Set the server ID.
	 * 
	 * @param serverId
	 *        the server ID to set
	 */
	public void setServerId(Long serverId) {
		setServerIds(serverId != null ? new Long[] { serverId } : null);
	}

	/**
	 * Get the server IDs.
	 * 
	 * @return the server IDs
	 */
	@Override
	public Long[] getServerIds() {
		return serverIds;
	}

	/**
	 * Set the server IDs.
	 * 
	 * @param serverIds
	 *        the server IDs to set
	 */
	public void setServerIds(Long[] serverIds) {
		this.serverIds = serverIds;
	}

}
