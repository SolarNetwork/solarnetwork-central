/* ==================================================================
 * LazyDatumMetadataOperations.java - 13/03/2025 10:06:24 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Lazily resolve datum metadata, with caching support.
 *
 * @author matt
 * @version 1.0
 */
public final class LazyDatumMetadataOperations implements DatumMetadataOperations {

	private final ObjectDatumStreamMetadataId id;
	private final DatumStreamMetadataDao dao;
	private final Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> cache;

	private GeneralDatumMetadata meta;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param dao
	 *        the DAO
	 * @param cache
	 *        the optional cache
	 * @throws IllegalArgumentException
	 *         if {@code id} or {@code dao} are {@code null}, or the
	 *         {@code objectId} or {@code kind} properties on the given
	 *         {@code id} are {@code null}
	 */
	public LazyDatumMetadataOperations(ObjectDatumStreamMetadataId id, DatumStreamMetadataDao dao,
			Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> cache) {
		super();
		this.id = requireNonNullArgument(id, "id");
		if ( id.getKind() == null || id.getObjectId() == null ) {
			throw new IllegalArgumentException("Both objectId and kind ID values are required.");
		}
		this.dao = requireNonNullArgument(dao, "dao");
		this.cache = cache;
	}

	private GeneralDatumMetadata meta() {
		if ( meta != null ) {
			return meta;
		}
		if ( cache != null ) {
			meta = cache.get(id);
			if ( meta != null ) {
				return meta;
			}
		}

		// lookup from DAO
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(id.getKind());
		if ( id.getKind() == ObjectDatumKind.Location ) {
			filter.setLocationId(id.getObjectId());
		} else {
			filter.setNodeId(id.getObjectId());
		}
		filter.setSourceId(id.getSourceId());
		ObjectDatumStreamMetadata m = StreamSupport
				.stream(dao.findDatumStreamMetadata(filter).spliterator(), false).findAny().orElse(null);
		if ( m != null ) {
			meta = DatumUtils.getObjectFromJSON(m.getMetaJson(), GeneralDatumMetadata.class);
		}

		if ( meta != null && cache != null ) {
			cache.put(id, meta);
		} else {
			meta = new GeneralDatumMetadata();
		}

		return meta;
	}

	@Override
	public Set<String> getInfoKeys() {
		return meta().getInfoKeys();
	}

	@Override
	public Object getInfo(String key) {
		return meta().getInfo(key);
	}

	@Override
	public boolean hasInfo(String key) {
		return meta().hasInfo(key);
	}

	@Override
	public boolean hasPropertyInfo(String key) {
		return meta().hasPropertyInfo(key);
	}

	public boolean removeTag(String... tags) {
		return meta().removeTag(tags);
	}

	@Override
	public boolean hasInfo(String property, String key) {
		return meta().hasInfo(property, key);
	}

	public void populate(KeyValuePair[] data) {
		meta().populate(data);
	}

	@Override
	public boolean isEmpty() {
		return meta().isEmpty();
	}

	@Override
	public boolean differsFrom(DatumMetadataOperations other) {
		return meta().differsFrom(other);
	}

	@Override
	public Set<String> getTags() {
		return meta().getTags();
	}

	public void setTags(Set<String> tags) {
		meta().setTags(tags);
	}

	public Set<String> getT() {
		return meta().getT();
	}

	public void setT(Set<String> set) {
		meta().setT(set);
	}

	@Override
	public boolean hasTag(String tag) {
		return meta().hasTag(tag);
	}

	@Override
	public boolean hasMetadataAtPath(String path) {
		return meta().hasMetadataAtPath(path);
	}

	public boolean addTag(String tag) {
		return meta().addTag(tag);
	}

	@Override
	public String toString() {
		return meta().toString();
	}

	public boolean removeTag(String tag) {
		return meta().removeTag(tag);
	}

	public void clear() {
		meta().clear();
	}

