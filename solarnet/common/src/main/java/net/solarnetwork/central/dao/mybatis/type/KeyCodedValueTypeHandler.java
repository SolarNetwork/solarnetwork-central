/* ==================================================================
 * KeyCodedValueTypeHandler.java - 10/03/2026 7:31:31 am
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

package net.solarnetwork.central.dao.mybatis.type;

import static net.solarnetwork.domain.KeyCodedValue.forKeyCode;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.KeyCodedValue;

/**
 * Type handler for enum values that implement {@link KeyCodedValue} so that an
 * character database column is used for storage.
 * 
 * @param <E>
 *        the enum type
 * @author matt
 * @version 1.0
 */
public class KeyCodedValueTypeHandler<E extends Enum<E> & KeyCodedValue> extends BaseTypeHandler<E> {

	/**
	 * {@link KeyCodedValue} enum handler that defaults to code {@literal '0'}.
	 * <p>
	 * This will attempt to resolve a code value {@literal '0'} for a default
	 * value, falling back to {@code null} if that is not found.
	 * </p>
	 * 
	 * @param <E>
	 *        the enum type
	 */
	public static class Null<E extends Enum<E> & KeyCodedValue> extends KeyCodedValueTypeHandler<E> {

		/**
		 * Constructor.
		 * 
		 * @param type
		 *        the type
		 */
		public Null(Class<E> type) {
			super(type, forKeyCode('0', type.getEnumConstants(), null));
		}
	}

	private final Class<E> type;
	private final E[] values;
	private final @Nullable E defaultValue;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This will attempt to resolve a code value {@literal 0} for a default
	 * value, falling back to {@code null} if that is not found.
	 * </p>
	 * 
	 * @param type
	 *        the type
	 */
	public KeyCodedValueTypeHandler(Class<E> type) {
		this(type, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type
	 * @param defaultValue
	 *        the default value to use if no matching code is found
	 */
	public KeyCodedValueTypeHandler(Class<E> type, @Nullable E defaultValue) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.values = type.getEnumConstants();
		this.defaultValue = defaultValue;
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, @Nullable E parameter,
			@Nullable JdbcType jdbcType) throws SQLException {
		super.setParameter(ps, i, parameter == null && defaultValue != null ? defaultValue : parameter,
				jdbcType);
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter,
			@Nullable JdbcType jdbcType) throws SQLException {
		ps.setString(i, String.valueOf(parameter.getKeyCode()));
	}

	@Override
	public @Nullable E getNullableResult(ResultSet rs, String columnName) throws SQLException {
		final String code = rs.getString(columnName);
		if ( code == null || code.isEmpty() ) {
			return defaultValue;
		}
		return forKeyCode(code.charAt(0), values, defaultValue);
	}

	@Override
	public @Nullable E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		final String code = rs.getString(columnIndex);
		if ( code == null || code.isEmpty() ) {
			return defaultValue;
		}
		return forKeyCode(code.charAt(0), values, defaultValue);
	}

	@Override
	public @Nullable E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		final String code = cs.getString(columnIndex);
		if ( code == null || code.isEmpty() ) {
			return defaultValue;
		}
		return forKeyCode(code.charAt(0), values, defaultValue);
	}

	/**
	 * Get the class type.
	 * 
	 * @return the type, never {@code null}
	 */
	public Class<E> getType() {
		return type;
	}

}
