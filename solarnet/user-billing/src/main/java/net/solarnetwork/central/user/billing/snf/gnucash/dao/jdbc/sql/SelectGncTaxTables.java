/* ==================================================================
 * SelectGncTaxTables.java - 11/10/2026 2:47:17 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql;

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getBoolean;
import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getNumeric;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTable;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTableEntry;

/**
 * Select GnuCash tax tables, with their entries.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncTaxTables implements PreparedStatementCreator, SqlProvider {

	/**
	 * The criteria to select tax tables by.
	 */
	public enum Criteria {

		/** Select a visible tax table by name. */
		Name,

		/** Select a tax table by GUID. */
		Guid,

		/** Select the hidden copies of a tax table, by the table's GUID. */
		Parent,

	}

	/** A result set extractor for the results. */
	public static final ResultSetExtractor<List<GncTaxTable>> EXTRACTOR = SelectGncTaxTables::extract;

	private final String schema;
	private final Criteria criteria;
	private final String value;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param criteria
	 *        the criteria to select by
	 * @param value
	 *        the criteria value
	 */
	public SelectGncTaxTables(String schema, Criteria criteria, String value) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.criteria = requireNonNullArgument(criteria, "criteria");
		this.value = requireNonNullArgument(value, "value");
	}

	@Override
	public String getSql() {
		final String where = switch (criteria) {
			case Name -> "t.name = ? AND t.invisible = 0 AND t.parent IS NULL";
			case Guid -> "t.guid = ?";
			case Parent -> "t.parent = ?";
		};
		return """
				SELECT t.guid, t.name, t.refcount, t.invisible, t.parent
					, e.account, e.amount_num, e.amount_denom, e.type
				FROM {schema}.taxtables t
				LEFT OUTER JOIN {schema}.taxtable_entries e ON e.taxtable = t.guid
				WHERE {where}
				ORDER BY t.guid, e.id
				""".replace("{schema}", schema).replace("{where}", where);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, value);
		return stmt;
	}

	private static List<GncTaxTable> extract(ResultSet rs) throws SQLException {
		final Map<String, GncTaxTable> tables = new LinkedHashMap<>(4);
		while ( rs.next() ) {
			final String guid = rs.getString(1);
			GncTaxTable table = tables.get(guid);
			if ( table == null ) {
				table = new GncTaxTable(guid, rs.getString(2), rs.getLong(3), getBoolean(rs, 4),
						rs.getString(5), new ArrayList<>(2));
				tables.put(guid, table);
			}
			final String account = rs.getString(6);
			if ( account != null ) {
				table.entries().add(new GncTaxTableEntry(account, getNumeric(rs, 7), rs.getInt(9)));
			}
		}
		return new ArrayList<>(tables.values());
	}

}
