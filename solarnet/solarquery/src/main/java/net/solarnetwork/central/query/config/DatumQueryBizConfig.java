/* ==================================================================
 * DatumQueryBizConfig.java - 8/10/2021 8:18:05 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.biz.dao.ReadingDatumCriteriaValidator;
import net.solarnetwork.central.query.biz.dao.ReadingDatumFilterValidator;
import net.solarnetwork.central.query.support.AuditingQueryBiz;

/**
 * Configuration for Datum Query business configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumQueryBizConfig {

	/** A qualifier used for reading datum criteria components. */
	public static final String READING_DATUM_CRITERIA = "reading-datum-criteria";

	/** A qualifier used for reading datum filter components. */
	public static final String READING_DATUM_FILTER = "reading-datum-filter";

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private DatumStreamMetadataDao streamMetadataDao;

	@Autowired
	private ReadingDatumDao readingDatumDao;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private QueryAuditor queryAuditor;

	public static class DatumQuerySettings {

		int filteredResultsLimit = 1000;
		int maxDaysForMinuteAggregation = 7;
		int maxDaysForHourAggregation = 31;
		int maxDaysForDayAggregation = 730;
		int maxDaysForDayOfWeekAggregation = 3650;
		int maxDaysForHourOfDayAggregation = 3650;
	}

	@Bean
	@ConfigurationProperties(prefix = "app.datum.query")
	public DatumQuerySettings datumQuerySettings() {
		return new DatumQuerySettings();
	}

	@Bean
	public QueryBiz queryBiz() {
		DatumQuerySettings settings = datumQuerySettings();
		DaoQueryBiz biz = new DaoQueryBiz(datumDao, streamMetadataDao, readingDatumDao,
				nodeOwnershipDao);
		biz.setFilteredResultsLimit(settings.filteredResultsLimit);
		biz.setMaxDaysForMinuteAggregation(settings.maxDaysForMinuteAggregation);
		biz.setMaxDaysForHourAggregation(settings.maxDaysForHourAggregation);
		biz.setMaxDaysForDayAggregation(settings.maxDaysForDayAggregation);
		biz.setMaxDaysForDayOfWeekAggregation(settings.maxDaysForDayOfWeekAggregation);
		biz.setMaxDaysForHourOfDayAggregation(settings.maxDaysForHourOfDayAggregation);
		return biz;
	}

	@Bean
	@Primary
	public AuditingQueryBiz auditingQueryBiz() {
		return new AuditingQueryBiz(queryBiz(), queryAuditor);
	}

	@Bean
	@Qualifier(READING_DATUM_CRITERIA)
	public Validator readingDatumCriteriaValidator() {
		return new ReadingDatumCriteriaValidator();
	}

	@Bean
	@Qualifier(READING_DATUM_FILTER)
	public SmartValidator readingDatumFilterValidator() {
		return new ReadingDatumFilterValidator();
	}

}
