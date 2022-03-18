/* ==================================================================
 * DirectoryCleanerJob.java - 18/03/2022 3:36:44 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static java.lang.String.format;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.unit.DataSize;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to clean out expired files from a directory.
 * 
 * @author matt
 * @version 1.0
 */
public class DirectoryCleanerJob extends JobSupport implements PingTest {

	/** The {@code minimumAge} property default value. */
	public static final Duration DEFAULT_MINIMUM_AGE = Duration.ofMinutes(60);

	/** The {@code freeSpaceWarningSize} property default value. */
	public static final DataSize DEFAULT_FREE_SPACE_WARNING_SIZE = DataSize.ofMegabytes(100);

	private final Path directory;
	private Duration minimumAge;
	private DataSize freeSpaceWarningSize;

	private long fileDeleteCount;

	/**
	 * Constructor.
	 * 
	 * @param directory
	 *        the directory to remove files from
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DirectoryCleanerJob(Path directory) {
		super();
		this.directory = ObjectUtils.requireNonNullArgument(directory, "directory");
		this.minimumAge = DEFAULT_MINIMUM_AGE;
		this.freeSpaceWarningSize = DEFAULT_FREE_SPACE_WARNING_SIZE;
		this.fileDeleteCount = 0;
	}

	@Override
	public synchronized void run() {
		final Instant expireDate = Instant.now().minus(minimumAge);
		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					if ( Files.getLastModifiedTime(file).toInstant().isBefore(expireDate)
							&& Files.deleteIfExists(file) ) {
						log.info("Deleted file: [{}]", file);
						fileDeleteCount++;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					log.warn("IO error inspecting file [{}] for deletion: {}", file, exc.toString());
					return FileVisitResult.CONTINUE;
				}

			});
		} catch ( IOException e ) {
			log.error("IO error cleaning files older than {} from [{}]: {}", minimumAge, directory,
					e.toString());
		}
	}

	@Override
	public String getPingTestId() {
		return getClass().getName() + "-" + directory;
	}

	@Override
	public String getPingTestName() {
		return "Directory Cleaner";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 2000;
	}

	@Override
	public Result performPingTest() throws Exception {
		final DataSize size = getFreeSpaceWarningSize();
		Map<String, Object> props = new LinkedHashMap<>(4);
		props.put("FilesDeleted", fileDeleteCount);
		if ( size == null || size.toBytes() < 1 ) {
			return new PingTestResult(true, "Disk space monitoring disabled.", props);

		}
		FileStore fs = Files.getFileStore(directory);
		long freeSpace = fs.getUsableSpace();
		props.put("FreeSpace", format("%d MB", DataSize.ofBytes(freeSpace).toMegabytes()));
		if ( freeSpace < size.toBytes() ) {
			return new PingTestResult(false, String.format("Disk space below %s.", size), props);
		}
		return new PingTestResult(true, "Disk space OK.", props);
	}

	/**
	 * Convenience method to set the minimum age, in minutes.
	 * 
	 * @param minutes
	 *        the minimum age to set, in minutes
	 */
	public void setMinutesOlder(int minutes) {
		setMinimumAge(Duration.ofMinutes(minutes));
	}

	/**
	 * Get the minimum age of files that can be deleted.
	 * 
	 * @return the minimum age
	 */
	public Duration getMinimumAge() {
		return minimumAge;
	}

	/**
	 * Set the minimum age of files that can be deleted.
	 * 
	 * @param minimumAge
	 *        the age to set; if {@literal null} then
	 *        {@link #DEFAULT_MINIMUM_AGE} will be set instead
	 */
	public void setMinimumAge(Duration minimumAge) {
		this.minimumAge = (minimumAge != null ? minimumAge : DEFAULT_MINIMUM_AGE);
	}

	/**
	 * Get the minimum amount of free space on the file system of the configured
	 * directory before {@link #performPingTest()} will fail.
	 * 
	 * @return the free space size; defaults to
	 *         {@link #DEFAULT_FREE_SPACE_WARNING_SIZE}
	 */
	public DataSize getFreeSpaceWarningSize() {
		return freeSpaceWarningSize;
	}

	/**
	 * Set the minimum amount of free space on the file system of the configured
	 * directory before {@link #performPingTest()} will fail.
	 * 
	 * @param freeSpaceWarningSize
	 *        the free space size to set
	 */
	public void setFreeSpaceWarningSize(DataSize freeSpaceWarningSize) {
		this.freeSpaceWarningSize = freeSpaceWarningSize;
	}

}
