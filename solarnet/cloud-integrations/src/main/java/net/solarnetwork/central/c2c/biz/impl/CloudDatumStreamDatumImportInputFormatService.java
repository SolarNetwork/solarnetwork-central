/* ==================================================================
 * CloudDatumStreamDatumImportInputFormatService.java - 15/10/2024 6:49:56 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatServiceImportContext;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A {@link DatumImportInputFormatService} implementation that reads data from a
 * {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamDatumImportInputFormatService extends BaseDatumImportInputFormatService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds-import";

	/** The setting for datum stream ID. */
	public static final String DATUM_STREAM_ID_SETTING = "datumStreamId";

	/** The setting for start date (ISO 8601 instant). */
	public static final String START_DATE_SETTING = "startDate";

	/** The setting for end date (ISO 8601 instant). */
	public static final String END_DATE_SETTING = "endDate";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for granularity
		var datumStreamId = new BasicTextFieldSettingSpecifier(DATUM_STREAM_ID_SETTING, null);
		var startDate = new BasicTextFieldSettingSpecifier(START_DATE_SETTING, null);
		var endDate = new BasicTextFieldSettingSpecifier(END_DATE_SETTING, null);

		SETTINGS = List.of(datumStreamId, startDate, endDate);
	}

	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final Function<String, CloudDatumStreamService> datumStreamServiceProvider;

	/**
	 * Constructor.
	 *
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamDatumImportInputFormatService(CloudDatumStreamConfigurationDao datumStreamDao,
			Function<String, CloudDatumStreamService> datumStreamServiceProvider) {
		super(SERVICE_IDENTIFIER);
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamServiceProvider = requireNonNullArgument(datumStreamServiceProvider,
				"datumStreamServiceProvider");
	}

	@Override
	public ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
			ProgressListener<DatumImportService> progressListener) throws IOException {
		requireNonNullArgument(config, "config");
		return new DatumStreamImportContext(config, resource, progressListener);
	}

	@Override
	public String getDisplayName() {
		return "Cloud Integrations Datum Stream";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SETTINGS;
	}

	private class DatumStreamImportContext extends BaseDatumImportInputFormatServiceImportContext {

		private final Instant startDate;
		private final Instant endDate;

		private final CloudDatumStreamService service;
		private final CloudDatumStreamConfiguration datumStream;

		/**
		 * Constructor.
		 *
		 * @param config
		 *        the input configuration
		 * @param resource
		 *        the resource (unused)
		 * @param progressListener
		 *        the progress listener
		 * @throws IllegalArgumentException
		 *         if {@code config} is {@literal null} or the provided
		 *         configuration is not valid
		 */
		private DatumStreamImportContext(InputConfiguration config, DatumImportResource resource,
				ProgressListener<DatumImportService> progressListener) {
			super(config, resource, progressListener);

			Long datumStreamId = requireNonNullArgument(
					config.serviceProperty(DATUM_STREAM_ID_SETTING, Long.class),
					DATUM_STREAM_ID_SETTING);

			try {
				this.startDate = Instant.parse(requireNonNullArgument(
						config.serviceProperty(START_DATE_SETTING, String.class), START_DATE_SETTING));
			} catch ( DateTimeException e ) {
				throw new IllegalArgumentException("Invalid start date: " + e.getMessage());
			}

			try {
				this.endDate = Instant.parse(requireNonNullArgument(
						config.serviceProperty(END_DATE_SETTING, String.class), END_DATE_SETTING));
			} catch ( DateTimeException e ) {
				throw new IllegalArgumentException("Invalid end date: " + e.getMessage());
			}

			datumStream = requireNonNullObject(
					datumStreamDao.get(new UserLongCompositePK(config.getUserId(), datumStreamId)),
					datumStreamId);

			if ( !datumStream.isFullyConfigured() ) {
				throw new IllegalArgumentException("Datum stream is not fully configured.");
			} else if ( datumStream.getKind() != ObjectDatumKind.Node ) {
				throw new IllegalArgumentException(
						"Datum stream kind %s is not supported.".formatted(datumStream.getKind()));
			}

			service = requireNonNullObject(
					datumStreamServiceProvider.apply(datumStream.getServiceIdentifier()),
					datumStream.getServiceIdentifier());
		}

		@Override
		public Iterator<GeneralNodeDatum> iterator() {
			return new DatumStreamImportContextIterator();
		}

		private class DatumStreamImportContextIterator implements Iterator<GeneralNodeDatum> {

			private Iterator<Datum> batchItr;
			private BasicQueryFilter filter;

			private DatumStreamImportContextIterator() {
				super();
				filter = new BasicQueryFilter();
				filter.setStartDate(startDate);
				filter.setEndDate(endDate);
				filter.setParameters(config.getServiceProperties());
				this.batchItr = Collections.emptyIterator();
			}

			@Override
			public boolean hasNext() {
				while ( !batchItr.hasNext() && filter != null ) {
					batchItr = listDatumForBatchRange();
				}
				return batchItr.hasNext();
			}

			@Override
			public GeneralNodeDatum next() {
				var next = batchItr.next();
				return (GeneralNodeDatum) DatumUtils.convertGeneralDatum(next);
			}

			private Iterator<Datum> listDatumForBatchRange() {
				var results = service.datum(datumStream, filter);
				var nextFilter = results.getNextQueryFilter();
				if ( nextFilter != null ) {
					filter = BasicQueryFilter.copyOf(nextFilter, config.getServiceProperties());
				} else {
					filter = null;
				}
				return results.iterator();
			}

		}

	}

}
