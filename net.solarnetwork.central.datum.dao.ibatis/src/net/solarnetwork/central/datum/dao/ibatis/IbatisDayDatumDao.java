/* ===================================================================
 * IbatisDayDatumDao.java
 * 
 * Created Aug 29, 2008 8:49:38 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.central.datum.dao.ibatis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.DayDatumMatch;
import net.solarnetwork.central.datum.domain.LocationDatumFilter;
import net.solarnetwork.central.datum.domain.SkyCondition;
import net.solarnetwork.central.domain.Aggregation;
import org.joda.time.LocalDate;
import org.joda.time.ReadableDateTime;

/**
 * Ibatis implementation of {@link DayDatumDao}.
 * 
 * @author matt.magoffin
 * @version 1.1
 */
public class IbatisDayDatumDao extends
		IbatisFilterableDatumDatoSupport<DayDatum, DayDatumMatch, LocationDatumFilter> implements
		DayDatumDao {

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * day-precise results.
	 */
	public static final String QUERY_DAY_DATUM_FOR_AGGREGATE_BY_DAY = "find-DayDatum-for-agg-day";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * month-precise results.
	 */
	public static final String QUERY_DAY_DATUM_FOR_AGGREGATE_BY_MONTH = "find-DayDatum-for-agg-month";

	private Map<Pattern, SkyCondition> conditionMapping;

	/**
	 * Default constructor.
	 */
	public IbatisDayDatumDao() {
		super(DayDatum.class, DayDatumMatch.class);
	}

	@Override
	public DayDatum getDatum(Long id) {
		DayDatum result = super.getDatum(id);
		populateCondition(result);
		return result;
	}

	@Override
	public DayDatum getDatumForDate(Long nodeId, ReadableDateTime date) {
		DayDatum result = super.getDatumForDate(nodeId, date);
		populateCondition(result);
		return result;
	}

	@Override
	public DayDatum getDatumForDate(Long locationId, LocalDate day) {
		return getDatumForDate(locationId, day.toDateTimeAtStartOfDay());
	}

	@Override
	protected String setupAggregatedDatumQuery(DatumQueryCommand criteria, Map<String, Object> params) {
		if ( criteria.getAggregate() != null && criteria.getAggregate().equals(Aggregation.Month) ) {
			return QUERY_DAY_DATUM_FOR_AGGREGATE_BY_MONTH;
		}
		return QUERY_DAY_DATUM_FOR_AGGREGATE_BY_DAY;
	}

	@Override
	protected List<DayDatum> postProcessDatumQuery(DatumQueryCommand criteria,
			Map<String, Object> params, List<DayDatum> results) {
		populateCondition(results);
		return results;
	}

	@Override
	protected List<DayDatumMatch> postProcessFilterQuery(LocationDatumFilter filter,
			List<DayDatumMatch> rows) {
		populateCondition(rows);
		return rows;
	}

	/**
	 * Populate the {@link DayDatum#getCondition()} value for each datum in the
	 * given list.
	 * 
	 * <p>
	 * This calls {@link #populateCondition(DayDatum)} on each datum in the
	 * given list.
	 * </p>
	 * 
	 * @param list
	 *        datums to set
	 * @see #populateCondition(DayDatum)
	 */
	private void populateCondition(List<? extends DayDatum> list) {
		if ( list == null || this.conditionMapping == null ) {
			return;
		}
		for ( DayDatum datum : list ) {
			if ( datum.getCondition() != null ) {
				continue;
			}
			String sky = datum.getSkyConditions();
			if ( sky == null ) {
				continue;
			}
			for ( Map.Entry<Pattern, SkyCondition> me : this.conditionMapping.entrySet() ) {
				if ( me.getKey().matcher(sky).find() ) {
					datum.setCondition(me.getValue());
					break;
				}
			}
		}
	}

	/**
	 * Populate the {@link DayDatum#getCondition()} value for a datum.
	 * 
	 * <p>
	 * This uses the configured {@link #getConditionMapping()} to compare
	 * regular expressions against the {@link DayDatum#getSkyConditions()}
	 * value. The {@link SkyCondition} for the first pattern that matches in
	 * {@link #getConditionMapping()} iteration order will be used. If a datum
	 * already has a {@link DayDatum#getCondition()} value set, it will not be
	 * changed.
	 * </p>
	 * 
	 * @param list
	 *        datums to set
	 */
	private void populateCondition(DayDatum datum) {
		if ( datum == null || this.conditionMapping == null ) {
			return;
		}
		if ( datum.getCondition() != null ) {
			return;
		}
		String sky = datum.getSkyConditions();
		if ( sky == null ) {
			return;
		}
		for ( Map.Entry<Pattern, SkyCondition> me : this.conditionMapping.entrySet() ) {
			if ( me.getKey().matcher(sky).find() ) {
				datum.setCondition(me.getValue());
				return;
			}
		}
	}

	/**
	 * Set the {@link #setConditionMapping(Map)} via String keys.
	 * 
	 * <p>
	 * This method is a convenience method for setting the
	 * {@code conditionMapping} property via String keys instead of compiled
	 * {@link Pattern} objects. The regular expressions are compiled with
	 * {@link Pattern#CASE_INSENSITIVE} and {@link Pattern#DOTALL} flags.
	 * </p>
	 * 
	 * @param conditionMapping
	 *        the mapping of regular expressions to SkyCondition instances
	 */
	public void setConditionMap(Map<String, SkyCondition> conditionMapping) {
		Map<Pattern, SkyCondition> map = new LinkedHashMap<Pattern, SkyCondition>();
		for ( Map.Entry<String, SkyCondition> me : conditionMapping.entrySet() ) {
			Pattern p = Pattern.compile(me.getKey(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			map.put(p, me.getValue());
		}
		setConditionMapping(map);
	}

	public Map<Pattern, SkyCondition> getConditionMapping() {
		return conditionMapping;
	}

	public void setConditionMapping(Map<Pattern, SkyCondition> conditionMapping) {
		this.conditionMapping = conditionMapping;
	}

}
