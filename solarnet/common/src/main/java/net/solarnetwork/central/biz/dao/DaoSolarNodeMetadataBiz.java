/* ==================================================================
 * DaoSolarNodeMetadataBiz.java - 11/11/2016 11:23:27 AM
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

package net.solarnetwork.central.biz.dao;

import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * DAO-based implementation of {@link SolarNodeMetadataBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoSolarNodeMetadataBiz implements SolarNodeMetadataBiz {

	private final SolarNodeMetadataDao solarNodeMetadataDao;

	/**
	 * Constructor.
	 * 
	 * @param solarNodeMetadataDao
	 *        the node metadata DAO to use
	 */
	public DaoSolarNodeMetadataBiz(SolarNodeMetadataDao solarNodeMetadataDao) {
		super();
		this.solarNodeMetadataDao = solarNodeMetadataDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert meta != null;
		SolarNodeMetadata snm = solarNodeMetadataDao.get(nodeId);
		GeneralDatumMetadata newMeta = meta;
		if ( snm == null ) {
			snm = new SolarNodeMetadata();
			snm.setCreated(Instant.now());
			snm.setId(nodeId);
			newMeta = meta;
		} else if ( snm.getMeta() != null && snm.getMeta().equals(meta) == false ) {
			newMeta = new GeneralDatumMetadata(snm.getMeta());
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && newMeta.equals(snm.getMeta()) == false ) {
			// have changes, so persist
			snm.setMeta(newMeta);
			solarNodeMetadataDao.store(snm);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert meta != null;
		SolarNodeMetadata snm = solarNodeMetadataDao.get(nodeId);
		if ( snm == null ) {
			snm = new SolarNodeMetadata();
			snm.setCreated(Instant.now());
			snm.setId(nodeId);
			snm.setMeta(meta);
		} else {
			snm.setMeta(meta);
		}
		solarNodeMetadataDao.store(snm);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeSolarNodeMetadata(Long nodeId) {
		SolarNodeMetadata meta = solarNodeMetadataDao.get(nodeId);
		if ( meta != null ) {
			solarNodeMetadataDao.delete(meta);
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(
			SolarNodeMetadataFilter criteria, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return solarNodeMetadataDao.findFiltered(criteria, sortDescriptors, offset, max);
	}

}
