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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportUserEvents;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatServiceImportContext;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A {@link DatumImportInputFormatService} implementation that reads data from a
 * {@link CloudDatumStreamService}.
 *
 * <p>
 * The progress updates made by this service are calculated by the start date
 * used in each cloud datum stream query, as a percentage of the the total
 * amount of time between the start/end dates of the overall import
 * configuration.
 * </p>
 *
 * <p>
 * This service will delete generated {@link DatumAuxiliaryEntity} for the time
 * range and node/source IDs given in each datum query issued. Then any records
 * returned by a given query's
 * {@link CloudDatumStreamQueryResult#getAuxiliary()} will be persisted.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
public class CloudDatumStreamDatumImportInputFormatService extends BaseDatumImportInputFormatService
		implements CloudIntegrationsUserEvents, DatumImportUserEvents {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds-import";

	/** The setting for datum stream ID. */
	public static final String DATUM_STREAM_ID_SETTING = "datumStreamId";

	/** The setting for start date (ISO 8601 instant). */
	public static final String START_DATE_SETTING = "startDate";

	/** The setting for end date (ISO 8601 instant). */
	public static final String END_DATE_SETTING = "endDate";

	/** The setting for a retry (after error) count. */
	public static final String RETRY_COUNT_SETTING = "retries";

	/** The default retry count setting value. */
	public static final int DEFAULT_RETRY_COUNT = 2;

	/** The maximum allowed retry count. */
	public static final int MAXIMUM_RETRY_COUNT = 5;

	private static final long RETRY_SLEEP_MS = 5_000L;

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		// menu for granularity
		var datumStreamId = new BasicTextFieldSettingSpecifier(DATUM_STREAM_ID_SETTING, null);
		var startDate = new BasicTextFieldSettingSpecifier(START_DATE_SETTING, null);
		var endDate = new BasicTextFieldSettingSpecifier(END_DATE_SETTING, null);
		var retries = new BasicTextFieldSettingSpecifier(RETRY_COUNT_SETTING,
				String.valueOf(DEFAULT_RETRY_COUNT));

		SETTINGS = List.of(datumStreamId, startDate, endDate, retries);
	}

	private final UserEventAppenderBiz userEventAppenderBiz;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final DatumAuxiliaryEntityDao datumAuxiliaryDao;
	private final DatumStreamMetadataDao datumStreamMetadataDao;
	private final Function<String, @Nullable CloudDatumStreamService> datumStreamServiceProvider;

	/**
	 * Constructor.
	 *
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumAuxiliaryDao
	 *        the datum auxiliary DAO
	 * @param datumStreamMetadataDao
	 *        the datum stream metadata DAO
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudDatumStreamDatumImportInputFormatService(UserEventAppenderBiz userEventAppenderBiz,
			CloudDatumStreamConfigurationDao datumStreamDao, DatumAuxiliaryEntityDao datumAuxiliaryDao,
			DatumStreamMetadataDao datumStreamMetadataDao,
			Function<String, @Nullable CloudDatumStreamService> datumStreamServiceProvider) {
		super(SERVICE_IDENTIFIER);
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumAuxiliaryDao = requireNonNullArgument(datumAuxiliaryDao, "datumAuxiliaryDao");
		this.datumStreamMetadataDao = requireNonNullArgument(datumStreamMetadataDao,
				"datumStreamMetadataDao");
		this.datumStreamServiceProvider = requireNonNullArgument(datumStreamServiceProvider,
				"datumStreamServiceProvider");
	}

	@Override
	public ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
			@Nullable ProgressListener<DatumImportService> progressListener) throws IOException {
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
		private final int retryCount;

		private final CloudDatumStreamService service;
		private final CloudDatumStreamConfiguration datumStream;
		private final BasicDatumCriteria auxFilter = new BasicDatumCriteria();
		private @Nullable Map<String, UUID> sourceToStreamIds;

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
		 *         if {@code config} is {@code null} or the provided
		 *         configuration is not valid
		 */
		private DatumStreamImportContext(InputConfiguration config, DatumImportResource resource,
				@Nullable ProgressListener<DatumImportService> progressListener) {
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

			var retryVal = config.serviceProperty(RETRY_COUNT_SETTING, Integer.class);
			this.retryCount = (retryVal != null ? Math.min(retryVal, MAXIMUM_RETRY_COUNT)
					: DEFAULT_RETRY_COUNT);

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

			// set estimated count to seconds between start/end dates
			setEstimatedResultCount(ChronoUnit.SECONDS.between(startDate, endDate));

			// for node streams, support auxiliary records
			if ( datumStream.getKind() == ObjectDatumKind.Node ) {
				auxFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Mark);
				auxFilter.setSearchFilter(CloudDatumStreamService.GENERATED_AUXILIARY_SEARCH_FILTER);
				auxFilter.setObjectKind(datumStream.getKind());
				auxFilter.setNodeId(datumStream.getObjectId());

				Set<String> sourceIds = service.datumStreamSourceIds(datumStream);
				if ( sourceIds != null ) {
					auxFilter.setSourceIds(sourceIds.toArray(String[]::new));
				}
			}
		}

		@Override
		public Iterator<GeneralNodeDatum> iterator() {
			return new DatumStreamImportContextIterator();
		}

		private Map<String, UUID> sourceToStreamIds() {
			if ( sourceToStreamIds != null ) {
				return sourceToStreamIds;
			}

			// copy aux filter for node/source IDs but clear out other criteria
			var filter = new BasicDatumCriteria();
			filter.setObjectKind(auxFilter.getObjectKind());
			filter.setNodeId(auxFilter.getNodeId());
			filter.setSourceIds(auxFilter.getSourceIds());

			sourceToStreamIds = stream(
					datumStreamMetadataDao.findDatumStreamMetadataIds(filter).spliterator(), false)
							.collect(toMap(ObjectDatumStreamMetadataId::getSourceId,
									ObjectDatumStreamMetadataId::getStreamId, (_, r) -> r));
			return sourceToStreamIds;
		}

		private class DatumStreamImportContextIterator implements Iterator<GeneralNodeDatum> {

			private Iterator<Datum> batchItr;
			private @Nullable BasicQueryFilter filter;

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
				boolean result = batchItr.hasNext();
				if ( !result && getCompleteCount() < getEstimatedResultCount() ) {
					updateProgress(CloudDatumStreamDatumImportInputFormatService.this,
							getEstimatedResultCount(), progressListener);
				}
				return result;
			}

			@Override
			public GeneralNodeDatum next() {
				var next = batchItr.next();
				var result = (GeneralNodeDatum) DatumUtils.convertGeneralDatum(next);
				if ( result == null ) {
					throw new NoSuchElementException();
				}
				return result;
			}

			private Iterator<Datum> listDatumForBatchRange() {
				final var f = nonnull(filter, "Filter");
				CloudDatumStreamQueryResult results = null;
				int attempt = 0;
				while ( true ) {
					try {
						results = service.datum(datumStream, f);
						break;
					} catch ( RemoteServiceException e ) {
						attempt++;
						if ( attempt > retryCount ) {
							throw e;
						} else {
							log.warn(
									"Error importing datum from Cloud Datum Stream {} for date range {} - {}: {}; will try up to {} more times",
									datumStream.ident(), f.getStartDate(), f.getEndDate(),
									e.getMessage(), (retryCount - attempt + 1));
						}
						try {
							Thread.sleep(RETRY_SLEEP_MS);
						} catch ( InterruptedException ie ) {
							// continue
						}
					}
				}

				// clear out any generated auxiliary records over the query time range
				final var usedFilter = (results != null && results.getUsedQueryFilter() != null
						? results.getUsedQueryFilter()
						: f);

				if ( auxFilter.hasSourceCriteria() ) {
					auxFilter.setStartDate(usedFilter.getStartDate());
					auxFilter.setEndDate(usedFilter.getEndDate());
					long deleteCount = datumAuxiliaryDao.deleteFiltered(auxFilter);
					if ( deleteCount > 0 ) {
						userEventAppenderBiz.addEvent(datumStream.getUserId(),
								eventForUserRelatedKey(datumStream.getId(), DATUM_IMPORT_TAGS,
										"Deleted %d generated Mark datum auxiliary records."
												.formatted(deleteCount),
										// @formatter:off
										Map.of(START_AT_DATA_KEY, auxFilter.getStartDate()
											, END_AT_DATA_KEY, auxFilter.getEndDate()
											, NODE_ID_DATA_KEY, auxFilter.getNodeId()
											, SOURCE_ID_DATA_KEY, auxFilter.getSourceIds()
										// @formatter:on
										)));
					}
				}

				if ( results == null ) {
					return Collections.emptyIterator();
				}

				final var nextFilter = results.getNextQueryFilter();
				if ( nextFilter != null && nonnull(nextFilter.getStartDate(), "Next start date")
						.isAfter(f.getStartDate()) ) {
					// update progress based on the seconds between the overall start date and the next start date
					updateProgress(CloudDatumStreamDatumImportInputFormatService.this,
							ChronoUnit.SECONDS.between(startDate, nextFilter.getStartDate()),
							progressListener);
					filter = BasicQueryFilter.copyOf(nextFilter, config.getServiceProperties());
					filter.setEndDate(endDate); // keep importing to end date
				} else {
					filter = null;
				}

				// save any auxiliary records returned
				final SequencedCollection<DatumAuxiliaryRecord> auxiliary = results.getAuxiliary();
				if ( auxiliary != null ) {
					final Map<String, UUID> sourceToStreamIds = sourceToStreamIds();
					for ( DatumAuxiliaryRecord aux : auxiliary ) {
						final UUID streamId = sourceToStreamIds.get(aux.getSourceId());
						if ( streamId != null ) {
							final var entity = new DatumAuxiliaryEntity(streamId, aux.getTimestamp(),
									aux.getType(), Instant.now(), aux.getSamplesFinal(),
									aux.getSamplesStart(), aux.getNotes(), aux.getMetadata());
							datumAuxiliaryDao.save(entity);
						}
					}
				}

				return results.iterator();
			}

		}

	}

}
