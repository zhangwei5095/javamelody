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
import static net.bull.javamelody.HttpParameters.APPLICATIONS_PART;
import static net.bull.javamelody.HttpParameters.CACHE_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.CONNECTIONS_PART;
import static net.bull.javamelody.HttpParameters.COUNTER_PARAMETER;
import static net.bull.javamelody.HttpParameters.COUNTER_SUMMARY_PER_CLASS_PART;
import static net.bull.javamelody.HttpParameters.CURRENT_REQUESTS_PART;
import static net.bull.javamelody.HttpParameters.DATABASE_PART;
import static net.bull.javamelody.HttpParameters.EXPLAIN_PLAN_PART;
import static net.bull.javamelody.HttpParameters.FORMAT_PARAMETER;
import static net.bull.javamelody.HttpParameters.GRAPH_PARAMETER;
import static net.bull.javamelody.HttpParameters.HEAP_HISTO_PART;
import static net.bull.javamelody.HttpParameters.HEIGHT_PARAMETER;
import static net.bull.javamelody.HttpParameters.HOTSPOTS_PART;
import static net.bull.javamelody.HttpParameters.JMX_VALUE;
import static net.bull.javamelody.HttpParameters.JNDI_PART;
import static net.bull.javamelody.HttpParameters.JOB_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.JROBINS_PART;
import static net.bull.javamelody.HttpParameters.JVM_PART;
import static net.bull.javamelody.HttpParameters.MBEANS_PART;
import static net.bull.javamelody.HttpParameters.OTHER_JROBINS_PART;
import static net.bull.javamelody.HttpParameters.PART_PARAMETER;
import static net.bull.javamelody.HttpParameters.PATH_PARAMETER;
import static net.bull.javamelody.HttpParameters.POM_XML_PART;
import static net.bull.javamelody.HttpParameters.PROCESSES_PART;
import static net.bull.javamelody.HttpParameters.REQUEST_PARAMETER;
import static net.bull.javamelody.HttpParameters.SESSIONS_PART;
import static net.bull.javamelody.HttpParameters.SESSION_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.THREADS_PART;
import static net.bull.javamelody.HttpParameters.THREAD_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.WEB_XML_PART;
import static net.bull.javamelody.HttpParameters.WIDTH_PARAMETER;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire de la classe CollectorServlet.
 * @author Emeric Vernat
 */
public class TestCollectorServletWithParts {
	private static final String TRUE = "true";
	private static final String TEST = "test";
	private CollectorServlet collectorServlet;

