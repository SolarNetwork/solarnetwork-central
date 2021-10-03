/* ==================================================================
 * DatumImportProgressInputStreamTests.java - 9/11/2018 4:00:16 PM
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

package net.solarnetwork.central.datum.imp.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.support.DatumImportProgressInputStream;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Test cases for the {@link DatumImportProgressInputStream} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumImportProgressInputStreamTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static class TestProgressContext extends
			BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements DatumImportService {

		public TestProgressContext() {
			super(UUID.randomUUID().toString());
		}

		@Override
		public String getDisplayName() {
			return null;
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			return null;
		}

	}

	@Test
	public void reportProgress() throws IOException {
		// given
		int count = 1024;
		ByteBuffer buf = ByteBuffer.allocate(count * 128);
		for ( int i = 0; i < count; i++ ) {
			UUID uuid = UUID.randomUUID();
			buf.putLong(uuid.getMostSignificantBits());
			buf.putLong(uuid.getLeastSignificantBits());
		}
		byte[] data = buf.array();
		DatumImportService ctx = new TestProgressContext();

		List<Double> progress = new ArrayList<>(8);
		DatumImportProgressInputStream in = new DatumImportProgressInputStream(
				new ByteArrayInputStream(data), data.length, ctx,
				new ProgressListener<DatumImportService>() {

					@Override
					public void progressChanged(DatumImportService context, double amountComplete) {
						assertThat("Context is service", context, sameInstance(ctx));
						progress.add(amountComplete);
					}

				});

		// when
		ByteArrayOutputStream out = new ByteArrayOutputStream(count + 1024);
		FileCopyUtils.copy(in, out);
		log.debug("Reported progress: {}", progress);

		// then
		assertThat("Progress was reported", progress, not(hasSize(0)));
		assertThat("Last reported progress 100%", progress.get(progress.size() - 1),
				equalTo((Double) 1.0));
		assertThat("Stream copied", Arrays.equals(data, out.toByteArray()), equalTo(true));
	}

}
