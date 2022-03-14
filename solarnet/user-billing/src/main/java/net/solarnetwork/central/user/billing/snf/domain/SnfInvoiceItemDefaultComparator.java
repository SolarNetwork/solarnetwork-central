/* ==================================================================
 * SnfInvoiceItemDefaultComparator.java - 28/07/2020 9:02:34 AM
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

package net.solarnetwork.central.user.billing.snf.domain;

import java.util.Comparator;
import java.util.UUID;

/**
 * A default sorting implementation for invoice items.
 * 
 * <p>
 * This sorts items using the following properties:
 * </p>
 * 
 * <ol>
 * <li>item type</li>
 * <li>node ID (metadata) (if available)</li>
 * <li>key</li>
 * <li>ID</lI>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoiceItemDefaultComparator implements Comparator<SnfInvoiceItem> {

	@SuppressWarnings("unchecked")
	private <T> T metaValue(SnfInvoiceItem item, String key, Class<? extends T> clazz) {
		Object v = (item != null && item.getMetadata() != null ? item.getMetadata().get(key) : null);
		return (v != null && clazz.isAssignableFrom(v.getClass()) ? (T) v : null);
	}

	@Override
	public int compare(SnfInvoiceItem o1, SnfInvoiceItem o2) {
		// first compare by type
		InvoiceItemType t1 = o1.getItemType();
		InvoiceItemType t2 = o2.getItemType();
		int result = t1.compareTo(t2);
		if ( result != 0 ) {
			return result;
		}

		// next compare by node ID, with non-node items sorted first
		Number n1 = metaValue(o1, SnfInvoiceItem.META_NODE_ID, Number.class);
		Number n2 = metaValue(o2, SnfInvoiceItem.META_NODE_ID, Number.class);
		if ( n1 != null && n2 != null ) {
			long l1 = n1.longValue();
			long l2 = n2.longValue();
			result = (l1 < l2 ? -1 : l1 > l2 ? 1 : 0);
			if ( result != 0 ) {
				return result;
			}
		}

		// next compare by key, with NULLs sorted first
		String k1 = o1.getKey();
		String k2 = o2.getKey();
		if ( k1 == null ) {
			return -1;
		} else if ( k2 == null ) {
			return 1;
		}
		try {
			NodeUsageType u1 = NodeUsageType.forKey(k1);
			NodeUsageType u2 = NodeUsageType.forKey(k2);
			result = Integer.compare(u1.getOrder(), u2.getOrder());
		} catch ( IllegalArgumentException e ) {
			result = k1.compareTo(k2);
		}
		if ( result != 0 ) {
			return result;
		}

		// finally, compare by ID, with NULLs sorted first
		UUID id1 = o1.getId();
		UUID id2 = o2.getId();
		if ( id1 == null ) {
			return -1;
		} else if ( id2 == null ) {
			return 1;
		}
		return id1.compareTo(id2);
	}

}
