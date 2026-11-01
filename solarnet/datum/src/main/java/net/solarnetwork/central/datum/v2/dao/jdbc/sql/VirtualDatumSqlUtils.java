/* ==================================================================
 * VirtualDatumSqlUtils.java - 8/12/2020 5:16:52 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static java.lang.String.format;
import java.util.Map;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.service.support.TextResourceCache;

/**
 * SQL utilities for virtual datum.
 *
 * @author matt
 * @version 2.1
 * @since 3.8
 */
public final class VirtualDatumSqlUtils {

	private VirtualDatumSqlUtils() {
		// don't construct me
	}

	/** A datum property column name for combining. */
	public static final String PROPERTY_COMBINE_COLUMN_NAME = "val";

	/** A datum reading difference column name for combining. */
	public static final String READING_DIFF_COMBINE_COLUMN_NAME = "rdiff";

	/** A datum property ordering column name for combining. */
	public static final String PROPERTY_COMBINE_ORDER_COLUMN_NAME = "prank";

	/**
	 * Generate a SQL clause for calculating a combined datum property value.
	 *
	 * <p>
	 * The {@link #PROPERTY_COMBINE_ORDER_COLUMN_NAME} value will be used as the
	 * ordering column name.
	 * </p>
	 *
	 * @param type
	 *        the combining type
	 * @param columnName
	 *        the property column name
	 * @return the SQL clause
	 * @see #combineCalculationSql(CombiningType, String, String)
	 */
	public static String combineCalculationSql(CombiningType type, String columnName) {
		return combineCalculationSql(type, columnName, PROPERTY_COMBINE_ORDER_COLUMN_NAME);
	}

	/**
	 * Generate a SQL clause for calculating a combined datum property value.
	 *
	 * @param type
	 *        the combining type
	 * @param columnName
	 *        the property column name
	 * @param orderColumnName
	 *        the property ordering column name
	 * @return the SQL clause
	 */
	public static String combineCalculationSql(CombiningType type, String columnName,
			String orderColumnName) {
		return switch (type) {
			case Average -> String.format("AVG(%s)", columnName);
			case Difference -> String.format(
					"SUM(CASE %2$s WHEN 1 THEN %1$s ELSE -%1$s END ORDER BY %2$s)", columnName,
					orderColumnName);
			case Sum -> String.format("SUM(%s)", columnName);
			default -> throw new IllegalArgumentException(
					format("The CombiningType %s is not supported.", type));
		};
	}

	private static final String DEFAULT_PROPERTY_COMBINE_AVG_SQL = combineCalculationSql(
			CombiningType.Average, PROPERTY_COMBINE_COLUMN_NAME);
	private static final String DEFAULT_PROPERTY_COMBINE_SUM_SQL = combineCalculationSql(
			CombiningType.Sum, PROPERTY_COMBINE_COLUMN_NAME);
	private static final String DEFAULT_PROPERTY_COMBINE_SUB_SQL = combineCalculationSql(
			CombiningType.Difference, PROPERTY_COMBINE_COLUMN_NAME);
	private static final String DEFAULT_READING_DIFF_COMBINE_AVG_SQL = combineCalculationSql(
			CombiningType.Average, READING_DIFF_COMBINE_COLUMN_NAME);
	private static final String DEFAULT_READING_DIFF_COMBINE_SUM_SQL = combineCalculationSql(
			CombiningType.Sum, READING_DIFF_COMBINE_COLUMN_NAME);
	private static final String DEFAULT_READING_DIFF_COMBINE_SUB_SQL = combineCalculationSql(
			CombiningType.Difference, READING_DIFF_COMBINE_COLUMN_NAME);

	/**
	 * The SQL resource template parameter name for the property combine
	 * calculation.
	 */
	public static final String PROPERTY_COMBINE_CALC_PARAM_NAME = "PROPERTY_CALC";

	/**
	 * The SQL resource template parameter name for the reading difference
	 * combine calculation.
	 */
	public static final String READING_DIFF_COMBINE_CALC_PARAM_NAME = "READING_DIFF_CALC";

	/**
	 * The SQL select columns for the real streams that contributed to each
	 * result of a {@link #combineContributorsCteSql()} query.
	 *
	 * @since 2.1
	 */
	public static final String COMBINE_CONTRIBUTORS_SELECT_SQL = ", dc.rnode_ids, dc.rsource_ids";

	/**
	 * The SQL join for the {@link #combineContributorsCteSql()} CTE.
	 *
	 * <p>
	 * Both sides of this join have the same keys, so it returns the same rows
	 * as an inner join. A full join is used because it forces a hash join on
	 * both keys: with an inner join the planner can choose a merge join on the
	 * stream ID alone, which was about 10x slower.
	 * </p>
	 *
	 * @since 2.1
	 */
	public static final String COMBINE_CONTRIBUTORS_JOIN_SQL = "FULL OUTER JOIN dc ON dc.stream_id = datum.stream_id AND dc.ts = datum.ts_start\n";

	/**
	 * Get a SQL group of CTE expressions for combining datum streams into
	 * virtual stream results.
	 *
	 * @param type
	 *        the type of combining to perform
	 * @return the SQL
	 * @throws IllegalArgumentException
	 *         if {@code type} is not supported
	 */
	public static String combineCteSql(CombiningType type) {
		Map<String, String> templates = switch (type) {
			case Average -> Map.of(PROPERTY_COMBINE_CALC_PARAM_NAME, DEFAULT_PROPERTY_COMBINE_AVG_SQL,
					READING_DIFF_COMBINE_CALC_PARAM_NAME, DEFAULT_READING_DIFF_COMBINE_AVG_SQL);

			case Difference -> Map.of(PROPERTY_COMBINE_CALC_PARAM_NAME, DEFAULT_PROPERTY_COMBINE_SUB_SQL,
					READING_DIFF_COMBINE_CALC_PARAM_NAME, DEFAULT_READING_DIFF_COMBINE_SUB_SQL);

			case Sum -> Map.of(PROPERTY_COMBINE_CALC_PARAM_NAME, DEFAULT_PROPERTY_COMBINE_SUM_SQL,
					READING_DIFF_COMBINE_CALC_PARAM_NAME, DEFAULT_READING_DIFF_COMBINE_SUM_SQL);

			default -> throw new IllegalArgumentException(
					format("The CombiningType %s is not supported.", type));
		};
		return TextResourceCache.INSTANCE.getResourceAsString("datum-combine-cte.sql",
				VirtualDatumSqlUtils.class, DatumSqlUtils.SQL_COMMENT, templates);
	}

	/**
	 * Get a SQL CTE expression for the real streams that contributed to each
	 * result of a {@link #combineCteSql(CombiningType)} query.
	 *
	 * <p>
	 * This must follow the {@link #combineCteSql(CombiningType)} SQL. The
	 * {@code d} CTE must include {@code rnode_id} and {@code rsource_id}
	 * columns.
	 * </p>
	 *
	 * @return the SQL
	 * @since 2.1
	 */
	public static String combineContributorsCteSql() {
		return TextResourceCache.INSTANCE.getResourceAsString("datum-combine-contributors-cte.sql",
				VirtualDatumSqlUtils.class, DatumSqlUtils.SQL_COMMENT, Map.of());
	}
}
