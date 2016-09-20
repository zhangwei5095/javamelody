/*
 * Copyright 2008-2016 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bull.javamelody; // NOPMD

import static net.bull.javamelody.HttpParameters.ACTION_PARAMETER;
import static net.bull.javamelody.HttpParameters.COLLECTOR_PARAMETER;
import static net.bull.javamelody.HttpParameters.CONNECTIONS_PART;
import static net.bull.javamelody.HttpParameters.COUNTER_PARAMETER;
import static net.bull.javamelody.HttpParameters.COUNTER_SUMMARY_PER_CLASS_PART;
import static net.bull.javamelody.HttpParameters.CURRENT_REQUESTS_PART;
import static net.bull.javamelody.HttpParameters.DATABASE_PART;
import static net.bull.javamelody.HttpParameters.DEFAULT_WITH_CURRENT_REQUESTS_PART;
import static net.bull.javamelody.HttpParameters.EXPLAIN_PLAN_PART;
import static net.bull.javamelody.HttpParameters.FORMAT_PARAMETER;
import static net.bull.javamelody.HttpParameters.GRAPH_PARAMETER;
import static net.bull.javamelody.HttpParameters.GRAPH_PART;
import static net.bull.javamelody.HttpParameters.HEIGHT_PARAMETER;
import static net.bull.javamelody.HttpParameters.JMX_VALUE;
import static net.bull.javamelody.HttpParameters.JNDI_PART;
import static net.bull.javamelody.HttpParameters.JNLP_PART;
import static net.bull.javamelody.HttpParameters.JROBINS_PART;
import static net.bull.javamelody.HttpParameters.JVM_PART;
import static net.bull.javamelody.HttpParameters.LAST_VALUE_PART;
import static net.bull.javamelody.HttpParameters.MBEANS_PART;
import static net.bull.javamelody.HttpParameters.OTHER_JROBINS_PART;
import static net.bull.javamelody.HttpParameters.PART_PARAMETER;
import static net.bull.javamelody.HttpParameters.PERIOD_PARAMETER;
import static net.bull.javamelody.HttpParameters.POM_XML_PART;
import static net.bull.javamelody.HttpParameters.PROCESSES_PART;
import static net.bull.javamelody.HttpParameters.REQUEST_PARAMETER;
import static net.bull.javamelody.HttpParameters.RESOURCE_PARAMETER;
import static net.bull.javamelody.HttpParameters.RUNTIME_DEPENDENCIES_PART;
import static net.bull.javamelody.HttpParameters.SESSIONS_PART;
import static net.bull.javamelody.HttpParameters.SESSION_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.THREADS_DUMP_PART;
import static net.bull.javamelody.HttpParameters.THREADS_PART;
import static net.bull.javamelody.HttpParameters.USAGES_PART;
import static net.bull.javamelody.HttpParameters.WEB_XML_PART;
import static net.bull.javamelody.HttpParameters.WIDTH_PARAMETER;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.ehcache.CacheManager;

/**
 * Test unitaire de la classe MonitoringFilter.
 * @author Emeric Vernat
 */
// CHECKSTYLE:OFF
public class TestMonitoringFilter { // NOPMD
	// CHECKSTYLE:ON
	private static final String FILTER_NAME = "monitoring";
	// identique à HttpCookieManager.PERIOD_COOKIE_NAME
	private static final String PERIOD_COOKIE_NAME = "javamelody.period";
	private static final String REMOTE_ADDR = "127.0.0.1"; // NOPMD
	private static final String CONTEXT_PATH = "/test";
	private static final String GRAPH = "graph";
	private static final String TRUE = "true";
	private MonitoringFilter monitoringFilter;

	/**
	 * Initialisation (deux Before ne garantissent pas l'ordre dans Eclipse).
	 */
	public TestMonitoringFilter() {
		super();
		Utils.initialize();
	}