	@Override
	public Set<String> getPropertyInfoKeys() {
		return meta().getPropertyInfoKeys();
	}

	@Override
	public Map<String, ?> getPropertyInfo(String key) {
		return meta().getPropertyInfo(key);
	}

	public void merge(DatumMetadataOperations meta, boolean replace) {
		this.meta().merge(meta, replace);
	}

	@Override
	public Number getInfoNumber(String key) {
		return meta().getInfoNumber(key);
	}

	public void putInfoValue(String key, Object value) {
		meta().putInfoValue(key, value);
	}

	@Override
	public Short getInfoShort(String key) {
		return meta().getInfoShort(key);
	}

	@Override
	public int hashCode() {
		return meta().hashCode();
	}

	@Override
	public Integer getInfoInteger(String key) {
		return meta().getInfoInteger(key);
	}

	@Override
	public boolean equals(Object obj) {
		return meta().equals(obj);
	}

	@Override
	public Long getInfoLong(String key) {
		return meta().getInfoLong(key);
	}

	@Override
	public Float getInfoFloat(String key) {
		return meta().getInfoFloat(key);
	}

	@Override
	public Map<String, Object> getInfo() {
		return meta().getInfo();
	}

	@Override
	public Double getInfoDouble(String key) {
		return meta().getInfoDouble(key);
	}

	public void setInfo(Map<String, Object> info) {
		meta().setInfo(info);
	}

	public Map<String, Object> getM() {
		return meta().getM();
	}

	public void setM(Map<String, Object> map) {
		meta().setM(map);
	}

	@Override
	public BigDecimal getInfoBigDecimal(String key) {
		return meta().getInfoBigDecimal(key);
	}

	public Map<String, Map<String, Object>> getPropertyInfo() {
		return meta().getPropertyInfo();
	}

	@Override
	public BigInteger getInfoBigInteger(String key) {
		return meta().getInfoBigInteger(key);
	}

	public void setInfo(String key, Map<String, Object> info) {
		meta().setInfo(key, info);
	}

	public void setPropertyInfo(Map<String, Map<String, Object>> propertyInfo) {
		meta().setPropertyInfo(propertyInfo);
	}

	@Override
	public String getInfoString(String key) {
		return meta().getInfoString(key);
	}

	public Map<String, Map<String, Object>> getPm() {
		return meta().getPm();
	}

	public void setPm(Map<String, Map<String, Object>> map) {
		meta().setPm(map);
	}

	@Override
	public Number getInfoNumber(String property, String key) {
		return meta().getInfoNumber(property, key);
	}

	public void putInfoValue(String property, String key, Object value) {
		meta().putInfoValue(property, key, value);
	}

	@Override
	public Short getInfoShort(String property, String key) {
		return meta().getInfoShort(property, key);
	}

	@Override
	public Object metadataAtPath(String path) {
		return meta().metadataAtPath(path);
	}

	@Override
	public <T> T metadataAtPath(String path, Class<T> clazz) {
		return meta().metadataAtPath(path, clazz);
	}

	@Override
	public Integer getInfoInteger(String property, String key) {
		return meta().getInfoInteger(property, key);
	}

	@Override
	public Long getInfoLong(String property, String key) {
		return meta().getInfoLong(property, key);
	}

	@Override
	public Float getInfoFloat(String property, String key) {
		return meta().getInfoFloat(property, key);
	}

	@Override
	public Double getInfoDouble(String property, String key) {
		return meta().getInfoDouble(property, key);
	}

	@Override
	public BigDecimal getInfoBigDecimal(String property, String key) {
		return meta().getInfoBigDecimal(property, key);
	}

	@Override
	public BigInteger getInfoBigInteger(String property, String key) {
		return meta().getInfoBigInteger(property, key);
	}

	@Override
	public String getInfoString(String property, String key) {
		return meta().getInfoString(property, key);
	}

}
