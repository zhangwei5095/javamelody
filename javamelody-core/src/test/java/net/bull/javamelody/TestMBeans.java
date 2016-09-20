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

import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bull.javamelody.TestTomcatInformations.GlobalRequestProcessor;
import net.bull.javamelody.TestTomcatInformations.ThreadPool;

/**
 * Test unitaire de la classe MBeans.
 * @author Emeric Vernat
 */
public class TestMBeans {
	private MBeans mbeans;
	private MBeanServer mBeanServer;
	private final List<ObjectName> mbeansList = new ArrayList<ObjectName>();

	/** Before.
	 * @throws JMException e */
	@Before
	public void setUp() throws JMException {
		Utils.initialize();
		mbeans = new MBeans();
		mBeanServer = MBeans.getPlatformMBeanServer();
		final ObjectInstance mBean1 = mBeanServer.registerMBean(new ThreadPool(),
				new ObjectName("Catalina:type=ThreadPool"));
		mbeansList.add(mBean1.getObjectName());
		final ObjectInstance mBean2 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
				new ObjectName("Catalina:type=GlobalRequestProcessor,name=http-8080"));
		mbeansList.add(mBean2.getObjectName());
		final ObjectInstance mBean3 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
				new ObjectName("jonas:j2eeType=Servlet"));
		mbeansList.add(mBean3.getObjectName());
		final ObjectInstance mBean4 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
				new ObjectName("notjonas:type=Servlet"));
		mbeansList.add(mBean4.getObjectName());
		final ObjectInstance mBean5 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
				new ObjectName("jboss.deployment:type=Servlet"));
		mbeansList.add(mBean5.getObjectName());
	}

	/** After.
	 * @throws JMException e */
	@After
	public void tearDown() throws JMException {
		for (final ObjectName registeredMBean : mbeansList) {
			mBeanServer.unregisterMBean(registeredMBean);
		}
	}

	/** Test.
	 * @throws JMException e */
	@Test
	public void testGetTomcatThreadPools() throws JMException {
		assertNotNull("getTomcatThreadPools", mbeans.getTomcatThreadPools());
	}

	/** Test.
	 * @throws JMException e */
	@Test
	public void testGetTomcatGlobalRequestProcessors() throws JMException {
		assertNotNull("getTomcatGlobalRequestProcessors",
				mbeans.getTomcatGlobalRequestProcessors());
	}

	/** Test.
	 * @throws JMException e */
	@Test
	public void testGetAttribute() throws JMException {
		assertNotNull("getAttribute", mbeans.getAttribute(mbeansList.get(0), "currentThreadsBusy"));
	}

	/** Test.
	 * @throws JMException e */
	@Test
	public void testGetAllMBeanNodes() throws JMException {
		assertNotNull("getAllMBeanNodes", MBeans.getAllMBeanNodes());
	}

	/** Test. */
	@Test
	public void testGetConvertedAttribute() {
		final String firstMBean = mbeansList.get(0).toString();
		final String message = "getConvertedAttributes";
		assertNotNull(message, MBeans.getConvertedAttributes(firstMBean + ".maxThreads"));
		assertNotNull(message, MBeans
				.getConvertedAttributes(firstMBean + ".maxThreads|" + firstMBean + ".maxThreads"));
		assertNotNull(message, MBeans.getConvertedAttributes(firstMBean + ".intArrayAsInJRockit"));
		assertNotNull(message,
				MBeans.getConvertedAttributes(firstMBean + ".doubleArrayAsInJRockit"));
		try {
			MBeans.getConvertedAttributes("Catalina:type=instanceNotFound.maxThreads");
		} catch (final IllegalArgumentException e) {
			assertNotNull("e", e);
		}
		try {
			MBeans.getConvertedAttributes("n'importe quoi.maxThreads");
		} catch (final IllegalArgumentException e) {
			assertNotNull("e", e);
		}
		try {
			MBeans.getConvertedAttributes(firstMBean + ".Password");
		} catch (final IllegalArgumentException e) {
			assertNotNull("e", e);
		}
		try {
			MBeans.getConvertedAttributes("noAttribute");
		} catch (final IllegalArgumentException e) {
			assertNotNull("e", e);
		}
	}
}
