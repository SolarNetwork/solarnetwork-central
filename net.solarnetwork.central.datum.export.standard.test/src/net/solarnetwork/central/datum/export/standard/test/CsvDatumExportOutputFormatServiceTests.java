/* ==================================================================
 * CsvDatumExportOutputFormatServiceTests.java - 23/04/2018 10:40:47 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.standard.test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService.ExportContext;
import net.solarnetwork.central.datum.export.domain.BasicOutputConfiguration;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.util.ProgressListener;

/**
 * Test cases for the {@link CsvDatumExportOutputFormatService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDatumExportOutputFormatServiceTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	@Test
	public void export() throws IOException {
		// given
		CsvDatumExportOutputFormatService service = new CsvDatumExportOutputFormatService();
		BasicOutputConfiguration config = new BasicOutputConfiguration();
		config.setCompressionType(OutputCompressionType.None);

		GeneralNodeDatumMatch d1 = new GeneralNodeDatumMatch();
		d1.setCreated(new DateTime(2018, 4, 23, 11, 19, DateTimeZone.UTC));
		d1.setNodeId(TEST_NODE_ID);
		d1.setSourceId(TEST_SOURCE_ID);
		d1.setLocalDateTime(d1.getCreated().toLocalDateTime());
		d1.setPosted(d1.getCreated().plusMinutes(1));
		d1.setSampleJson("{\"i\":{\"watts\":123.456}}");
		List<GeneralNodeDatumFilterMatch> data = Arrays.asList(d1);

		List<Double> progress = new ArrayList<>(4);

		// when
		Iterable<Resource> results = null;
		try (DatumExportOutputFormatService.ExportContext context = service
				.createExportContext(config)) {
			assertThat("Context created", context, notNullValue());

			context.start(1);
			context.appendDatumMatch(data,
					new ProgressListener<DatumExportOutputFormatService.ExportContext>() {

						@Override
						public void progressChanged(ExportContext ctx, double amountComplete) {
							assertThat("Same context", ctx, sameInstance(context));
							progress.add(amountComplete);
						}
					});
			results = context.finish();
		}

		// then
		assertThat("Result created", results, notNullValue());
		assertThat("Progress provided", progress, hasSize(1));

		List<Resource> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(Collectors.toList());
		assertThat(resultList, hasSize(1));

		Resource r = resultList.get(0);
		File tempFile = r.getFile();
		assertThat("Temp file exists", tempFile.exists(), equalTo(true));
		assertThat("Temp file extension", tempFile.getName(), endsWith(".csv"));
		String csv = FileCopyUtils.copyToString(new InputStreamReader(r.getInputStream(), "UTF-8"));
		assertThat("Temp file deleted", tempFile.exists(), equalTo(false));
		assertThat("Generated CSV", csv, equalTo("created,nodeId,sourceId,localDate,localTime,watts\r\n"
				+ "2018-04-23T11:19:00.000Z,-1,test.source,2018-04-23,11:19:00.000,123.456\r\n"));
	}

	@Test
	public void exportCompressedGzip() throws IOException {
		// given
		CsvDatumExportOutputFormatService service = new CsvDatumExportOutputFormatService();
		BasicOutputConfiguration config = new BasicOutputConfiguration();
		config.setCompressionType(OutputCompressionType.GZIP);

		List<GeneralNodeDatumFilterMatch> data = new ArrayList<>(100);
		DateTime start = new DateTime(2018, 4, 23, 11, 19, DateTimeZone.UTC);
		for ( int i = 0; i < 100; i++ ) {
			GeneralNodeDatumMatch d1 = new GeneralNodeDatumMatch();
			d1.setCreated(start.plusMinutes(i));
			d1.setNodeId(TEST_NODE_ID);
			d1.setSourceId(TEST_SOURCE_ID);
			d1.setLocalDateTime(d1.getCreated().toLocalDateTime());
			d1.setSampleJson("{\"i\":{\"watts\":123.456}}");
			data.add(d1);
		}

		List<Double> progress = new ArrayList<>(4);

		// when
		Iterable<Resource> results = null;
		try (DatumExportOutputFormatService.ExportContext context = service
				.createExportContext(config)) {
			assertThat("Context created", context, notNullValue());

			context.start(1);
			context.appendDatumMatch(data,
					new ProgressListener<DatumExportOutputFormatService.ExportContext>() {

						@Override
						public void progressChanged(ExportContext ctx, double amountComplete) {
							assertThat("Same context", ctx, sameInstance(context));
							progress.add(amountComplete);
						}
					});
			results = context.finish();
		}

		// then
		assertThat("Result created", results, notNullValue());

		List<Resource> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(Collectors.toList());
		assertThat(resultList, hasSize(1));
		assertThat("Progress provided", progress, hasSize(100));

		Resource r = resultList.get(0);
		File tempFile = r.getFile();
		assertThat("Temp file exists", tempFile.exists(), equalTo(true));
		assertThat("Temp file extension", tempFile.getName(), endsWith(".csv.gz"));
		String csv = FileCopyUtils
				.copyToString(new InputStreamReader(new GZIPInputStream(r.getInputStream()), "UTF-8"));
		assertThat("Temp file deleted", tempFile.exists(), equalTo(false));

		StringBuilder buf = new StringBuilder("created,nodeId,sourceId,localDate,localTime,watts\r\n");
		DateTimeFormatter tsFormatter = new DateTimeFormatterBuilder()
				.appendPattern("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'").toFormatter();
		DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd")
				.toFormatter();
		DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().appendPattern("hh:mm:ss.SSS")
				.toFormatter();
		for ( int i = 0; i < 100; i++ ) {
			DateTime date = start.plusMinutes(i);
			buf.append(String.format("%s,-1,test.source,%s,%s,123.456\r\n", tsFormatter.print(date),
					dateFormatter.print(date), timeFormatter.print(date)));
		}

		assertThat("Generated CSV", csv, equalTo(buf.toString()));
	}

}