	/**
	 * Initialisation.
	 * @throws IOException e
	 * @throws ServletException e
	 */
	@Before
	public void setUp() throws IOException, ServletException {
		tearDown();
		Utils.initialize();
		Utils.setProperty(Parameters.PARAMETER_SYSTEM_PREFIX + "mockLabradorRetriever", TRUE);
		Utils.setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, TRUE);
		final ServletConfig config = createNiceMock(ServletConfig.class);
		final ServletContext context = createNiceMock(ServletContext.class);
		expect(config.getServletContext()).andReturn(context).anyTimes();
		collectorServlet = new CollectorServlet();
		InputStream webXmlStream = null;
		try {
			webXmlStream = getClass().getResourceAsStream("/WEB-INF/web.xml");
			InputStream webXmlStream2 = null;
			try {
				webXmlStream2 = context.getResourceAsStream("/WEB-INF/web.xml");
				expect(webXmlStream2).andReturn(webXmlStream).anyTimes();
				final String javamelodyDir = "/META-INF/maven/net.bull.javamelody/";
				final String webapp = javamelodyDir + "javamelody-test-webapp/";
				expect(context.getResourcePaths("/META-INF/maven/"))
						.andReturn(Collections.singleton(javamelodyDir)).anyTimes();
				expect(context.getResourcePaths(javamelodyDir))
						.andReturn(Collections.singleton(webapp)).anyTimes();
				expect(context.getResourceAsStream(webapp + "pom.xml"))
						.andReturn(getClass().getResourceAsStream("/pom.xml")).anyTimes();
				replay(config);
				replay(context);
				collectorServlet.init(config);
				verify(config);
				verify(context);
			} finally {
				if (webXmlStream2 != null) {
					webXmlStream2.close();
				}
			}
		} finally {
			if (webXmlStream != null) {
				webXmlStream.close();
			}
		}
	}

	/**
	 * Terminaison.
	 * @throws IOException e
	 */
	@After
	public void tearDown() throws IOException {
		if (collectorServlet != null) {
			collectorServlet.destroy();
		}
		Parameters.removeCollectorApplication(TEST);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoPart() throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		// partParameter null: monitoring principal
		parameters.put(PART_PARAMETER, null);
		doPart(parameters);
		parameters.put(FORMAT_PARAMETER, "pdf");
		doPart(parameters);
		parameters.remove(FORMAT_PARAMETER);
		parameters.put(PART_PARAMETER, WEB_XML_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, POM_XML_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, JNDI_PART);
		doPart(parameters);
		parameters.put(PATH_PARAMETER, "/");
		doPart(parameters);
		parameters.remove(PATH_PARAMETER);
		parameters.put(PART_PARAMETER, MBEANS_PART);
		doPart(parameters);
		parameters.remove(PART_PARAMETER);
		parameters.put(JMX_VALUE, "JMImplementation:type=MBeanServerDelegate.MBeanServerId");
		doPart(parameters);
		parameters.remove(JMX_VALUE);
		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		doPart(parameters);
		parameters.put(FORMAT_PARAMETER, "pdf");
		doPart(parameters);
		parameters.remove(FORMAT_PARAMETER);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		doPart(parameters);
		parameters.put(REQUEST_PARAMETER, "0");
		doPart(parameters);
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		doPart(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoCompressedSerializable() throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, "xml");
		// partParameter null: monitoring principal
		parameters.put(PART_PARAMETER, null);
		doPart(parameters);
		parameters.put(PART_PARAMETER, APPLICATIONS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, COUNTER_SUMMARY_PER_CLASS_PART);
		parameters.put(COUNTER_PARAMETER, "services");
		doPart(parameters);
		parameters.remove(COUNTER_PARAMETER);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, THREADS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		doPart(parameters);
		parameters.put(WIDTH_PARAMETER, "80");
		parameters.put(HEIGHT_PARAMETER, "80");
		parameters.put(PART_PARAMETER, JROBINS_PART);
		doPart(parameters);
		parameters.put(GRAPH_PARAMETER, "cpu");
		parameters.put(PART_PARAMETER, JROBINS_PART);
		doPart(parameters);
		parameters.remove(GRAPH_PARAMETER);
		parameters.put(PART_PARAMETER, OTHER_JROBINS_PART);
		doPart(parameters);
		parameters.remove(WIDTH_PARAMETER);
		parameters.remove(HEIGHT_PARAMETER);
		parameters.put(PART_PARAMETER, EXPLAIN_PLAN_PART);
		parameters.put(REQUEST_PARAMETER, "select 1 from dual");
		doPart(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoCompressedSerializableForSystemActions()
			throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put(FORMAT_PARAMETER, "xml");
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, JNDI_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, MBEANS_PART);
		doPart(parameters);
		TestDatabaseInformations.initJdbcDriverParameters();
		parameters.put(PART_PARAMETER, DATABASE_PART);
		doPart(parameters);
		parameters.put(REQUEST_PARAMETER, "0");
		doPart(parameters);
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, JVM_PART);
		doPart(parameters);
		parameters.put(PART_PARAMETER, HOTSPOTS_PART);
		doPart(parameters);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testAction() throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("application", TEST);
		parameters.put(ACTION_PARAMETER, Action.GC.toString());
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.CLEAR_COUNTER.toString());
		parameters.put(COUNTER_PARAMETER, "all");
		doPart(parameters);
		parameters.put(FORMAT_PARAMETER, TransportFormat.SERIALIZED.getCode());
		doPart(parameters);
		parameters.remove(FORMAT_PARAMETER);
		parameters.put(ACTION_PARAMETER, Action.MAIL_TEST.toString());
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.PURGE_OBSOLETE_FILES.toString());
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.INVALIDATE_SESSION.toString());
		parameters.put(SESSION_ID_PARAMETER, "aSessionId");
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.KILL_THREAD.toString());
		parameters.put(THREAD_ID_PARAMETER, "aThreadId");
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.PAUSE_JOB.toString());
		parameters.put(JOB_ID_PARAMETER, "all");
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, Action.CLEAR_CACHE.toString());
		parameters.put(CACHE_ID_PARAMETER, "aCacheId");
		doPart(parameters);
		parameters.put(ACTION_PARAMETER, "remove_application");
		doPart(parameters);
	}

	private void doPart(Map<String, String> parameters) throws IOException, ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRequestURI()).andReturn("/test/monitoring").anyTimes();
		final ServletContext servletContext = createNiceMock(ServletContext.class);
		expect(servletContext.getServerInfo()).andReturn("Mock").anyTimes();
		if (MBEANS_PART.equals(parameters.get(PART_PARAMETER))) {
			expect(request.getHeaders("Accept-Encoding"))
					.andReturn(Collections.enumeration(Collections.singleton("application/gzip")))
					.anyTimes();
		} else {
			expect(request.getHeaders("Accept-Encoding"))
					.andReturn(Collections.enumeration(Collections.singleton("text/html")))
					.anyTimes();
		}
		Parameters.removeCollectorApplication(TEST);
		expect(request.getParameter("appName")).andReturn(TEST).anyTimes();
		expect(request.getParameter("appUrls"))
				.andReturn("http://localhost/test,http://localhost:8080/test2").anyTimes();
		// un cookie d'une application (qui existe)
		final Cookie[] cookies = { new Cookie("javamelody.application", TEST) };
		expect(request.getCookies()).andReturn(cookies).anyTimes();
		for (final Map.Entry<String, String> entry : parameters.entrySet()) {
			expect(request.getParameter(entry.getKey())).andReturn(entry.getValue()).anyTimes();
		}
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final FilterServletOutputStream servletOutputStream = new FilterServletOutputStream(
				new ByteArrayOutputStream());
		expect(response.getOutputStream()).andReturn(servletOutputStream).anyTimes();
		replay(request);
		replay(response);
		replay(servletContext);
		Parameters.initialize(servletContext);
		collectorServlet.doPost(request, response);
		collectorServlet.doGet(request, response);
		verify(request);
		verify(response);
		verify(servletContext);
	}
}
