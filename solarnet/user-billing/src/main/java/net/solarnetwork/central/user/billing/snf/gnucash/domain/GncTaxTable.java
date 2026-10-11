/* ==================================================================
 * GncTaxTable.java - 11/10/2026 2:44:32 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.domain;

import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A GnuCash tax table.
 *
 * <p>
 * When GnuCash posts an invoice it points each line at a hidden copy of the
 * line's tax table, so that later changes to the table do not change posted
 * invoices. A copy is {@code invisible} and has the original table as its
 * {@code parentGuid}.
 * </p>
 *
 * @param guid
 *        the GUID
 * @param name
 *        the name
 * @param refcount
 *        the number of objects that use the table
 * @param invisible
 *        {@code true} for a hidden copy made when posting
 * @param parentGuid
 *        the GUID of the table this is a hidden copy of
 * @param entries
 *        the entries
 * @author matt
 * @version 1.0
 */
public record GncTaxTable(String guid, String name, long refcount, boolean invisible,
		@Nullable String parentGuid, List<GncTaxTableEntry> entries) {

	private static final Comparator<GncTaxTableEntry> ENTRY_ORDER = Comparator
			.comparing(GncTaxTableEntry::accountGuid).thenComparingInt(GncTaxTableEntry::type)
			.thenComparing(e -> e.amount().toBigDecimal());

	/**
	 * Create a hidden copy of this table, as GnuCash does when posting an
	 * invoice.
	 *
	 * @param guid
	 *        the GUID of the copy
	 * @return the copy
	 */
	public GncTaxTable postedCopy(String guid) {
		return new GncTaxTable(guid, name, 0, true, this.guid, entries);
	}

	/**
	 * Test if another table has the same entries as this one, ignoring their
	 * order and how their amounts are expressed.
	 *
	 * @param other
	 *        the table to compare with
	 * @return {@code true} if the entries are the same
	 */
	public boolean hasSameEntries(GncTaxTable other) {
		if ( entries.size() != other.entries.size() ) {
			return false;
		}
		final List<GncTaxTableEntry> a = entries.stream().sorted(ENTRY_ORDER).toList();
		final List<GncTaxTableEntry> b = other.entries.stream().sorted(ENTRY_ORDER).toList();
		for ( int i = 0, len = a.size(); i < len; i++ ) {
			GncTaxTableEntry e1 = a.get(i);
			GncTaxTableEntry e2 = b.get(i);
			if ( !(e1.accountGuid().equals(e2.accountGuid()) && e1.type() == e2.type()
					&& e1.amount().toBigDecimal().compareTo(e2.amount().toBigDecimal()) == 0) ) {
				return false;
			}
		}
		return true;
	}

}
