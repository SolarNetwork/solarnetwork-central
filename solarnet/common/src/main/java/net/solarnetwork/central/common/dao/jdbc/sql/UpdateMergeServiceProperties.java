/* ==================================================================
 * UpdateMergeServiceProperties.java - 21/07/2026 6:59:58 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * SQL update to merge a set of service properties into a user-related entity.
 * 
 * <p>
 * The general SQL structure takes one of two forms. For {@code Simple} mode:
 * </p>
 * 
 * <pre>{@code
 * UPDATE my_table
 * SET sprops = COALESCE(sprops, '{}'::jsonb) || ?::jsonb
 * WHERE user_id = ?
 * AND id = ?
 * }</pre>
 * 
 * <p>
 * For the recursive modes:
 * </p>
 * 
 * <pre>{@code
 * UPDATE my_table
 * SET sprops = solarcommon.jsonb_recursive_merge(COALESCE(sprops, '{}'::jsonb), ?::jsonb, ?)
 * WHERE user_id = ?
 * AND id = ?
 * }</pre>
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateMergeServiceProperties implements PreparedStatementCreator, SqlProvider {

	/** A common {@code servicePropertiesColumnName} value. */
	public static final String SERVICE_PROPERTIES_COLUMN_NAME = "sprops";

	/** A common column name for the user ID. */
	public static final String USER_ID_COLUMN_NAME = "user_id";

	/** A common column name for the entity ID. */
	public static final String ENTITY_ID_COLUMN_NAME = "id";

	/**
	 * A merge mode enumeration.
	 */
	public enum MergeMode {
		/** A simple merge of top-level properties only. */
		Simple,

		/** A recursive merge of objects. */
		RecursiveObjects,

		/** A recusrive merge of objects and arrays. */
		RecursiveObjectsAndArrays,
	}

	/**
	 * A common {@code (user_id, id)} composite key column naming function.
	 * 
	 * @param index
	 *        the column index
	 * @return the column name
	 */
	public static String common2ColumnName(int index) {
		if ( index == 0 ) {
			return USER_ID_COLUMN_NAME;
		} else if ( index == 1 ) {
			return ENTITY_ID_COLUMN_NAME;
		}
		throw new IllegalArgumentException("Unsupported column index [%d]".formatted(index));
	}

	private final String tableName;
	private final IntFunction<String> idColumnNameProvider;
	private final String servicePropertiesColumnName;
	private final UserRelatedCompositeKey<?> id;
	private final MergeMode mode;
	private final Map<String, ?> serviceProperties;

	/**
	 * Constructor.
	 * 
	 * @param tableName
	 *        the SQL table name
	 * @param idColumnNameProvider
	 *        a SQL ID column name provider
	 * @param servicePropertiesColumnName
	 *        the service properties column name
	 * @param id
	 *        the ID of the entity to update
	 * @param mode
	 *        the update mode
	 * @param serviceProperties
	 *        the service properties to merge
	 */
	public UpdateMergeServiceProperties(String tableName, IntFunction<String> idColumnNameProvider,
			String servicePropertiesColumnName, UserRelatedCompositeKey<?> id, MergeMode mode,
			Map<String, ?> serviceProperties) {
		this.tableName = requireNonNullArgument(tableName, "tableName");
		this.idColumnNameProvider = requireNonNullArgument(idColumnNameProvider, "idColumnNameProvider");
		this.servicePropertiesColumnName = requireNonNullArgument(servicePropertiesColumnName,
				"servicePropertiesColumnName");
		this.id = requireNonNullArgument(id, "id");
		this.mode = requireNonNullArgument(mode, "mode");
		this.serviceProperties = requireNonNullArgument(serviceProperties, "serviceProperties");
	}

	/**
	 * Create an instance using common settings for a 2-column composite key
	 * like {@code (user_id, id)}.
	 * 
	 * @param tableName
	 *        the table name
	 * @param id
	 *        the ID of the entity to update
	 * @param mode
	 *        the update mode
	 * @param serviceProperties
	 *        the service properties to merge
	 * @return the new instance
	 */
	public static UpdateMergeServiceProperties common2Column(String tableName,
			UserRelatedCompositeKey<?> id, MergeMode mode, Map<String, ?> serviceProperties) {
		return new UpdateMergeServiceProperties(tableName,
				UpdateMergeServiceProperties::common2ColumnName, SERVICE_PROPERTIES_COLUMN_NAME, id,
				mode, serviceProperties);
	}

	@Override
	public @Nullable String getSql() {
		StringBuilder buf = new StringBuilder(64);
		buf.append("UPDATE ").append(tableName).append('\n');
		buf.append("SET ").append(servicePropertiesColumnName).append(" = ");
		if ( mode == MergeMode.Simple ) {
			buf.append("COALESCE(").append(servicePropertiesColumnName)
					.append(", '{}'::jsonb) || ?::jsonb\n");
		} else {
			buf.append("solarcommon.jsonb_recursive_merge(COALESCE(").append(servicePropertiesColumnName)
					.append(", '{}'::jsonb), ?::jsonb, ?)\n");
		}
		buf.append("WHERE ");
		for ( int i = 0, len = id.keyComponentLength(); i < len; i++ ) {
			if ( i > 0 ) {
				buf.append("AND ");
			}
			buf.append(idColumnNameProvider.apply(i)).append(" = ?\n");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, JsonUtils.getJSONString(serviceProperties, "{}"));
		if ( mode != MergeMode.Simple ) {
			stmt.setBoolean(++p, mode == MergeMode.RecursiveObjectsAndArrays);
		}
		for ( int i = 0, len = id.keyComponentLength(); i < len; i++ ) {
			stmt.setObject(++p, id.keyComponent(i));
		}
		return stmt;
	}

}
