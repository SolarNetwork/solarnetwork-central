/* ==================================================================
 * CodedValueTypeHandler.java - 25/02/2020 7:42:31 pm
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

package net.solarnetwork.central.dao.mybatis.type;

import static net.solarnetwork.domain.CodedValue.forCodeValue;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import net.solarnetwork.domain.CodedValue;

/**
 * Type handler for enum values that implement {@link CodedValue} so that an
 * integer database column is used for storage.
 * 
 * @param <E>
 *        the enum type
 * @author matt
 * @version 1.0
 */
public class CodedValueTypeHandler<E extends Enum<E> & CodedValue> extends BaseTypeHandler<E> {

	/**
	 * {@link CodedValue} enum handler that defaults to code {@literal 0}.
	 * <p>
	 * This will attempt to resolve a code value {@literal 0} for a default
	 * value, falling back to {@literal null} if that is not found.
	 * </p>
	 * 
	 * @param <E>
	 *        the enum type
	 */
	public static class Zero<E extends Enum<E> & CodedValue> extends CodedValueTypeHandler<E> {

		/**
		 * Constructor.
		 * 
		 * @param type
		 *        the type
		 */
		public Zero(Class<E> type) {
			super(type, forCodeValue(0, type.getEnumConstants(), null));
		}
	}

	private final Class<E> type;
	private final E[] values;
	private final E defaultValue;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This will attempt to resolve a code value {@literal 0} for a default
	 * value, falling back to {@literal null} if that is not found.
	 * </p>
	 * 
	 * @param type
	 *        the type
	 */
	public CodedValueTypeHandler(Class<E> type) {
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
	public CodedValueTypeHandler(Class<E> type, E defaultValue) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.values = type.getEnumConstants();
		this.defaultValue = defaultValue;
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
			throws SQLException {
		super.setParameter(ps, i, parameter == null && defaultValue != null ? defaultValue : parameter,
				jdbcType);
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setInt(i, parameter.getCode());
	}

	@Override
	public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
		int code = rs.getInt(columnName);
		if ( rs.wasNull() ) {
			return defaultValue;
		}
		return forCodeValue(code, values, defaultValue);
	}

	@Override
	public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		int code = rs.getInt(columnIndex);
		if ( rs.wasNull() ) {
			return defaultValue;
		}
		return forCodeValue(code, values, defaultValue);
	}

	@Override
	public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		int code = cs.getInt(columnIndex);
		if ( cs.wasNull() ) {
			return defaultValue;
		}
		return forCodeValue(code, values, defaultValue);
	}

	/**
	 * Get the class type.
	 * 
	 * @return the type, never {@literal null}
	 */
	public Class<E> getType() {
		return type;
	}

}
