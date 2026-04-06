/* ==================================================================
 * GenericWriteOnlyDaoDatumAdapter.java - 21/03/2026 12:47:28 pm
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
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Adapts {@link GenericWriteOnlyDao} as a {@link DatumWriteOnlyDao}.
 *
 * <p>
 * This is the logical inverse of the {@link DatumWriteOnlyDaoGenericAdapter}
 * class.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class GenericWriteOnlyDaoDatumAdapter implements DatumWriteOnlyDao {

	private final GenericWriteOnlyDao<Object, DatumPK> queue;

	/**
	 * Constructor.
	 *
	 * @param queue
	 *        the queue to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GenericWriteOnlyDaoDatumAdapter(GenericWriteOnlyDao<Object, DatumPK> queue) {
		this.queue = requireNonNullArgument(queue, "queue");
	}

	@Override
	public @Nullable DatumPK persist(GeneralObjectDatum<? extends GeneralObjectDatumKey> entity) {
		return queue.persist(entity);
	}

	@Override
	public @Nullable DatumPK store(StreamDatum datum) {
		return queue.persist(datum);
	}

	@Override
	public @Nullable DatumPK store(Datum datum) {
		return queue.persist(datum);
	}

}
