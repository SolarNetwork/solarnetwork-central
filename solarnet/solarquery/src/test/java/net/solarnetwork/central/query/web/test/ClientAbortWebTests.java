/* ==================================================================
 * ClientAbortWebTests.java - 17/09/2026 7:30:00 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.web.test;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static net.solarnetwork.central.query.config.ContentCachingServiceConfig.QUERY_CACHE;
import static net.solarnetwork.central.web.WebUtils.throwUnlessCommitted;
import static net.solarnetwork.domain.Result.success;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.catalina.AccessLog;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.ActionCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.query.config.JsonConfig;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.RateLimitExceededException;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.central.web.support.ContentCachingFilter;
import net.solarnetwork.central.web.support.ContentCachingService;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.central.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.RemoteServiceException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test cases for handling clients that disconnect before a response has been
 * written, which surfaces in the server as a {@link ClientAbortException}.
 *
 * <p>
 * These tests run the complete SolarQuery web stack in the embedded Tomcat
 * container, including the servlet-layer error handling, with test handlers
 * that wait for the test client before writing their response. A raw socket
 * client sends each request twice: first reading the complete response, then
 * aborting the connection (with a TCP reset) once the request has reached the
 * handler, so the handler's response write fails like it does when a real
 * client goes away.
 * </p>
 *
 * <p>
 * An aborted request must not log anything above {@code DEBUG} other than what
 * the completed request also logged, such as an exception handler logging the
 * exception thrown by the handler. That covers exception handler logging, a
 * failure writing an exception handler response body, filter logging, and
 * errors logged by the servlet container when an exception escapes the
 * application. Neither request may log a servlet container or Spring warning,
 * such as when an exception handler fails because the handler already closed
 * the response.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
// use a separate persistent cache directory, as other test contexts lock the default one;
// enable the query cache, as in production
// @formatter:off
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"app.cache.persistence.path=build/tmp/ClientAbortWebTests/cache",
		"app.query-cache.enabled=true",
})
// @formatter:on
public class ClientAbortWebTests {

	/**
	 * A query parameter to correlate test requests with their processing, which
	 * also gives each request a unique query cache key.
	 */
	private static final String REQUEST_ID_PARAM = "testRequestId";

	/** The base path for the test handlers. */
	private static final String TEST_PATH = "/api/v1/pub/test/client-abort";

	/** The maximum time to wait for request processing steps. */
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	/**
	 * The time to allow the server to receive the TCP reset of an aborted
	 * connection, before its handler writes the response.
	 */
	private static final Duration CLIENT_ABORT_DELAY = Duration.ofMillis(100);

	/** The number of items in response bodies, to overflow response buffers. */
	private static final int ITEM_COUNT = 2_000;

	/** The chunk size for writing directly to the response output stream. */
	private static final int CHUNK_SIZE = 1024;

	/**
	 * The number of items streamed before a query timeout, small enough to fit
	 * in response buffers.
	 */
	private static final int QUERY_TIMEOUT_ITEM_COUNT = 10;

	@Value("${local.server.port}")
	private int port;

	@Value("${server.servlet.context-path:}")
	private String contextPath;

	@Autowired
	private RequestTracker tracker;

	/**
	 * The kinds of controller to test, which determines the controller advice
	 * and filters that apply.
	 */
	enum ControllerKind {

		/**
		 * A controller annotated with {@link GlobalExceptionRestController},
		 * like the SolarQuery API controllers, so both
		 * {@link WebServiceControllerSupport} and
		 * {@link WebServiceGlobalControllerSupport} apply.
		 */
		App("/app"),

		/**
		 * Like {@link #App}, with the query cache {@link ContentCachingFilter}
		 * applied like it is to SolarQuery datum API paths.
		 */
		AppQueryCache("/app-cache"),

		/**
		 * A plain REST controller, so only
		 * {@link WebServiceGlobalControllerSupport} applies.
		 */
		Rest("/rest"),

		;

		private final String path;

		ControllerKind(String path) {
			this.path = path;
		}

	}

	/**
	 * An exception for a test handler to throw.
	 *
	 * @param name
	 *        the unique name of the case
	 * @param status
	 *        the expected HTTP response status, when the client does not abort
	 * @param exception
	 *        provides the exception to throw
	 */
	record ExceptionCase(String name, int status, Supplier<Throwable> exception) {

		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * Exceptions for each {@link WebServiceGlobalControllerSupport} exception
	 * handler method that renders a response body.
	 */
	// @formatter:off
	private static final List<ExceptionCase> EXCEPTION_CASES = List.of(
		new ExceptionCase("MaxUploadSizeExceeded", 422,
			() -> new MaxUploadSizeExceededException(1024L)),
		new ExceptionCase("DataAccessResourceFailure", 429,
			() -> new DataAccessResourceFailureException("Test")),
		new ExceptionCase("TransientDataAccess", 429,
			() -> new QueryTimeoutException("Test")),
		new ExceptionCase("UncategorizedSQL", 429,
			() -> new UncategorizedSQLException("Test", "SELECT 1", new SQLException("Test"))),
		new ExceptionCase("Transaction", 429,
			() -> new CannotCreateTransactionException("Test")),
		new ExceptionCase("Authorization", 403,
			() -> new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, 1L)),
		new ExceptionCase("BasicSecurity", 403,
			() -> new BasicSecurityException("Test")),
		new ExceptionCase("BadCredentials", 403,
			() -> new BadCredentialsException("Test")),
		new ExceptionCase("Authentication", 401,
			() -> new InsufficientAuthenticationException("Test")),
		new ExceptionCase("AccessDenied", 403,
			() -> new AccessDeniedException("Test")),
		new ExceptionCase("RequestRejected", 400,
			() -> new RequestRejectedException("Test")),
		new ExceptionCase("Execution", 500,
			() -> new ExecutionException(new RuntimeException("Test"))),
		new ExceptionCase("IllegalArgument", 422,
			() -> new IllegalArgumentException("Test")),
		new ExceptionCase("Runtime", 500,
			() -> new RuntimeException("Test")),
		new ExceptionCase("Error", 500,
			() -> new Error("Test")),
		new ExceptionCase("HttpMessageConversion", 500,
			() -> new HttpMessageConversionException("Test")),
		new ExceptionCase("IO", 422,
			() -> new IOException("Test")),
		new ExceptionCase("RemoteService", 422,
			() -> new RemoteServiceException("Test")),
		new ExceptionCase("RateLimitExceeded", 429,
			() -> new RateLimitExceededException("Test", 1L))
	);
	// @formatter:on

	/** The media types supported for SolarQuery response bodies. */
	private static final List<String> RESPONSE_BODY_MEDIA_TYPES = List.of(
			MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_CBOR_VALUE, "text/csv",
			MediaType.APPLICATION_XML_VALUE);

	/** The paths of the handlers that stream their response. */
	private static final List<String> STREAMING_PATHS = List.of("/stream", "/stream/json",
			"/stream/json/throw-unless-committed", "/stream/json/query-timeout",
			"/stream/json/query-timeout/throw-unless-committed");

	private static ExceptionCase exceptionCase(String name) {
		return EXCEPTION_CASES.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
	}

	private static List<Map<String, Object>> items() {
		return IntStream.range(0, ITEM_COUNT)
				.mapToObj(i -> Map.<String, Object> of("id", i, "name", "Item " + i)).toList();
	}

	/**
	 * The server-side state of a test request.
	 */
	static final class TrackedRequest {

		/** Released when the handler has been invoked. */
		private final CountDownLatch handling = new CountDownLatch(1);

		/** Released when the handler can write its response. */
		private final CountDownLatch clientReady = new CountDownLatch(1);

		/** Released when the container has finished processing the request. */
		private final CountDownLatch completed = new CountDownLatch(1);

		private volatile String threadName;
		private volatile boolean ioAllowed;

	}

	/**
	 * Tracks the server-side processing of test requests, so a test client can
	 * coordinate with the handler of its request.
	 */
	static final class RequestTracker {

		private final Map<String, TrackedRequest> requests = new ConcurrentHashMap<>();

		private TrackedRequest request(String requestId) {
			return requests.computeIfAbsent(requestId, _ -> new TrackedRequest());
		}

		/**
		 * Wait for the test client to either abort its connection or be ready
		 * to read the response.
		 *
		 * @param requestId
		 *        the ID of the request being handled
		 */
		private void awaitClient(String requestId) {
			final TrackedRequest req = request(requestId);
			req.handling.countDown();
			try {
				if ( !req.clientReady.await(TIMEOUT.toMillis(), MILLISECONDS) ) {
					throw new IllegalStateException("Timeout waiting for test client.");
				}
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		}

	}

	/**
	 * Tracks when the container starts and finishes processing test requests.
	 *
	 * <p>
	 * The container calls {@link AccessLog#log(Request, Response, long)} after
	 * all of its error handling and logging for a request has completed.
	 * </p>
	 */
	static final class RequestTrackingValve extends ValveBase implements AccessLog {

		private final RequestTracker tracker;

		private RequestTrackingValve(RequestTracker tracker) {
			super(true);
			this.tracker = tracker;
		}

		@Override
		public void invoke(Request request, Response response) throws IOException, ServletException {
			final String requestId = request.getParameter(REQUEST_ID_PARAM);
			if ( requestId != null ) {
				tracker.request(requestId).threadName = Thread.currentThread().getName();
			}
			getNext().invoke(request, response);
		}

		@Override
		public void log(Request request, Response response, long time) {
			final String requestId = request.getParameter(REQUEST_ID_PARAM);
			if ( requestId == null ) {
				return;
			}
			// I/O is no longer allowed once the container has failed writing to the client
			final AtomicBoolean ioAllowed = new AtomicBoolean(true);
			response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, ioAllowed);
			final TrackedRequest req = tracker.request(requestId);
			req.ioAllowed = ioAllowed.get();
			req.completed.countDown();
		}

		@Override
		public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
			// not supported
		}

		@Override
		public boolean getRequestAttributesEnabled() {
			return false;
		}

	}

	/**
	 * Captures log events from all loggers, while open.
	 */
	static final class LogCapture extends AppenderBase<ILoggingEvent> implements AutoCloseable {

		private final Logger rootLogger;
		private final Queue<ILoggingEvent> events = new ConcurrentLinkedQueue<>();

		private LogCapture() {
			super();
			rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
			setContext(rootLogger.getLoggerContext());
			setName(ClientAbortWebTests.class.getSimpleName());
			start();
			rootLogger.addAppender(this);
		}

		@Override
		protected void append(ILoggingEvent event) {
			// resolve the thread name and message now, on the logging thread
			event.prepareForDeferredProcessing();
			events.add(event);
		}

		private List<ILoggingEvent> events(String threadName) {
			return events.stream().filter(e -> threadName.equals(e.getThreadName())).toList();
		}

		@Override
		public void close() {
			rootLogger.detachAppender(this);
			stop();
		}

	}

	/**
	 * Test handlers that wait for the test client before writing a response.
	 */
	abstract static class ClientAbortTestController {

		private final RequestTracker tracker;
		private final JsonMapper jsonMapper;

		private ClientAbortTestController(RequestTracker tracker, JsonMapper jsonMapper) {
			super();
			this.tracker = tracker;
			this.jsonMapper = jsonMapper;
		}

		/**
		 * Return a response body, written by a message converter.
		 */
		@GetMapping("/result")
		public Result<List<Map<String, Object>>> result(
				@RequestParam(REQUEST_ID_PARAM) String requestId) {
			tracker.awaitClient(requestId);
			return success(items());
		}

		/**
		 * Write directly to the response output stream.
		 */
		@GetMapping("/stream")
		public void outputStream(@RequestParam(REQUEST_ID_PARAM) String requestId,
				HttpServletResponse response) throws IOException {
			tracker.awaitClient(requestId);
			final byte[] data = jsonMapper.writeValueAsBytes(items());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			try (OutputStream out = response.getOutputStream()) {
				for ( int offset = 0; offset < data.length; offset += CHUNK_SIZE ) {
					out.write(data, offset, Math.min(CHUNK_SIZE, data.length - offset));
				}
			}
		}

		/**
		 * Stream JSON to the response output stream with a generator, letting
		 * any exception propagate, like {@code NodeInstructionController} does.
		 */
		@GetMapping("/stream/json")
		public void jsonGenerator(@RequestParam(REQUEST_ID_PARAM) String requestId,
				HttpServletResponse response) throws IOException {
			tracker.awaitClient(requestId);
			writeJsonItems(response, -1);
		}

		/**
		 * Stream JSON to the response output stream with a generator, passing
		 * exceptions to
		 * {@link WebUtils#throwUnlessCommitted(RuntimeException, WebRequest, HttpServletResponse)}
		 * like {@code DatumStreamController} does.
		 */
		@GetMapping("/stream/json/throw-unless-committed")
		public void jsonGeneratorThrowUnlessCommitted(@RequestParam(REQUEST_ID_PARAM) String requestId,
				WebRequest request, HttpServletResponse response) throws IOException {
			tracker.awaitClient(requestId);
			try {
				writeJsonItems(response, -1);
			} catch ( RuntimeException e ) {
				throwUnlessCommitted(e, request, response);
			}
		}

		/**
		 * Stream some JSON to the response output stream with a generator, then
		 * fail like a query timing out while streaming results, letting the
		 * exception propagate.
		 */
		@GetMapping("/stream/json/query-timeout")
		public void jsonGeneratorQueryTimeout(@RequestParam(REQUEST_ID_PARAM) String requestId,
				HttpServletResponse response) throws IOException {
			tracker.awaitClient(requestId);
			writeJsonItems(response, QUERY_TIMEOUT_ITEM_COUNT);
		}

		/**
		 * Stream some JSON to the response output stream with a generator, then
		 * fail like a query timing out while streaming results, passing the
		 * exception to
		 * {@link WebUtils#throwUnlessCommitted(RuntimeException, WebRequest, HttpServletResponse)}.
		 */
		@GetMapping("/stream/json/query-timeout/throw-unless-committed")
		public void jsonGeneratorQueryTimeoutThrowUnlessCommitted(
				@RequestParam(REQUEST_ID_PARAM) String requestId, WebRequest request,
				HttpServletResponse response) throws IOException {
			tracker.awaitClient(requestId);
			try {
				writeJsonItems(response, QUERY_TIMEOUT_ITEM_COUNT);
			} catch ( RuntimeException e ) {
				throwUnlessCommitted(e, request, response);
			}
		}

		/**
		 * Stream JSON items to the response output stream with a generator,
		 * which closes the output stream when done.
		 *
		 * @param response
		 *        the response
		 * @param queryTimeoutItemCount
		 *        if not negative, the number of items to write before throwing
		 *        a {@link QueryTimeoutException}
		 * @throws IOException
		 *         if an IO error occurs
		 */
		private void writeJsonItems(HttpServletResponse response, int queryTimeoutItemCount)
				throws IOException {
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			try (JsonGenerator generator = jsonMapper.createGenerator(response.getOutputStream())) {
				generator.writeStartArray();
				int count = 0;
				for ( Map<String, Object> item : items() ) {
					if ( count++ == queryTimeoutItemCount ) {
						throw new QueryTimeoutException("Test");
					}
					generator.writePOJO(item);
				}
				generator.writeEndArray();
			}
		}

		/**
		 * Throw an exception, for an exception handler to render.
		 */
		@GetMapping("/throw/{name}")
		public Result<Void> throwException(@RequestParam(REQUEST_ID_PARAM) String requestId,
				@PathVariable("name") String name) throws Exception {
			tracker.awaitClient(requestId);
			final Throwable t = exceptionCase(name).exception().get();
			if ( t instanceof Exception e ) {
				throw e;
			}
			throw (Error) t;
		}

	}

	@GlobalExceptionRestController
	@RequestMapping(TEST_PATH + "/app")
	static class AppTestController extends ClientAbortTestController {

		private AppTestController(RequestTracker tracker, JsonMapper jsonMapper) {
			super(tracker, jsonMapper);
		}

	}

	@GlobalExceptionRestController
	@RequestMapping(TEST_PATH + "/app-cache")
	static class AppQueryCacheTestController extends ClientAbortTestController {

		private AppQueryCacheTestController(RequestTracker tracker, JsonMapper jsonMapper) {
			super(tracker, jsonMapper);
		}

	}

	@RestController
	@RequestMapping(TEST_PATH + "/rest")
	static class RestTestController extends ClientAbortTestController {

		private RestTestController(RequestTracker tracker, JsonMapper jsonMapper) {
			super(tracker, jsonMapper);
		}

	}

	@TestConfiguration
	static class ClientAbortTestConfig {

		@Bean
		RequestTracker clientAbortRequestTracker() {
			return new RequestTracker();
		}

		@Bean
		WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> clientAbortRequestTrackingValve(
				RequestTracker tracker) {
			return factory -> factory.addEngineValves(new RequestTrackingValve(tracker));
		}

		@Bean
		AppTestController clientAbortAppTestController(RequestTracker tracker,
				@Qualifier(JsonConfig.JSON_STREAMING_MAPPER) JsonMapper jsonMapper) {
			return new AppTestController(tracker, jsonMapper);
		}

		@Bean
		AppQueryCacheTestController clientAbortAppQueryCacheTestController(RequestTracker tracker,
				@Qualifier(JsonConfig.JSON_STREAMING_MAPPER) JsonMapper jsonMapper) {
			return new AppQueryCacheTestController(tracker, jsonMapper);
		}

		@Bean
		RestTestController clientAbortRestTestController(RequestTracker tracker,
				@Qualifier(JsonConfig.JSON_STREAMING_MAPPER) JsonMapper jsonMapper) {
			return new RestTestController(tracker, jsonMapper);
		}

		@Bean
		FilterRegistrationBean<ContentCachingFilter> clientAbortContentCachingFilterRegistration(
				@Qualifier(QUERY_CACHE) ContentCachingService contentCachingService) {
			final var reg = new FilterRegistrationBean<>(
					new ContentCachingFilter(contentCachingService, 1));
			reg.setOrder(0);
			reg.addUrlPatterns(TEST_PATH + ControllerKind.AppQueryCache.path + "/*");
			return reg;
		}

	}

	/**
	 * The outcome of a test request.
	 *
	 * @param response
	 *        the HTTP response received by the client, or {@code null} if the
	 *        client aborted
	 * @param ioAllowed
	 *        {@code false} if the container failed writing to the client
	 * @param logEvents
	 *        the log events generated while the container processed the request
	 */
	record Exchange(String response, boolean ioAllowed, List<ILoggingEvent> logEvents) {

		private int status() {
			then(response).as("HTTP response received").startsWith("HTTP/1.1 ");
			return Integer.parseInt(response.substring(9, 12));
		}

	}

	/**
	 * Send a GET request to a test handler.
	 *
	 * @param kind
	 *        the kind of controller to send the request to
	 * @param path
	 *        the handler path
	 * @param accept
	 *        the acceptable response media type
	 * @param clientAbort
	 *        {@code true} to abort the connection before the handler writes its
	 *        response, {@code false} to read the complete response
	 * @return the exchange
	 * @throws Exception
	 *         if any error occurs
	 */
	private Exchange exchange(ControllerKind kind, String path, String accept, boolean clientAbort)
			throws Exception {
		final String requestId = UUID.randomUUID().toString();
		final TrackedRequest req = tracker.request(requestId);
		try (LogCapture logs = new LogCapture();
				Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
			socket.setSoTimeout((int) TIMEOUT.toMillis());

			// @formatter:off
			final String httpRequest =
					"GET " + contextPath + TEST_PATH + kind.path + path
							+ "?" + REQUEST_ID_PARAM + "=" + requestId + " HTTP/1.1\r\n"
					+ "Host: localhost:" + port + "\r\n"
					+ "Accept: " + accept + "\r\n"
					+ "Connection: close\r\n"
					+ "\r\n";
			// @formatter:on
			socket.getOutputStream().write(httpRequest.getBytes(US_ASCII));
			socket.getOutputStream().flush();

			then(req.handling.await(TIMEOUT.toMillis(), MILLISECONDS)).as("Request handler invoked")
					.isTrue();

			String response = null;
			if ( clientAbort ) {
				// close with a TCP reset, so the server fails on its next write to the client
				socket.setSoLinger(true, 0);
				socket.close();
				Thread.sleep(CLIENT_ABORT_DELAY);
				req.clientReady.countDown();
			} else {
				req.clientReady.countDown();
				response = new String(socket.getInputStream().readAllBytes(), UTF_8);
			}

			then(req.completed.await(TIMEOUT.toMillis(), MILLISECONDS))
					.as("Request processing completed").isTrue();
			return new Exchange(response, req.ioAllowed, logs.events(req.threadName));
		} finally {
			tracker.requests.remove(requestId);
		}
	}

	private static boolean aboveDebug(ILoggingEvent event) {
		return event.getLevel().isGreaterOrEqual(Level.INFO);
	}

	/**
	 * Test if a log event is a warning or error from the servlet container or
	 * Spring, such as for an exception handler failure or an exception thrown
	 * out of the application to the container.
	 */
	private static boolean frameworkWarning(ILoggingEvent event) {
		final String loggerName = event.getLoggerName();
		return event.getLevel().isGreaterOrEqual(Level.WARN) && (loggerName.startsWith("org.apache.")
				|| loggerName.startsWith("org.springframework."));
	}

	private static String logEventKey(ILoggingEvent event) {
		return event.getLevel() + " " + event.getLoggerName() + ": " + event.getMessage();
	}

	private static String describe(ILoggingEvent event) {
		final StringBuilder buf = new StringBuilder();
		buf.append(event.getLevel()).append(' ').append(event.getLoggerName()).append(": ")
				.append(event.getFormattedMessage());
		IThrowableProxy t = event.getThrowableProxy();
		for ( int i = 0; t != null && i < 10; i++, t = t.getCause() ) {
			buf.append(i == 0 ? "\n\t" : "\n\tcaused by ").append(t.getClassName()).append(": ")
					.append(t.getMessage());
		}
		return buf.toString();
	}

	/**
	 * Get the log events above {@code DEBUG} of an aborted exchange that the
	 * completed exchange did not also log.
	 *
	 * @param completed
	 *        the exchange where the client read the complete response
	 * @param aborted
	 *        the exchange where the client aborted the connection
	 * @return descriptions of the log events
	 */
	private static List<String> clientAbortLogEvents(Exchange completed, Exchange aborted) {
		final List<String> completedEventKeys = new ArrayList<>(completed.logEvents().stream()
				.filter(ClientAbortWebTests::aboveDebug).map(ClientAbortWebTests::logEventKey).toList());
		final List<String> result = new ArrayList<>();
		for ( ILoggingEvent event : aborted.logEvents() ) {
			if ( aboveDebug(event) && !completedEventKeys.remove(logEventKey(event)) ) {
				result.add(describe(event));
			}
		}
		return result;
	}

	/**
	 * Assert a client abort was handled without logging above {@code DEBUG}.
	 *
	 * <p>
	 * The server must have failed writing to the aborted connection, the
	 * aborted exchange must not have logged anything above {@code DEBUG} that
	 * the completed exchange did not also log, and the completed exchange must
	 * not have logged a servlet container or Spring warning.
	 * </p>
	 *
	 * @param completed
	 *        the exchange where the client read the complete response
	 * @param aborted
	 *        the exchange where the client aborted the connection
	 */
	private static void thenClientAbortNotLogged(Exchange completed, Exchange aborted) {
		// @formatter:off
		then(aborted.ioAllowed())
			.as("Server failed writing to aborted client connection")
			.isFalse()
			;

		then(clientAbortLogEvents(completed, aborted))
			.as("No log events above DEBUG caused by client abort")
			.isEmpty()
			;

		then(completed.logEvents())
			.filteredOn(ClientAbortWebTests::frameworkWarning)
			.extracting(ClientAbortWebTests::describe)
			.as("No servlet container or Spring warnings, even with connected client")
			.isEmpty()
			;
		// @formatter:on
	}

	private static Stream<Arguments> responseBodyCases() {
		return Arrays.stream(ControllerKind.values()).flatMap(
				kind -> RESPONSE_BODY_MEDIA_TYPES.stream().map(accept -> Arguments.of(kind, accept)));
	}

	@ParameterizedTest
	@MethodSource("responseBodyCases")
	public void responseBody_clientAbort(ControllerKind kind, String accept) throws Exception {
		// WHEN
		final Exchange completed = exchange(kind, "/result", accept, false);
		final Exchange aborted = exchange(kind, "/result", accept, true);

		// THEN
		then(completed.status()).as("Response body returned").isEqualTo(200);
		thenClientAbortNotLogged(completed, aborted);
	}

	private static Stream<Arguments> streamingResponseCases() {
		// the query cache only applies to SolarQuery paths that return a response body
		return Stream.of(ControllerKind.App, ControllerKind.Rest)
				.flatMap(kind -> STREAMING_PATHS.stream().map(path -> Arguments.of(kind, path)));
	}

	@ParameterizedTest
	@MethodSource("streamingResponseCases")
	public void streamingResponse_clientAbort(ControllerKind kind, String path) throws Exception {
		// WHEN
		final Exchange completed = exchange(kind, path, MediaType.APPLICATION_JSON_VALUE, false);
		final Exchange aborted = exchange(kind, path, MediaType.APPLICATION_JSON_VALUE, true);

		// THEN
		then(completed.status()).as("Response streamed").isEqualTo(200);
		thenClientAbortNotLogged(completed, aborted);
	}

	private static Stream<Arguments> exceptionHandlerCases() {
		return Arrays.stream(ControllerKind.values())
				.flatMap(kind -> EXCEPTION_CASES.stream().map(c -> Arguments.of(kind, c)));
	}

	@ParameterizedTest
	@MethodSource("exceptionHandlerCases")
	public void exceptionHandler_clientAbort(ControllerKind kind, ExceptionCase exceptionCase)
			throws Exception {
		// GIVEN
		final String path = "/throw/" + exceptionCase.name();

		// WHEN
		final Exchange completed = exchange(kind, path, MediaType.APPLICATION_JSON_VALUE, false);
		final Exchange aborted = exchange(kind, path, MediaType.APPLICATION_JSON_VALUE, true);

		// THEN
		then(completed.status()).as("Exception handler response returned")
				.isEqualTo(exceptionCase.status());
		thenClientAbortNotLogged(completed, aborted);
	}

}
