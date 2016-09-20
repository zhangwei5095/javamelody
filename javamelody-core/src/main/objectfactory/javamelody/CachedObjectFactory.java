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
package javamelody;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * javax.naming.spi.ObjectFactory intended to be used to cache the result of a lookup to a JNDI Reference,<br/>
 * in particular to cache the instance of a DataSource in GlassFish v3,<br/>
 * and so to workaround having a different wrapper instance of the DataSource for each lookup.<br/>
 * <br/>
 * Example usage in GlassFish: <br/>
 * 1) Your JDBC Resource (DataSource) is defined in GlassFish as "jdbc/MyDataSource_uncached" <br/>
 * 2) Add https://github.com/javamelody/javamelody/releases/download/javamelody-core-1.49.0/javamelody-objectfactory.jar in lib of GlassFish <br/>
 * 3) asadmin create-custom-resource --restype javax.naming.spi.ObjectFactory --factoryclass javamelody.CachedObjectFactory --property jndi-ref=jdbc/MyDataSource_uncached jdbc/MyDataSource <br/>
 *    or see sample-resource.xml beside this file <br/>
 * 4) Define the target of the custom resource in GlassFish if needed <br/>
 * 5) Webapp uses "jdbc/MyDataSource" to lookup from JNDI (which is the custom resource referencing "jdbc/MyDataSource_uncached")<br/>
 * @author bhun.kho
 * @author Emeric Vernat
 */
public class CachedObjectFactory implements ObjectFactory {

	private static final Map<String, Object> CACHED_OBJECTS = new HashMap<String, Object>();

	/** {@inheritDoc} */
	@Override
	// CHECKSTYLE:OFF
	public synchronized Object getObjectInstance(Object obj, Name name, Context nameCtx, // NOPMD
			Hashtable<?, ?> environment) throws NamingException { // NOPMD
		// CHECKSTYLE:ON
		final Reference reference = (Reference) obj;
		final RefAddr jndiRefAddr = reference.get("jndi-ref");
		if (jndiRefAddr == null) {
			throw new NamingException("You must specify a 'jndi-ref' in the <Resource> tag");
		}
		final String jndiRef = (String) jndiRefAddr.getContent();
		Object cachedObject = CACHED_OBJECTS.get(jndiRef);
		if (cachedObject == null) {
			final InitialContext context = new InitialContext();
			cachedObject = context.lookup(jndiRef);
			if (cachedObject == null) {
				throw new NamingException("No jndi object found for the 'jndi-ref': " + jndiRef);
			}
			CACHED_OBJECTS.put(jndiRef, cachedObject);
		}
		return cachedObject;
	}
}
