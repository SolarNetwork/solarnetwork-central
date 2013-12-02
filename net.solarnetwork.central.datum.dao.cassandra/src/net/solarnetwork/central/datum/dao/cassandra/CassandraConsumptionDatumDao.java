/* ==================================================================
 * CassandraConsumptionDatumDao.java - Nov 22, 2013 1:28:15 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.cassandra;

import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.domain.SortDescriptor;
import org.joda.time.LocalDate;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInterval;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class CassandraConsumptionDatumDao implements ConsumptionDatumDao {

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsumptionDatum getDatum(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long storeDatum(ConsumptionDatum datum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsumptionDatum getDatumForDate(Long id, ReadableDateTime date) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ConsumptionDatum> getAggregatedDatum(DatumQueryCommand criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ConsumptionDatum> getMostRecentDatum(DatumQueryCommand criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadableInterval getReportableInterval(Long nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadableInterval getReportableInterval() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAvailableSources(Long nodeId, LocalDate start, LocalDate end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends ConsumptionDatum> getObjectType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long store(ConsumptionDatum domainObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsumptionDatum get(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ConsumptionDatum> getAll(List<SortDescriptor> sortDescriptors) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(ConsumptionDatum domainObject) {
		// TODO Auto-generated method stub

	}

}
