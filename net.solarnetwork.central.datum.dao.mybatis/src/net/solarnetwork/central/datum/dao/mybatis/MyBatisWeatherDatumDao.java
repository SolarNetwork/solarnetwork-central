/* ==================================================================
 * MyBatisWeatherDatumDao.java - Nov 13, 2014 7:01:00 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.mybatis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.LocationDatumFilter;
import net.solarnetwork.central.datum.domain.SkyCondition;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.datum.domain.WeatherDatumMatch;
import org.joda.time.ReadableDateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis implemenation of {@link WeatherDatumDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisWeatherDatumDao extends
		BaseMyBatisFilterableDatumDao<WeatherDatum, WeatherDatumMatch, LocationDatumFilter> implements
		WeatherDatumDao {

	/**
	 * The query name used for
	 * {@link #getMostRecentWeatherDatum(Long, ReadableDateTime)}.
	 */
	public static final String QUERY_WEATHER_DATUM_FOR_MOST_RECENT = "find-WeatherDatum-for-most-recent";

	private Map<Pattern, SkyCondition> conditionMapping;

	/**
	 * Default constructor.
	 */
	public MyBatisWeatherDatumDao() {
		super(WeatherDatum.class, WeatherDatumMatch.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public WeatherDatum getDatum(Long id) {
		WeatherDatum datum = super.getDatum(id);
		populateCondition(datum);
		return datum;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public WeatherDatum getDatumForDate(Long nodeId, ReadableDateTime date) {
		WeatherDatum result = super.getDatumForDate(nodeId, date);
		populateCondition(result);
		return result;
	}

	@Override
	protected String setupAggregatedDatumQuery(DatumQueryCommand criteria, Map<String, Object> params) {
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public WeatherDatum getMostRecentWeatherDatum(Long nodeId, ReadableDateTime upToDate) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("node", nodeId);
		params.put("upToDate", new java.sql.Timestamp(upToDate.getMillis()));
		List<WeatherDatum> results = selectList(QUERY_WEATHER_DATUM_FOR_MOST_RECENT, params, 0, 1);
		if ( results != null && results.size() > 0 ) {
			populateCondition(results);
			return results.get(0);
		}
		return null;
	}

	@Override
	protected List<WeatherDatumMatch> postProcessFilterQuery(LocationDatumFilter filter,
			List<WeatherDatumMatch> rows) {
		populateCondition(rows);
		return rows;
	}

	@Override
	protected List<WeatherDatum> postProcessDatumQuery(DatumQueryCommand criteria,
			Map<String, Object> params, List<WeatherDatum> results) {
		populateCondition(results);
		return results;
	}

	/**
	 * Populate the {@link WeatherDatum#getCondition()} value for each datum in
	 * the given list.
	 * 
	 * <p>
	 * This calls {@link #populateCondition(WeatherDatum)} on each datum in the
	 * given list.
	 * </p>
	 * 
	 * @param list
	 *        datums to set
	 * @see #populateCondition(WeatherDatum)
	 */
	private void populateCondition(List<? extends WeatherDatum> list) {
		if ( list == null || this.conditionMapping == null ) {
			return;
		}
		for ( WeatherDatum datum : list ) {
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
	 * Populate the {@link WeatherDatum#getCondition()} value for a datum.
	 * 
	 * <p>
	 * This uses the configured {@link #getConditionMapping()} to compare
	 * regular expressions against the {@link WeatherDatum#getSkyConditions()}
	 * value. The {@link SkyCondition} for the first pattern that matches in
	 * {@link #getConditionMapping()} iteration order will be used. If a datum
	 * already has a {@link WeatherDatum#getCondition()} value set, it will not
	 * be changed.
	 * </p>
	 * 
	 * @param list
	 *        datums to set
	 */
	private void populateCondition(WeatherDatum datum) {
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
