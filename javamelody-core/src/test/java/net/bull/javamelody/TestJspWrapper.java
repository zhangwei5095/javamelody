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
package net.bull.javamelody;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire de la classe JspWrapper.
 * @author Emeric Vernat
 */
public class TestJspWrapper {
	/**
	 * Initialisation.
	 */
	@Before
	public void setUp() {
		Utils.initialize();
	}

	/**
	 * Test.
	 * @throws ServletException e
	 * @throws IOException e
	 */
	@Test
	public void testJspWrapper() throws ServletException, IOException {
		assertNotNull("getJspCounter", JspWrapper.getJspCounter());

		final ServletContext servletContext = createNiceMock(ServletContext.class);
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final RequestDispatcher requestDispatcher = createNiceMock(RequestDispatcher.class);
		final RequestDispatcher requestDispatcherWithError = createNiceMock(
				RequestDispatcher.class);
		final RequestDispatcher requestDispatcherWithException = createNiceMock(
				RequestDispatcher.class);
		final String url1 = "test.jsp";
		final String url2 = "test.jsp?param=test2";
		final String url3 = "test.jsp?param=test3";
		final String url4 = null;
		expect(request.getRequestDispatcher(url1)).andReturn(requestDispatcher);
		expect(request.getRequestDispatcher(url2)).andReturn(requestDispatcherWithError);
		requestDispatcherWithError.forward(request, response);
		expectLastCall().andThrow(new UnknownError("erreur dans forward"));
		expect(request.getRequestDispatcher(url3)).andReturn(requestDispatcherWithException);
		requestDispatcherWithException.forward(request, response);
		expectLastCall().andThrow(new IllegalStateException("erreur dans forward"));
		expect(request.getRequestDispatcher(url4)).andReturn(null);

		replay(request);
		replay(response);
		replay(requestDispatcher);
		replay(requestDispatcherWithError);
		replay(requestDispatcherWithException);
		replay(servletContext);
		Parameters.initialize(servletContext);
		final HttpServletRequest wrappedRequest = JspWrapper.createHttpRequestWrapper(request,
				response);
		final RequestDispatcher wrappedRequestDispatcher = wrappedRequest
				.getRequestDispatcher(url1);
		wrappedRequestDispatcher.toString();
		wrappedRequestDispatcher.include(wrappedRequest, response);
		final RequestDispatcher wrappedRequestDispatcher2 = wrappedRequest
				.getRequestDispatcher(url2);
		try {
			wrappedRequestDispatcher2.forward(request, response);
		} catch (final UnknownError e) {
			assertNotNull("ok", e);
		}
		final RequestDispatcher wrappedRequestDispatcher3 = wrappedRequest
				.getRequestDispatcher(url3);
		try {
			wrappedRequestDispatcher3.forward(request, response);
		} catch (final IllegalStateException e) {
			assertNotNull("ok", e);
		}
		final RequestDispatcher wrappedRequestDispatcher4 = wrappedRequest
				.getRequestDispatcher(url4);
		assertNull("getRequestDispatcher(null)", wrappedRequestDispatcher4);
		verify(request);
		verify(response);
		verify(requestDispatcher);
		// verify ne marche pas ici car on fait une Error, verify(requestDispatcherWithError);
		verify(requestDispatcherWithException);
		verify(servletContext);
	}
}
