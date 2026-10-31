/* ==================================================================
 * GncSlot.java - 11/10/2026 2:44:53 pm
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

import java.time.Instant;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * A GnuCash slot: one key/value property of a GnuCash object.
 *
 * <p>
 * A slot named with a path, like {@code gncInvoice/invoice-guid}, belongs to a
 * frame slot named {@code gncInvoice}, and the frame's {@code guidValue} is the
 * {@code objGuid} of the slots inside it. GnuCash writes {@literal 0} and the
 * epoch to the integer and time columns a slot does not use, and {@code null}
 * to the rest.
 * </p>
 *
 * @param objGuid
 *        the GUID of the object, or frame, the slot belongs to
 * @param name
 *        the name
 * @param type
 *        the value type, such as {@link #TYPE_STRING}
 * @param int64Value
 *        the integer value
 * @param stringValue
 *        the string value
 * @param timespecValue
 *        the timestamp value
 * @param guidValue
 *        the GUID value, or the GUID of the frame's slots
 * @param gdateValue
 *        the date value
 * @author matt
 * @version 1.0
 */
public record GncSlot(String objGuid, String name, int type, long int64Value,
		@Nullable String stringValue, Instant timespecValue, @Nullable String guidValue,
		@Nullable LocalDate gdateValue) {

	/** The integer value type. */
	public static final int TYPE_INT64 = 1;

	/** The string value type. */
	public static final int TYPE_STRING = 4;

	/** The GUID value type. */
	public static final int TYPE_GUID = 5;

	/** The timestamp value type. */
	public static final int TYPE_TIMESPEC = 6;

	/** The frame type, which holds other slots. */
	public static final int TYPE_FRAME = 9;

	/** The date value type. */
	public static final int TYPE_GDATE = 10;

	/**
	 * Create an integer slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the slot
	 */
	public static GncSlot int64(String objGuid, String name, long value) {
		return new GncSlot(objGuid, name, TYPE_INT64, value, null, Instant.EPOCH, null, null);
	}

	/**
	 * Create a string slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the slot
	 */
	public static GncSlot string(String objGuid, String name, String value) {
		return new GncSlot(objGuid, name, TYPE_STRING, 0, value, Instant.EPOCH, null, null);
	}

	/**
	 * Create a GUID slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the slot
	 */
	public static GncSlot guid(String objGuid, String name, String value) {
		return new GncSlot(objGuid, name, TYPE_GUID, 0, null, Instant.EPOCH, value, null);
	}

	/**
	 * Create a frame slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param frameGuid
	 *        the GUID that the slots in the frame belong to
	 * @return the slot
	 */
	public static GncSlot frame(String objGuid, String name, String frameGuid) {
		return new GncSlot(objGuid, name, TYPE_FRAME, 0, null, Instant.EPOCH, frameGuid, null);
	}

	/**
	 * Create a timestamp slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the slot
	 */
	public static GncSlot timespec(String objGuid, String name, Instant value) {
		return new GncSlot(objGuid, name, TYPE_TIMESPEC, 0, null, value, null, null);
	}

	/**
	 * Create a date slot.
	 *
	 * @param objGuid
	 *        the GUID of the object the slot belongs to
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 * @return the slot
	 */
	public static GncSlot gdate(String objGuid, String name, LocalDate value) {
		return new GncSlot(objGuid, name, TYPE_GDATE, 0, null, Instant.EPOCH, null, value);
	}

}
