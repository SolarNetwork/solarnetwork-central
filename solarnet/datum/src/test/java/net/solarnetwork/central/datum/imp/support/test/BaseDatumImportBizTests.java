/* ==================================================================
 * BaseDatumImportBizTests.java - 10/11/2018 11:54:04 AM
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.domain.Configuration;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportBiz;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.io.TransferrableResource;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link BaseDatumImportBiz} class.
 *
 * @author matt
 * @version 2.1
 */
public class BaseDatumImportBizTests {

	private static final Long TEST_USER_ID = randomLong();

	private class TestDatumImportBiz extends BaseDatumImportBiz {

		@Override
		public <T extends Identity<String>> T optionalService(List<T> collection,
				IdentifiableConfiguration config) {
			return super.optionalService(collection, config);
		}

		@Override
		public File getImportDataFile(UserUuidPK id) {
			return super.getImportDataFile(id);
		}

		@Override
		public File saveToWorkDirectory(DatumImportResource resource, UserUuidPK id) throws IOException {
			return super.saveToWorkDirectory(resource, id);
		}

		@Override
		public Future<FilterResults<GeneralNodeDatumComponents, GeneralNodeDatumPK>> previewStagedImportRequest(
				DatumImportPreviewRequest request) {
			return null;
		}

		@Override
		public DatumImportStatus datumImportJobStatusForUser(Long userId, String jobId) {
			return null;
		}

		@Override
		public Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
				Set<DatumImportState> states) {
			return null;
		}

		@Override
		public DatumImportStatus updateDatumImportJobConfigurationForUser(Long userId, String jobId,
				Configuration configuration) {
			return null;
		}

		@Override
		public DatumImportStatus updateDatumImportJobStateForUser(Long userId, String jobId,
				DatumImportState desiredState, Set<DatumImportState> expectedStates) {
			return null;
		}

		@Override
		public DatumImportStatus submitDatumImportRequest(DatumImportRequest request,
				DatumImportResource resource) {
			return null;
		}

		@Override
		public Collection<DatumImportStatus> deleteDatumImportJobsForUser(Long userId,
				Set<String> jobIds) {
			return null;
		}

	}

	@Test
	public void defaultWorkDirectory() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		File d = biz.getWorkDirectory();
		assertThat("Work directory available", d, notNullValue());
	}

	@Test
	public void setWorkPath() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		biz.setWorkPath("/var/tmp/foo");
		File d = biz.getWorkDirectory();
		assertThat("Work directory configured from path", d, equalTo(new File("/var/tmp/foo")));
	}

	@Test
	public void availableInputServices() {
		List<DatumImportInputFormatService> services = new ArrayList<>();
		TestDatumImportBiz biz = new TestDatumImportBiz();
		biz.setInputServices(services);
		assertThat("Services returned", biz.availableInputFormatServices(), sameInstance(services));
	}

	@Test
	public void dataFile() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		biz.setWorkPath("/tmp");
		UserUuidPK pk = new UserUuidPK(1L, UUID.randomUUID());
		File f = biz.getImportDataFile(pk);
		assertThat("Work file", f, equalTo(new File("/tmp/" + pk.getUserId() + "-" + pk.getId())));
	}

	@Test
	public void copyInputResource() throws IOException {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		byte[] data = "Hello,world".getBytes(UTF_8);
		ByteArrayResource r = new ByteArrayResource(data);
		BasicDatumImportResource resource = new BasicDatumImportResource(r, "text/csv");
		UserUuidPK pk = new UserUuidPK(1L, UUID.randomUUID());
		File f = biz.saveToWorkDirectory(resource, pk);
		assertThat("Saved file exists", f.exists(), equalTo(true));
		byte[] contents = FileCopyUtils.copyToByteArray(f);
		assertThat("Saved file contents", Arrays.equals(data, contents), equalTo(true));
		f.delete();
	}

	private static class TransferrableFileSystemResource extends FileSystemResource
			implements TransferrableResource {

		public TransferrableFileSystemResource(File file) {
			super(file);
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			getFile().renameTo(dest);
		}

	}

	@Test
	public void moveTransferrableInputResource() throws IOException {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		byte[] data = "Hello,world".getBytes(UTF_8);
		File srcFile = File.createTempFile("data-", ".csv", biz.getWorkDirectory());
		FileCopyUtils.copy(data, srcFile);

		TransferrableFileSystemResource r = new TransferrableFileSystemResource(srcFile);
		BasicDatumImportResource resource = new BasicDatumImportResource(r, "text/csv");
		UserUuidPK pk = new UserUuidPK(1L, UUID.randomUUID());

		File f = biz.saveToWorkDirectory(resource, pk);

		assertThat("Saved file exists", f.exists(), equalTo(true));
		byte[] contents = FileCopyUtils.copyToByteArray(f);
		assertThat("Saved file contents", Arrays.equals(data, contents), equalTo(true));
		f.delete();
		assertThat("Source file moved", srcFile.exists(), equalTo(false));
	}

	private static class TestInputFormatService extends BaseDatumImportInputFormatService {

		public TestInputFormatService() {
			super(TestInputFormatService.class.getName());
		}

		@Override
		public ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
				ProgressListener<DatumImportService> progressListener) throws IOException {
			return null;
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
	public void inputServiceForIdNoList() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		BasicInputConfiguration conf = new BasicInputConfiguration(TEST_USER_ID);
		conf.setServiceIdentifier("foo");
		assertThat("No service list", biz.optionalService(biz.getInputServices(), conf), nullValue());
	}

	@Test
	public void inputServiceForIdNullId() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		DatumImportInputFormatService service = new TestInputFormatService();
		biz.setInputServices(singletonList(service));
		assertThat("No ID", biz.optionalService(biz.getInputServices(), null), nullValue());
	}

	@Test
	public void inputServiceForIdNoMatch() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		DatumImportInputFormatService service = new TestInputFormatService();
		biz.setInputServices(singletonList(service));
		BasicInputConfiguration conf = new BasicInputConfiguration(TEST_USER_ID);
		conf.setServiceIdentifier("foo");
		assertThat("No match", biz.optionalService(biz.getInputServices(), conf), nullValue());
	}

	@Test
	public void inputServiceForId() {
		TestDatumImportBiz biz = new TestDatumImportBiz();
		DatumImportInputFormatService service = new TestInputFormatService();
		biz.setInputServices(singletonList(service));
		BasicInputConfiguration conf = new BasicInputConfiguration(TEST_USER_ID);
		conf.setServiceIdentifier(service.getId());
		assertThat("Match", biz.optionalService(biz.getInputServices(), conf), sameInstance(service));
	}
}