	/**
	 * Initialisation.
	 * @throws ServletException e
	 */
	@Before
	public void setUp() throws ServletException {
		try {
			final Field field = MonitoringFilter.class.getDeclaredField("instanceCreated");
			field.setAccessible(true);
			field.set(null, false);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (final NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
		final FilterConfig config = createNiceMock(FilterConfig.class);
		final ServletContext context = createNiceMock(ServletContext.class);
		expect(config.getServletContext()).andReturn(context).anyTimes();
		expect(config.getFilterName()).andReturn(FILTER_NAME).anyTimes();
		// anyTimes sur getInitParameter car TestJdbcDriver a pu fixer la propriété système à false
		expect(context.getInitParameter(
				Parameters.PARAMETER_SYSTEM_PREFIX + Parameter.DISABLED.getCode())).andReturn(null)
						.anyTimes();
		expect(config.getInitParameter(Parameter.DISABLED.getCode())).andReturn(null).anyTimes();
		expect(context.getMajorVersion()).andReturn(2).anyTimes();
		expect(context.getMinorVersion()).andReturn(5).anyTimes();
		expect(context.getServletContextName()).andReturn("test webapp").anyTimes();
		// mockJetty pour avoir un applicationServerIconName dans JavaInformations
		expect(context.getServerInfo()).andReturn("mockJetty").anyTimes();
		// dependencies pour avoir des dépendances dans JavaInformations
		final Set<String> dependencies = new LinkedHashSet<String>(
				Arrays.asList("/WEB-INF/lib/jrobin.jar", "/WEB-INF/lib/javamelody.jar"));
		// et flags pour considérer que les ressources pom.xml et web.xml existent
		JavaInformations.setWebXmlExistsAndPomXmlExists(true, true);
		expect(context.getResourcePaths("/WEB-INF/lib/")).andReturn(dependencies).anyTimes();
		expect(context.getContextPath()).andReturn(CONTEXT_PATH).anyTimes();
		monitoringFilter = new MonitoringFilter();
		replay(config);
		replay(context);
		monitoringFilter.init(config);
		verify(config);
		verify(context);
	}

	/** Test.
	 * @throws ServletException e */
	@Test
	public void testLog() throws ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRemoteAddr()).andReturn(REMOTE_ADDR);
		expect(request.getRequestURI()).andReturn("/test/request");
		expect(request.getContextPath()).andReturn(CONTEXT_PATH);
		expect(request.getQueryString()).andReturn("param1=1");
		expect(request.getMethod()).andReturn("GET");

		setProperty(Parameter.LOG, TRUE);

		setUp();

		replay(request);
		monitoringFilter.log(request, "test", 1000, false, 10000);
		verify(request);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilterNoHttp() throws ServletException, IOException {
		final FilterChain servletChain = createNiceMock(FilterChain.class);
		final ServletRequest servletRequest = createNiceMock(ServletRequest.class);
		final ServletResponse servletResponse = createNiceMock(ServletResponse.class);
		replay(servletRequest);
		replay(servletResponse);
		replay(servletChain);
		monitoringFilter.doFilter(servletRequest, servletResponse, servletChain);
		verify(servletRequest);
		verify(servletResponse);
		verify(servletChain);

		final FilterChain servletChain2 = createNiceMock(FilterChain.class);
		final HttpServletRequest servletRequest2 = createNiceMock(HttpServletRequest.class);
		final ServletResponse servletResponse2 = createNiceMock(ServletResponse.class);
		replay(servletRequest2);
		replay(servletResponse2);
		replay(servletChain2);
		monitoringFilter.doFilter(servletRequest2, servletResponse2, servletChain2);
		verify(servletRequest2);
		verify(servletResponse2);
		verify(servletChain2);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilter() throws ServletException, IOException {
		// displayed-counters
		setProperty(Parameter.DISPLAYED_COUNTERS, "sql");
		try {
			setUp();
			doFilter(createNiceMock(HttpServletRequest.class));
			setProperty(Parameter.DISPLAYED_COUNTERS, "");
			setUp();
			doFilter(createNiceMock(HttpServletRequest.class));
			setProperty(Parameter.DISPLAYED_COUNTERS, "unknown");
			try {
				setUp();
				doFilter(createNiceMock(HttpServletRequest.class));
			} catch (final IllegalArgumentException e) {
				assertNotNull("ok", e);
			}
		} finally {
			setProperty(Parameter.DISPLAYED_COUNTERS, null);
		}

		// url exclue
		setProperty(Parameter.URL_EXCLUDE_PATTERN, ".*");
		try {
			setUp();
			doFilter(createNiceMock(HttpServletRequest.class));
		} finally {
			setProperty(Parameter.URL_EXCLUDE_PATTERN, "");
		}

		// standard
		setUp();
		doFilter(createNiceMock(HttpServletRequest.class));

		// log
		setUp();
		setProperty(Parameter.LOG, TRUE);
		try {
			((Logger) org.slf4j.LoggerFactory.getLogger(FILTER_NAME)).setLevel(Level.WARN);
			doFilter(createNiceMock(HttpServletRequest.class));

			((Logger) org.slf4j.LoggerFactory.getLogger(FILTER_NAME)).setLevel(Level.DEBUG);
			doFilter(createNiceMock(HttpServletRequest.class));

			final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
			expect(request.getHeader("X-Forwarded-For")).andReturn("me").anyTimes();
			expect(request.getQueryString()).andReturn("param1=1").anyTimes();
			doFilter(request);
		} finally {
			setProperty(Parameter.LOG, null);
		}

		// ajax
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getHeader("X-Requested-With")).andReturn("XMLHttpRequest");
		doFilter(request);

		// erreur système http, avec log
		setProperty(Parameter.LOG, TRUE);
		try {
			final String test = "test";
			doFilter(createNiceMock(HttpServletRequest.class), new UnknownError(test));
			doFilter(createNiceMock(HttpServletRequest.class), new IllegalStateException(test));
			// pas possibles:
			//			doFilter(createNiceMock(HttpServletRequest.class), new IOException(test));
			//			doFilter(createNiceMock(HttpServletRequest.class), new ServletException(test));
			//			doFilter(createNiceMock(HttpServletRequest.class), new Exception(test));
		} finally {
			setProperty(Parameter.LOG, null);
		}
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilterWithSession() throws ServletException, IOException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		final HttpSession session = createNiceMock(HttpSession.class);
		expect(request.getSession(false)).andReturn(session);
		expect(request.getLocale()).andReturn(Locale.FRANCE);
		replay(session);
		doFilter(request);
		verify(session);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilterWithSessionBis() throws ServletException, IOException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		final HttpSession session = createNiceMock(HttpSession.class);
		expect(request.getSession(false)).andReturn(session);
		// Locale sans pays
		expect(request.getLocale()).andReturn(Locale.FRENCH).anyTimes();
		// "X-Forwarded-For"
		expect(request.getHeader("X-Forwarded-For")).andReturn("somewhere").anyTimes();
		// getRemoteUser
		expect(request.getRemoteUser()).andReturn("me").anyTimes();
		replay(session);
		doFilter(request);
		verify(session);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilterWithSessionTer() throws ServletException, IOException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		final HttpSession session = createNiceMock(HttpSession.class);
		expect(request.getSession(false)).andReturn(session);
		expect(session.getAttribute(SessionInformations.SESSION_COUNTRY_KEY))
				.andReturn(Locale.FRANCE.getCountry()).anyTimes();
		expect(session.getAttribute(SessionInformations.SESSION_REMOTE_ADDR)).andReturn("somewhere")
				.anyTimes();
		expect(session.getAttribute(SessionInformations.SESSION_REMOTE_USER)).andReturn("me")
				.anyTimes();
		replay(session);
		doFilter(request);
		verify(session);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoFilterWithGWT() throws ServletException, IOException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		final String textGwtRpc = "text/x-gwt-rpc";
		expect(request.getContentType()).andReturn(textGwtRpc).anyTimes();
		expect(request.getInputStream())
				.andReturn(createInputStreamForString("1|2|3|4|5|6|7|8|9|10")).anyTimes();
		doFilter(request);

		final HttpServletRequest request2a = createNiceMock(HttpServletRequest.class);
		expect(request2a.getContentType()).andReturn("not/x-gwt-rpc").anyTimes();
		expect(request2a.getInputStream())
				.andReturn(createInputStreamForString("1|2|3|4|5|6|7|8|9|10")).anyTimes();
		doFilter(request2a);

		final HttpServletRequest request2b = createNiceMock(HttpServletRequest.class);
		expect(request2b.getContentType()).andReturn(textGwtRpc).anyTimes();
		expect(request2b.getInputStream()).andReturn(createInputStreamForString("1|2|3|4|5|6"))
				.anyTimes();
		expect(request2b.getReader()).andReturn(new BufferedReader(new StringReader("1|2|3|4|5|6")))
				.anyTimes();
		replay(request2b);
		final PayloadNameRequestWrapper wrapper2b = new PayloadNameRequestWrapper(request2b);
		wrapper2b.getInputStream().read();
		wrapper2b.getReader().read();
		verify(request2b);

		final HttpServletRequest request2 = createNiceMock(HttpServletRequest.class);
		expect(request2.getContentType()).andReturn(textGwtRpc).anyTimes();
		expect(request2.getInputStream())
				.andReturn(createInputStreamForString("1|2|3|4|5|6||8|9|10")).anyTimes();
		expect(request2.getReader()).andReturn(new BufferedReader(new StringReader("1|2|3|4|5|6")))
				.anyTimes();
		replay(request2);
		final PayloadNameRequestWrapper wrapper2 = new PayloadNameRequestWrapper(request2);
		wrapper2.getInputStream().read();
		wrapper2.getReader().read();
		verify(request2);

		final HttpServletRequest request3 = createNiceMock(HttpServletRequest.class);
		expect(request3.getContentType()).andReturn(textGwtRpc).anyTimes();
		expect(request3.getCharacterEncoding()).andReturn("utf-8").anyTimes();
		expect(request3.getInputStream())
				.andReturn(createInputStreamForString("1|2|3|4|5|6||8|9|10")).anyTimes();
		expect(request3.getReader()).andReturn(new BufferedReader(new StringReader("1|2|3|4|5|6")))
				.anyTimes();
		replay(request3);
		final PayloadNameRequestWrapper wrapper3 = new PayloadNameRequestWrapper(request3);
		wrapper3.getInputStream().read();
		wrapper3.getInputStream().read();
		wrapper3.getReader().read();
		wrapper3.getReader().read();
		verify(request3);
	}

	private ServletInputStream createInputStreamForString(final String string) {
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				string.getBytes());
		// CHECKSTYLE:OFF
		final ServletInputStream inputStream = new ServletInputStream() {
			// CHECKSTYLE:ON
			@Override
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				// nothing
			}
		};
		return inputStream;
	}

	private void doFilter(HttpServletRequest request) throws ServletException, IOException {
		doFilter(request, null);
	}

	private void doFilter(HttpServletRequest request, Throwable exceptionInDoFilter)
			throws ServletException, IOException {
		final FilterChain chain = createNiceMock(FilterChain.class);
		expect(request.getRequestURI()).andReturn("/test/request").anyTimes();
		expect(request.getContextPath()).andReturn(CONTEXT_PATH).anyTimes();
		expect(request.getMethod()).andReturn("GET").anyTimes();
		if (exceptionInDoFilter != null) {
			// cela fera une erreur système http comptée dans les stats
			expect(request.getRemoteUser()).andThrow(exceptionInDoFilter);
		}
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);

		replay(request);
		replay(response);
		replay(chain);
		if (exceptionInDoFilter != null) {
			try {
				monitoringFilter.doFilter(request, response, chain);
			} catch (final Throwable t) { // NOPMD
				assertNotNull("ok", t);
			}
		} else {
			monitoringFilter.doFilter(request, response, chain);
		}
		verify(request);
		verify(response);
		verify(chain);
	}

	/** Test.
	 * @throws IOException e */
	@Test
	public void testFilterServletResponseWrapper() throws IOException {
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		expect(response.getOutputStream())
				.andReturn(new FilterServletOutputStream(new ByteArrayOutputStream())).anyTimes();
		expect(response.getCharacterEncoding()).andReturn("ISO-8859-1").anyTimes();
		final CounterServletResponseWrapper wrappedResponse = new CounterServletResponseWrapper(
				response);
		replay(response);
		assertNotNull("getOutputStream", wrappedResponse.getOutputStream());
		assertNotNull("getOutputStream bis", wrappedResponse.getOutputStream());
		assertNotNull("getOutputStream", wrappedResponse.getCharacterEncoding());
		wrappedResponse.close();
		verify(response);

		final HttpServletResponse response2 = createNiceMock(HttpServletResponse.class);
		expect(response2.getOutputStream())
				.andReturn(new FilterServletOutputStream(new ByteArrayOutputStream())).anyTimes();
		expect(response2.getCharacterEncoding()).andReturn(null).anyTimes();
		final CounterServletResponseWrapper wrappedResponse2 = new CounterServletResponseWrapper(
				response);
		replay(response2);
		assertNotNull("getWriter", wrappedResponse2.getWriter());
		assertNotNull("getWriter bis", wrappedResponse2.getWriter());
		wrappedResponse2.close();
		verify(response2);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoring() throws ServletException, IOException {
		monitoring(Collections.<String, String> emptyMap());
		monitoring(Collections.<String, String> singletonMap(FORMAT_PARAMETER, "html"));
		monitoring(Collections.<String, String> singletonMap(FORMAT_PARAMETER, "htmlbody"));
		setProperty(Parameter.DISABLED, Boolean.TRUE.toString());
		try {
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
		} finally {
			monitoringFilter.destroy();
			setProperty(Parameter.DISABLED, Boolean.FALSE.toString());
		}
		setProperty(Parameter.NO_DATABASE, Boolean.TRUE.toString());
		try {
			setUp();
			monitoring(Collections.<String, String> emptyMap());
		} finally {
			setProperty(Parameter.NO_DATABASE, Boolean.FALSE.toString());
		}
		setProperty(Parameter.ALLOWED_ADDR_PATTERN, "256.*");
		try {
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
			setProperty(Parameter.ALLOWED_ADDR_PATTERN, ".*");
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
		} finally {
			setProperty(Parameter.ALLOWED_ADDR_PATTERN, null);
		}
		setProperty(Parameter.AUTHORIZED_USERS, "admin:password, ");
		try {
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
			setProperty(Parameter.AUTHORIZED_USERS, "");
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
		} finally {
			setProperty(Parameter.AUTHORIZED_USERS, null);
		}
		setProperty(Parameter.MONITORING_PATH, "/admin/monitoring");
		try {
			setUp();
			monitoring(Collections.<String, String> emptyMap(), false);
		} finally {
			setProperty(Parameter.MONITORING_PATH, "/monitoring");
		}
		setProperty(Parameter.MAIL_SESSION, "testmailsession");
		setProperty(Parameter.ADMIN_EMAILS, null);
		setUp();
		monitoring(Collections.<String, String> emptyMap());
		setProperty(Parameter.ADMIN_EMAILS, "evernat@free.fr");
		setUp();
		monitoring(Collections.<String, String> emptyMap());
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithPeriod() throws ServletException, IOException {
		monitoring(
				Collections.<String, String> singletonMap(PERIOD_PARAMETER, Period.JOUR.getCode()));
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithResource() throws ServletException, IOException {
		monitoring(Collections.<String, String> singletonMap(RESOURCE_PARAMETER, "monitoring.css"));
		monitoring(Collections.<String, String> singletonMap(RESOURCE_PARAMETER, "beans.png"));
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithGraph() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(GRAPH, "usedMemory");
		parameters.put("width", "800");
		parameters.put("height", "600");
		monitoring(parameters);
		parameters.put(GRAPH, "unknown");
		monitoring(parameters, false);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithParts() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();

		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, THREADS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, THREADS_DUMP_PART);
		monitoring(parameters);

		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		monitoring(parameters);
		monitorJdbcParts(parameters);
		parameters.remove(FORMAT_PARAMETER);
		parameters.put(REQUEST_PARAMETER, "0");
		monitoring(parameters);
		parameters.remove(REQUEST_PARAMETER);
		// il ne faut pas faire un heapHisto sans thread comme dans TestHtmlHeapHistogramReport
		//		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		//		monitoring(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		parameters.put(SESSION_ID_PARAMETER, "expired session");
		monitoring(parameters);
		parameters.remove(SESSION_ID_PARAMETER);
		parameters.put(PART_PARAMETER, WEB_XML_PART);
		monitoring(parameters, false);
		parameters.put(PART_PARAMETER, POM_XML_PART);
		monitoring(parameters, false);
		parameters.put(PART_PARAMETER, JNDI_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, MBEANS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, JNLP_PART);
		monitoring(parameters);
		parameters.remove(PART_PARAMETER);
		parameters.put(JMX_VALUE, "java.lang:type=OperatingSystem.ProcessCpuTime");
		monitoring(parameters);
		parameters.remove(JMX_VALUE);
		parameters.put(PART_PARAMETER, COUNTER_SUMMARY_PER_CLASS_PART);
		parameters.put(COUNTER_PARAMETER, "services");
		monitoring(parameters);
		parameters.put(GRAPH, "unknown service");
		monitoring(parameters);
		parameters.remove(COUNTER_PARAMETER);

		doMonitoringWithGraphPart();

		parameters.put(PART_PARAMETER, "unknown part");
		boolean exception = false;
		try {
			monitoring(parameters);
		} catch (final IllegalArgumentException e) {
			exception = true;
		}
		assertTrue("exception if unknown part", exception);
	}

	private void doMonitoringWithGraphPart() throws IOException, ServletException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(PART_PARAMETER, GRAPH);
		parameters.put(GRAPH, "usedMemory");
		monitoring(parameters);

		parameters.put(PART_PARAMETER, LAST_VALUE_PART);
		parameters.put(GRAPH, "usedMemory,cpu,unknown");
		monitoring(parameters);

		parameters.put(PART_PARAMETER, USAGES_PART);
		parameters.put(GRAPH, "unknown");
		monitoring(parameters);
	}

	private void monitorJdbcParts(Map<String, String> parameters)
			throws IOException, ServletException {
		final Connection connection = TestDatabaseInformations.initH2();
		try {
			parameters.put(PART_PARAMETER, DATABASE_PART);
			monitoring(parameters);
			parameters.put(PART_PARAMETER, DATABASE_PART);
			parameters.put(REQUEST_PARAMETER, "0");
			monitoring(parameters);
			parameters.put(PART_PARAMETER, CONNECTIONS_PART);
			monitoring(parameters);
			parameters.put(PART_PARAMETER, CONNECTIONS_PART);
			parameters.put(FORMAT_PARAMETER, "htmlbody");
			monitoring(parameters);
		} finally {
			try {
				connection.close();
			} catch (final SQLException e) {
				LOG.warn(e.toString(), e);
			}
		}
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithActions() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();

		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		parameters.put(ACTION_PARAMETER, Action.GC.toString());
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.INVALIDATE_SESSIONS.toString());
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.INVALIDATE_SESSION.toString());
		parameters.put(SESSION_ID_PARAMETER, "123456789");
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.CLEAR_CACHES.toString());
		monitoring(parameters);
		if (CacheManager.getInstance().getCache("test clear") == null) {
			CacheManager.getInstance().addCache("test clear");
		}
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.PAUSE_JOB.toString());
		parameters.put("jobId", "all");
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.RESUME_JOB.toString());
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.CLEAR_COUNTER.toString());
		parameters.put("counter", "all");
		monitoring(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithFormatPdf() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, "pdf");
		monitoring(parameters);

		parameters.put(PART_PARAMETER, RUNTIME_DEPENDENCIES_PART);
		parameters.put(COUNTER_PARAMETER, "services");
		monitoring(parameters);
		parameters.remove(COUNTER_PARAMETER);

		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		monitoring(parameters);

		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		monitoring(parameters);

		parameters.put(PART_PARAMETER, PROCESSES_PART);
		monitoring(parameters);

		parameters.put(PART_PARAMETER, MBEANS_PART);
		monitoring(parameters);

		parameters.put(PART_PARAMETER, COUNTER_SUMMARY_PER_CLASS_PART);
		parameters.put(COUNTER_PARAMETER, "guice");
		monitoring(parameters);
		parameters.remove(COUNTER_PARAMETER);

		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		monitoring(parameters);

		// il ne faut pas faire un heapHisto sans thread comme dans TestHtmlHeapHistogramReport
		//		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		//		monitoring(parameters);

		parameters.put(PART_PARAMETER, "unknown part");
		boolean exception = false;
		try {
			monitoring(parameters);
		} catch (final Exception e) {
			exception = true;
		}
		assertTrue("exception if unknown part", exception);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	// CHECKSTYLE:OFF
	@Test
	// CHECKSTYLE:ON
	public void testDoMonitoringWithFormatSerialized() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, TransportFormat.SERIALIZED.getCode());
		monitoring(parameters);

		parameters.put(JMX_VALUE, "java.lang:type=OperatingSystem.ProcessCpuTime");
		monitoring(parameters);
		parameters.remove(JMX_VALUE);

		parameters.put(PART_PARAMETER, LAST_VALUE_PART);
		parameters.put(GRAPH, "usedMemory,cpu,unknown");
		monitoring(parameters);
		parameters.remove(GRAPH);

		parameters.put(PART_PARAMETER, JVM_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, THREADS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, DEFAULT_WITH_CURRENT_REQUESTS_PART);
		monitoring(parameters);
		parameters.put(WIDTH_PARAMETER, "80");
		parameters.put(HEIGHT_PARAMETER, "80");
		parameters.put(PART_PARAMETER, JROBINS_PART);
		monitoring(parameters);
		parameters.put(GRAPH_PARAMETER, "cpu");
		parameters.put(PART_PARAMETER, JROBINS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, GRAPH_PART);
		monitoring(parameters);
		parameters.remove(GRAPH_PARAMETER);
		parameters.put(PART_PARAMETER, OTHER_JROBINS_PART);
		monitoring(parameters);
		parameters.remove(WIDTH_PARAMETER);
		parameters.remove(HEIGHT_PARAMETER);
		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		parameters.put(SESSION_ID_PARAMETER, "expired session");
		monitoring(parameters);
		parameters.remove(SESSION_ID_PARAMETER);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, JNDI_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, MBEANS_PART);
		monitoring(parameters);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		monitoring(parameters);
		parameters.put(REQUEST_PARAMETER, "0");
		monitoring(parameters);
		parameters.put(PART_PARAMETER, EXPLAIN_PLAN_PART);
		parameters.put(REQUEST_PARAMETER, "select 1 from dual");
		monitoring(parameters);
		parameters.remove(REQUEST_PARAMETER);
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, COUNTER_SUMMARY_PER_CLASS_PART);
		parameters.put(COUNTER_PARAMETER, "guice");
		monitoring(parameters);
		parameters.put(PERIOD_PARAMETER, "jour");
		monitoring(parameters);
		parameters.remove(COUNTER_PARAMETER);
		parameters.remove(PERIOD_PARAMETER);
		// il ne faut pas faire un heapHisto sans thread comme dans TestHtmlHeapHistogramReport
		//		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		//		monitoring(parameters);
		parameters.put(PART_PARAMETER, null);
		parameters.put(COLLECTOR_PARAMETER, "stop");
		monitoring(parameters);
		parameters.put(ACTION_PARAMETER, Action.GC.toString());
		monitoring(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithFormatXml() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, TransportFormat.XML.getCode());
		monitoring(parameters);
		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		monitoring(parameters);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		monitoring(parameters);
		// il ne faut pas faire un heapHisto sans thread comme dans TestHtmlHeapHistogramReport
		//		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		//		monitoring(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoMonitoringWithFormatJson() throws ServletException, IOException {
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, TransportFormat.JSON.getCode());
		monitoring(parameters);
		parameters.put(PART_PARAMETER, THREADS_PART);
		monitoring(parameters);
		setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		monitoring(parameters);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		monitoring(parameters);
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		monitoring(parameters);
		// il ne faut pas faire un heapHisto sans thread comme dans TestHtmlHeapHistogramReport
		//		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		//		monitoring(parameters);
	}

	private void monitoring(Map<String, String> parameters) throws IOException, ServletException {
		monitoring(parameters, true);
	}

	private void monitoring(Map<String, String> parameters, boolean checkResultContent)
			throws IOException, ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRequestURI()).andReturn("/test/monitoring").anyTimes();
		expect(request.getRequestURL()).andReturn(new StringBuffer("/test/monitoring")).anyTimes();
		expect(request.getContextPath()).andReturn(CONTEXT_PATH).anyTimes();
		expect(request.getRemoteAddr()).andReturn("here").anyTimes();
		final Random random = new Random();
		if (random.nextBoolean()) {
			expect(request.getHeaders("Accept-Encoding"))
					.andReturn(Collections.enumeration(Arrays.asList("application/gzip")))
					.anyTimes();
		} else {
			expect(request.getHeaders("Accept-Encoding"))
					.andReturn(Collections.enumeration(Arrays.asList("text/html"))).anyTimes();
		}
		for (final Map.Entry<String, String> entry : parameters.entrySet()) {
			if (REQUEST_PARAMETER.equals(entry.getKey())) {
				expect(request.getHeader(entry.getKey())).andReturn(entry.getValue()).anyTimes();
			} else {
				expect(request.getParameter(entry.getKey())).andReturn(entry.getValue()).anyTimes();
			}
		}
		if (parameters.isEmpty() || JNLP_PART.equals(parameters.get(PART_PARAMETER))) {
			// dans au moins un cas on met un cookie
			final Cookie[] cookies = { new Cookie("dummy", "dummy"),
					new Cookie(PERIOD_COOKIE_NAME, Period.SEMAINE.getCode()), };
			expect(request.getCookies()).andReturn(cookies).anyTimes();
		}
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		expect(response.getOutputStream()).andReturn(new FilterServletOutputStream(output))
				.anyTimes();
		final StringWriter stringWriter = new StringWriter();
		expect(response.getWriter()).andReturn(new PrintWriter(stringWriter)).anyTimes();
		final FilterChain chain = createNiceMock(FilterChain.class);

		replay(request);
		replay(response);
		replay(chain);
		monitoringFilter.doFilter(request, response, chain);
		verify(request);
		verify(response);
		verify(chain);

		if (checkResultContent) {
			assertTrue("result", output.size() != 0 || stringWriter.getBuffer().length() != 0);
		}
	}

	private static void setProperty(Parameter parameter, String value) {
		Utils.setProperty(parameter, value);
	}
}
