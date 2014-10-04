/* ==================================================================
 * DaoDatumMetadataBiz.java - Oct 3, 2014 4:02:04 PM
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

package net.solarnetwork.central.daum.biz.dao;

import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumMetadataDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link DatumMetadataBiz}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>generalNodeDatumMetadataDao</dt>
 * <dd>The {@link GeneralNodeDatumMetadataDao} to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class DaoDatumMetadataBiz implements DatumMetadataBiz {

	private GeneralNodeDatumMetadataDao generalNodeDatumMetadataDao = null;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert sourceId != null;
		assert meta != null;
		NodeSourcePK pk = new NodeSourcePK(nodeId, sourceId);
		GeneralNodeDatumMetadata gdm = generalNodeDatumMetadataDao.get(pk);
		GeneralDatumMetadata newMeta = meta;
		if ( gdm == null ) {
			gdm = new GeneralNodeDatumMetadata();
			gdm.setCreated(new DateTime());
			gdm.setId(pk);
			newMeta = meta;
		} else if ( gdm.getMeta() != null && gdm.getMeta().equals(meta) == false ) {
			newMeta = new GeneralDatumMetadata(gdm.getMeta());
			if ( meta.getTags() != null ) {
				for ( String tag : meta.getTags() ) {
					newMeta.addTag(tag);
				}
			}
			if ( meta.getInfo() != null ) {
				for ( Map.Entry<String, Object> me : meta.getInfo().entrySet() ) {
					// do not overwrite keys, only add
					if ( newMeta.getInfo() == null
							|| newMeta.getInfo().containsKey(me.getKey()) == false ) {
						newMeta.putInfoValue(me.getKey(), me.getValue());
					}
				}
			}
			if ( meta.getPropertyInfo() != null ) {
				Map<String, Map<String, Object>> gdmPropertyMeta = newMeta.getPropertyInfo();
				if ( gdmPropertyMeta == null ) {
					newMeta.setPropertyInfo(meta.getPropertyInfo());
				} else {
					for ( Map.Entry<String, Map<String, Object>> me : meta.getPropertyInfo().entrySet() ) {
						if ( gdmPropertyMeta.get(me.getKey()) == null ) {
							gdmPropertyMeta.put(me.getKey(), me.getValue());
						} else {
							for ( Map.Entry<String, Object> pme : me.getValue().entrySet() ) {
								if ( gdmPropertyMeta.get(me.getKey()).containsKey(pme.getKey()) ) {
									continue;
								}
								newMeta.putInfoValue(me.getKey(), pme.getKey(), pme.getValue());
							}
						}
					}
				}
			}
		}
		if ( newMeta != null && newMeta.equals(gdm.getMeta()) == false ) {
			// have changes, so persist
			gdm.setMeta(newMeta);
			generalNodeDatumMetadataDao.store(gdm);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert sourceId != null;
		assert meta != null;
		NodeSourcePK pk = new NodeSourcePK(nodeId, sourceId);
		GeneralNodeDatumMetadata gdm = generalNodeDatumMetadataDao.get(pk);
		if ( gdm == null ) {
			gdm = new GeneralNodeDatumMetadata();
			gdm.setCreated(new DateTime());
			gdm.setId(pk);
			gdm.setMeta(meta);
		} else {
			gdm.setMeta(meta);
		}
		generalNodeDatumMetadataDao.store(gdm);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeGeneralNodeDatumMetadata(Long nodeId, String sourceId) {
		GeneralNodeDatumMetadata meta = generalNodeDatumMetadataDao.get(new NodeSourcePK(nodeId,
				sourceId));
		if ( meta != null ) {
			generalNodeDatumMetadataDao.delete(meta);
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return generalNodeDatumMetadataDao.findFiltered(criteria, sortDescriptors, offset, max);
	}

	public GeneralNodeDatumMetadataDao getGeneralNodeDatumMetadataDao() {
		return generalNodeDatumMetadataDao;
	}

	public void setGeneralNodeDatumMetadataDao(GeneralNodeDatumMetadataDao generalNodeDatumMetadataDao) {
		this.generalNodeDatumMetadataDao = generalNodeDatumMetadataDao;
	}

}
