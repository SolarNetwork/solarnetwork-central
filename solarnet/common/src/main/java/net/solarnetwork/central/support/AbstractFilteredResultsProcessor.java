/* ==================================================================
 * AbstractFilteredResultsProcessor.java - 6/08/2022 9:27:16 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.io.IOException;
import java.util.Map;
import org.springframework.util.MimeType;

/**
 * Abstract convenience implementation of {@link FilteredResultsProcessor}.
 * 
 * @param <R>
 *        the result item type
 * @author matt
 * @version 1.0
 */
public abstract class AbstractFilteredResultsProcessor<R> implements FilteredResultsProcessor<R> {

	@Override
	public void flush() throws IOException {
		// NO-OP
	}

	@Override
	public void close() throws IOException {
		// NO-OP
	}

	@Override
	public MimeType getMimeType() {
		return MimeType.valueOf("application/octet-stream");
	}

	@Override
	public void start(Long totalResultCount, Integer startingOffset, Integer expectedResultCount,
			Map<String, ?> attributes) throws IOException {
		// NO-OP
	}

}
