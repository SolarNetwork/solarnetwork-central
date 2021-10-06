/* ==================================================================
 * DaoUserMetadataBiz.java - 11/11/2016 5:05:25 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao;

import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.user.biz.UserMetadataBiz;
import net.solarnetwork.central.user.dao.UserMetadataDao;
import net.solarnetwork.central.user.domain.UserMetadataEntity;
import net.solarnetwork.central.user.domain.UserMetadataFilter;
import net.solarnetwork.central.user.domain.UserMetadataFilterMatch;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * DAO-based implementation of {@link UserMetadataBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoUserMetadataBiz implements UserMetadataBiz {

	private final UserMetadataDao userMetadataDao;

	/**
	 * Constructor.
	 * 
	 * @param userMetadataDao
	 *        the DAO to use
	 */
	public DaoUserMetadataBiz(UserMetadataDao userMetadataDao) {
		super();
		this.userMetadataDao = userMetadataDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addUserMetadata(Long userId, GeneralDatumMetadata meta) {
		assert userId != null;
		assert meta != null;
		UserMetadataEntity um = userMetadataDao.get(userId);
		GeneralDatumMetadata newMeta = meta;
		if ( um == null ) {
			um = new UserMetadataEntity();
			um.setCreated(Instant.now());
			um.setId(userId);
			newMeta = meta;
		} else if ( um.getMeta() != null && um.getMeta().equals(meta) == false ) {
			newMeta = new GeneralDatumMetadata(um.getMeta());
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && newMeta.equals(um.getMeta()) == false ) {
			// have changes, so persist
			um.setMeta(newMeta);
			userMetadataDao.store(um);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeUserMetadata(Long userId, GeneralDatumMetadata meta) {
		assert userId != null;
		assert meta != null;
		UserMetadataEntity um = userMetadataDao.get(userId);
		if ( um == null ) {
			um = new UserMetadataEntity();
			um.setCreated(Instant.now());
			um.setId(userId);
			um.setMeta(meta);
		} else {
			um.setMeta(meta);
		}
		userMetadataDao.store(um);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeUserMetadata(Long userId) {
		UserMetadataEntity meta = userMetadataDao.get(userId);
		if ( meta != null ) {
			userMetadataDao.delete(meta);
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<UserMetadataFilterMatch> findUserMetadata(UserMetadataFilter criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return userMetadataDao.findFiltered(criteria, sortDescriptors, offset, max);
	}

}
