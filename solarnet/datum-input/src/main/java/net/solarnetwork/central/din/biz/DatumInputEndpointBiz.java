/* ==================================================================
 * DatumInputEndpointBiz.java - 21/02/2024 6:53:13 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.biz;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import org.springframework.util.MimeType;
import net.solarnetwork.domain.datum.DatumId;

/**
 * API for a datum input endpoint service.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumInputEndpointBiz {

	/**
	 * Import datum.
	 *
	 * @param userId
	 *        the endpoint owner user ID
	 * @param endpointId
	 *        the endpoint ID to import to
	 * @param contentType
	 *        the data content type
	 * @param in
	 *        the data stream to import
	 * @return the collection of datum IDs successfully imported
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	Collection<DatumId> importDatum(Long userId, UUID endpointId, MimeType contentType, InputStream in)
			throws IOException;
}
