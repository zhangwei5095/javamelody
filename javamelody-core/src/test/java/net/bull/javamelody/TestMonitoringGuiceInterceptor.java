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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import net.bull.javamelody.TestMonitoringSpringInterceptor.AnnotatedTest;
import net.bull.javamelody.TestMonitoringSpringInterceptor.AnnotatedTestClass;
import net.bull.javamelody.TestMonitoringSpringInterceptor.AnnotatedTestMethod;
import net.bull.javamelody.TestMonitoringSpringInterceptor.AnnotatedTestOtherClass;

/**
 * Test unitaire de la classe MonitoringGuiceInterceptor.
 * @author Emeric Vernat
 */
public class TestMonitoringGuiceInterceptor {
	private static final String REQUESTS_COUNT = "requestsCount";

	/** Check. */
	@Before
	public void setUp() {
		Utils.initialize();
	}

	/** Test. */
	@Test
	public void testNewInstance() {
		assertNotNull("new MonitoringGuiceInterceptor", new MonitoringGuiceInterceptor());
	}

	/** Test. */
	@Test
	public void testGetGuiceCounter() {
		assertNotNull("getGuiceCounter", MonitoringProxy.getGuiceCounter());
	}

	/** Test. */
	@Test
	public void testGuiceAOP() {
		final Counter guiceCounter = MonitoringProxy.getGuiceCounter();
		guiceCounter.clear();

		final Key<AnnotatedTest> annotatedTestMethodKey = Key.get(AnnotatedTest.class,
				Names.named("annotatedTestMethod"));
		final Key<AnnotatedTest> annotatedTestOtherClassKey = Key.get(AnnotatedTest.class,
				Names.named("annotatedTestOtherClass"));
		final Module testModule = new AbstractModule() {
			/** {@inheritDoc} */
			@Override
			protected void configure() {
				// configuration du monitoring Guice
				install(new MonitoringGuiceModule());
				// implémentation de test
				bind(SpringTestFacade.class).to(SpringTestFacadeImpl.class);
				bind(AnnotatedTest.class).to(AnnotatedTestClass.class);
				bind(annotatedTestOtherClassKey).to(AnnotatedTestOtherClass.class);
				bind(annotatedTestMethodKey).to(AnnotatedTestMethod.class);
			}
		};
		final Injector injector = Guice.createInjector(testModule);
		final SpringTestFacade springTestFacade = injector.getInstance(SpringTestFacade.class);

		guiceCounter.setDisplayed(false);
		assertNotNull("now()", springTestFacade.now());
		assertSame(REQUESTS_COUNT, 0, guiceCounter.getRequestsCount());

		guiceCounter.setDisplayed(true);
		assertNotNull("now()", springTestFacade.now());
		assertSame(REQUESTS_COUNT, 1, guiceCounter.getRequestsCount());

		try {
			springTestFacade.throwError();
		} catch (final Error e) {
			assertSame(REQUESTS_COUNT, 2, guiceCounter.getRequestsCount());
		}

		final AnnotatedTest annotatedTestClass = injector.getInstance(AnnotatedTestClass.class);
		assertNotNull("annotatedTestClass", annotatedTestClass.myMethod());
		assertSame(REQUESTS_COUNT, 3, guiceCounter.getRequestsCount());

		final AnnotatedTest annotatedTestOtherClass = injector
				.getInstance(annotatedTestOtherClassKey);
		assertNotNull("annotatedTestOtherClass", annotatedTestOtherClass.myMethod());
		assertSame(REQUESTS_COUNT, 4, guiceCounter.getRequestsCount());

		final AnnotatedTest annotatedTestMethod = injector.getInstance(annotatedTestMethodKey);
		assertNotNull("annotatedTestMethod", annotatedTestMethod.myMethod());
		assertNotNull("annotatedTestMethod", annotatedTestMethod.myOtherMethod());
		assertSame(REQUESTS_COUNT, 6, guiceCounter.getRequestsCount());
	}
}
