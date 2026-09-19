/* ==================================================================
 * DatumWriteOnlyDaoGenericAdapter.java - 19/03/2026 5:32:21 pm
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

package net.solarnetwork.central.datum.v2.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Adapts {@link DatumWriteOnlyDao} as a {@link GenericWriteOnlyDao}.
 *
 * <p>
 * This is the logical inverse of the {@link GenericWriteOnlyDaoDatumAdapter}
 * class.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumWriteOnlyDaoGenericAdapter implements GenericWriteOnlyDao<Object, DatumPK> {

	private final DatumWriteOnlyDao delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumWriteOnlyDaoGenericAdapter(DatumWriteOnlyDao delegate) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public @Nullable DatumPK persist(Object entity) {
		return switch (entity) {
			case GeneralObjectDatum<?> gd -> delegate.persist(gd);
			case StreamDatum sd -> delegate.store(sd);
			case Datum cd -> delegate.store(cd);
			default -> throw new IllegalArgumentException("Unsupported datum type: " + entity);
		};
	}

}
