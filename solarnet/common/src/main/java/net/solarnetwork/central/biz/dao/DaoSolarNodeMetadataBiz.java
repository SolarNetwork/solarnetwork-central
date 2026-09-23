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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * DAO-based implementation of {@link SolarNodeMetadataBiz}.
 *
 * @author matt
 * @version 3.1
 */
public class DaoSolarNodeMetadataBiz implements SolarNodeMetadataBiz {

	private final SolarNodeMetadataDao solarNodeMetadataDao;

	/**
	 * Constructor.
	 *
	 * @param solarNodeMetadataDao
	 *        the node metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoSolarNodeMetadataBiz(SolarNodeMetadataDao solarNodeMetadataDao) {
		super();
		this.solarNodeMetadataDao = requireNonNullArgument(solarNodeMetadataDao, "solarNodeMetadataDao");
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
		} else if ( snm.getMeta() != null && !snm.getMeta().equals(meta) ) {
			newMeta = new GeneralDatumMetadata(snm.getMeta());
			newMeta.merge(meta, true);
		}
		if ( !newMeta.equals(snm.getMeta()) ) {
			// have changes, so persist
			snm.setMeta(newMeta);
			solarNodeMetadataDao.save(snm);
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
		solarNodeMetadataDao.save(snm);
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
	public FilterResults<SolarNodeMetadata, Long> findSolarNodeMetadata(SolarNodeMetadataFilter criteria,
			@Nullable List<SortDescriptor> sortDescriptors, @Nullable Long offset,
			@Nullable Integer max) {
		return solarNodeMetadataDao.findFiltered(daoCriteria(criteria), sortDescriptors, offset, max);
	}

	/**
	 * Translate a legacy filter into the criteria supported by the DAO.
	 *
	 * <p>
	 * The legacy {@code metadataFilter} becomes the DAO search filter, which
	 * uses the same node metadata path syntax. Sort and pagination criteria are
	 * passed to the DAO as arguments, so are not translated here.
	 * </p>
	 *
	 * @param criteria
	 *        the criteria to translate
	 * @return the DAO criteria, never {@code null}
	 */
	private static BasicCoreCriteria daoCriteria(SolarNodeMetadataFilter criteria) {
		requireNonNullArgument(criteria, "criteria");
		var result = new BasicCoreCriteria();
		result.setNodeIds(criteria.getNodeIds());
		result.setSearchFilter(criteria.getMetadataFilter());
		// when the actor is a token, restrict the metadata of the results to that
		// token policy's node metadata paths; a null token ID leaves the criteria alone
		result.setTokenId(SecurityUtils.currentTokenId());
		return result;
	}

}
