/* ==================================================================
 * JdbcSolarNodeOwnershipDao.java - 28/02/2020 2:57:32 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.cache.Cache;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectSolarNodeOwnership;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserAuthTokenNodes;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * JDBC implementation of {@link SolarNodeOwnershipDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcSolarNodeOwnershipDao implements SolarNodeOwnershipDao {

	/**
	 * The SQL for finding metadata ID values for a single stream ID.
	 * 
	 * @since 1.1
	 */
	public static final String FIND_METADATA_IDS_FOR_STREAM_ID = """
			SELECT stream_id, obj_id, source_id, kind
			FROM solardatm.find_metadata_for_stream(?)
			""";

	private final JdbcOperations jdbcOps;
	private Cache<Long, SolarNodeOwnership> nodeOwnershipCache;
	private Cache<UUID, ObjectDatumStreamMetadataId> streamMetadataIdCache;

	/**
	 * Metadata ID provider implementation that returns an empty map.
	 * 
	 * @param streamIds
	 *        the stream IDs (unused)
	 * @return an empty map
	 */
	public static Map<UUID, ObjectDatumStreamMetadataId> emptyMetadataIdProvider(UUID... streamIds) {
		return Map.of();
	}

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcSolarNodeOwnershipDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public SolarNodeOwnership ownershipForNodeId(Long nodeId) {
		if ( nodeId == null ) {
			return null;
		}
		SolarNodeOwnership result = null;
		Cache<Long, SolarNodeOwnership> cache = getNodeOwnershipCache();
		if ( cache != null ) {
			result = cache.get(nodeId);
			if ( result != null ) {
				return result;
			}
		}
		List<SolarNodeOwnership> results = getJdbcOps().query(
				SelectSolarNodeOwnership.selectForNode(nodeId),
				BasicSolarNodeOwnershipRowMapper.INSTANCE);
		if ( !results.isEmpty() ) {
			result = results.getFirst();
			if ( result != null && cache != null ) {
				cache.put(nodeId, result);
			}
		}
		return result;
	}

	@Override
	public SolarNodeOwnership[] ownershipsForUserId(Long userId) {
		if ( userId == null ) {
			return null;
		}
		SolarNodeOwnership[] result = null;
		List<SolarNodeOwnership> results = getJdbcOps().query(
				SelectSolarNodeOwnership.selectForUser(userId),
				BasicSolarNodeOwnershipRowMapper.INSTANCE);
		if ( !results.isEmpty() ) {
			result = results.toArray(SolarNodeOwnership[]::new);
		}
		return result;
	}

	@Override
	public Long[] nonArchivedNodeIdsForToken(String tokenId) {
		if ( tokenId == null ) {
			return new Long[0];
		}
		List<Long> results = getJdbcOps().query(new SelectUserAuthTokenNodes(tokenId),
				new ColumnRowMapper<>(2, Long.class));
		return results.toArray(Long[]::new);
	}

	@Override
	public Map<UUID, ObjectDatumStreamMetadataId> getDatumStreamMetadataIds(UUID... streamIds) {
		if ( streamIds == null || streamIds.length < 1 ) {
			return Collections.emptyMap();
		}

		final Map<UUID, ObjectDatumStreamMetadataId> result = new LinkedHashMap<>(streamIds.length);
		final List<UUID> queryList = (streamMetadataIdCache != null ? new ArrayList<>(streamIds.length)
				: Arrays.asList(streamIds));
		if ( streamMetadataIdCache != null ) {
			for ( UUID streamId : streamIds ) {
				ObjectDatumStreamMetadataId id = streamMetadataIdCache.get(streamId);
				if ( id != null ) {
					result.put(streamId, id);
				} else {
					queryList.add(streamId);
				}
			}
		}

		if ( queryList.isEmpty() ) {
			return Collections.unmodifiableMap(result);
		}

		jdbcOps.execute((ConnectionCallback<Void>) con -> {

			try (PreparedStatement stmt = con.prepareStatement(FIND_METADATA_IDS_FOR_STREAM_ID)) {
				int resultNum = 0;
				for ( UUID streamId : queryList ) {
					stmt.setObject(1, streamId, Types.OTHER);
					try (ResultSet rs = stmt.executeQuery()) {
						if ( rs.next() ) {
							ObjectDatumStreamMetadataId id = ObjectDatumStreamMetadataIdRowMapper.INSTANCE
									.mapRow(rs, ++resultNum);
							result.put(streamId, id);
							if ( streamMetadataIdCache != null ) {
								streamMetadataIdCache.put(streamId, id);
							}
						}
					}
				}
			}

			return null;
		});
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Get the JDBC operations.
	 *
	 * @return the ops
	 */
	public JdbcOperations getJdbcOps() {
		return jdbcOps;
	}

	/**
	 * Get the cache of node IDs to associated node ownership.
	 *
	 * @return the cache, or {@literal null} if not available
	 */
	public Cache<Long, SolarNodeOwnership> getNodeOwnershipCache() {
		return nodeOwnershipCache;
	}

	/**
	 * Set the cache of node IDs to associated node ownership.
	 *
	 * @param nodeOwnershipCache
	 *        the cache to set
	 */
	public void setNodeOwnershipCache(Cache<Long, SolarNodeOwnership> nodeOwnershipCache) {
		this.nodeOwnershipCache = nodeOwnershipCache;
	}

	/**
	 * Get the stream metadata ID cache.
	 *
	 * @return the cache, or {@literal null}
	 * @since 1.1
	 */
	public Cache<UUID, ObjectDatumStreamMetadataId> getStreamMetadataIdCache() {
		return streamMetadataIdCache;
	}

	/**
	 * Set the stream metadata ID cache.
	 *
	 * @param streamMetadataIdCache
	 *        the cache to set
	 * @since 1.1
	 */
	public void setStreamMetadataIdCache(
			Cache<UUID, ObjectDatumStreamMetadataId> streamMetadataIdCache) {
		this.streamMetadataIdCache = streamMetadataIdCache;
	}

}
