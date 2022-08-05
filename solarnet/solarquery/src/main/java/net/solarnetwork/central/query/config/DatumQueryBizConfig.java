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
import org.springframework.context.annotation.Profile;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.biz.dao.DatumCriteriaValidator;
import net.solarnetwork.central.query.support.AuditingQueryBiz;
import net.solarnetwork.central.query.support.GeneralNodeDatumFilterValidator;
import net.solarnetwork.central.query.support.StreamDatumFilterValidator;

/**
 * Configuration for Datum Query business configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumQueryBizConfig {

	/** A qualifier used for datum criteria components. */
	public static final String DATUM_CRITERIA = "datum-criteria";

	/** A qualifier used for datum filter components. */
	public static final String DATUM_FILTER = "datum-filter";

	/** A qualifier used for stream datum filter components. */
	public static final String STREAM_DATUM_FILTER = "stream-datum-filter";

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private DatumStreamMetadataDao streamMetadataDao;

	@Autowired
	private ReadingDatumDao readingDatumDao;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	public static class DatumQuerySettings {

		private int filteredResultsLimit = 1000;
		private int maxDaysForMinuteAggregation = 7;
		private int maxDaysForHourAggregation = 31;
		private int maxDaysForDayAggregation = 730;
		private int maxDaysForDayOfWeekAggregation = 3650;
		private int maxDaysForHourOfDayAggregation = 3650;
		private int maxDaysForWeekOfYearAggregation = 3650;

		public int getFilteredResultsLimit() {
			return filteredResultsLimit;
		}

		public void setFilteredResultsLimit(int filteredResultsLimit) {
			this.filteredResultsLimit = filteredResultsLimit;
		}

		public int getMaxDaysForMinuteAggregation() {
			return maxDaysForMinuteAggregation;
		}

		public void setMaxDaysForMinuteAggregation(int maxDaysForMinuteAggregation) {
			this.maxDaysForMinuteAggregation = maxDaysForMinuteAggregation;
		}

		public int getMaxDaysForHourAggregation() {
			return maxDaysForHourAggregation;
		}

		public void setMaxDaysForHourAggregation(int maxDaysForHourAggregation) {
			this.maxDaysForHourAggregation = maxDaysForHourAggregation;
		}

		public int getMaxDaysForDayAggregation() {
			return maxDaysForDayAggregation;
		}

		public void setMaxDaysForDayAggregation(int maxDaysForDayAggregation) {
			this.maxDaysForDayAggregation = maxDaysForDayAggregation;
		}

		public int getMaxDaysForDayOfWeekAggregation() {
			return maxDaysForDayOfWeekAggregation;
		}

		public void setMaxDaysForDayOfWeekAggregation(int maxDaysForDayOfWeekAggregation) {
			this.maxDaysForDayOfWeekAggregation = maxDaysForDayOfWeekAggregation;
		}

		public int getMaxDaysForHourOfDayAggregation() {
			return maxDaysForHourOfDayAggregation;
		}

		public void setMaxDaysForHourOfDayAggregation(int maxDaysForHourOfDayAggregation) {
			this.maxDaysForHourOfDayAggregation = maxDaysForHourOfDayAggregation;
		}

		public int getMaxDaysForWeekOfYearAggregation() {
			return maxDaysForWeekOfYearAggregation;
		}

		public void setMaxDaysForWeekOfYearAggregation(int maxDaysForWeekOfYearAggregation) {
			this.maxDaysForWeekOfYearAggregation = maxDaysForWeekOfYearAggregation;
		}

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
		biz.setMaxDaysForWeekOfYearAggregation(settings.maxDaysForWeekOfYearAggregation);
		biz.setCriteriaValidator(datumCriteriaValidator());
		return biz;
	}

	@Bean
	@Profile("query-auditor")
	@Primary
	public AuditingQueryBiz auditingQueryBiz(@Autowired QueryAuditor queryAuditor) {
		return new AuditingQueryBiz(queryBiz(), queryAuditor);
	}

	@Bean
	@Qualifier(DATUM_CRITERIA)
	public Validator datumCriteriaValidator() {
		return new DatumCriteriaValidator();
	}

	@Bean
	@Qualifier(DATUM_FILTER)
	public SmartValidator datumFilterValidator() {
		return new GeneralNodeDatumFilterValidator(datumCriteriaValidator());
	}

	@Bean
	@Qualifier(STREAM_DATUM_FILTER)
	public SmartValidator streamDatumFilterValidator() {
		return new StreamDatumFilterValidator(datumCriteriaValidator());
	}

}
