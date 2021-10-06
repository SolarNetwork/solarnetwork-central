/* ==================================================================
 * MyBatisUserNodeCertificateDao.java - Nov 11, 2014 7:20:35 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.mybatis;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodePK;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis implementation of {@link UserNodeCertificateDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserNodeCertificateDao extends
		BaseMyBatisGenericDao<UserNodeCertificate, UserNodePK> implements UserNodeCertificateDao {

	/** The query name used for {@link #getActiveCertificateForNode(Long)}. */
	public static final String QUERY_ACTIVE_FOR_NODE = "get-UserNodeCertificate-for-active-node";

	/**
	 * Default constructor.
	 */
	public MyBatisUserNodeCertificateDao() {
		super(UserNodeCertificate.class, UserNodePK.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeCertificate getActiveCertificateForNode(Long nodeId) {
		return selectFirst(QUERY_ACTIVE_FOR_NODE, nodeId);
	}
}
