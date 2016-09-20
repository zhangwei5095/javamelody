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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire pour JpaPersistence et JpaWrapper.
 * @author Emeric Vernat
 */
public class TestJpa {
	private static final String PERSON_NAME = "emeric";

	/**
	 * (Ré)initialisation.
	 */
	@Before
	@After
	public void reset() {
		JpaWrapper.getJpaCounter().clear();
	}

	/**
	 * Test EntityManger.find.
	 */
	@Test
	public void simpleFind() {
		try {
			Class.forName("org.apache.openjpa.persistence.PersistenceProviderImpl");
		} catch (final ClassNotFoundException e) {
			Logger.getRootLogger().info(e.toString());
			// si openjpa n'est pas disponible dans le classpath (test depuis Ant),
			// on ne peut pas exécuter ce test
			return;
		}
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-jse");
		assertNotNull("createEntityManagerFactory", emf);

		try {
			final EntityManager em = emf.createEntityManager();
			assertNotNull("createEntityManager", em);
			try {
				em.find(Person.class, 0L);
				assertCounter("find(Person)");
			} finally {
				em.close();
			}
		} finally {
			emf.close();
		}
	}

	/**
	 * Tests createNamedQuery, createNativeQuery et createQery.
	 */
	@Test
	public void createAllQuery() {
		try {
			Class.forName("org.apache.openjpa.persistence.PersistenceProviderImpl");
		} catch (final ClassNotFoundException e) {
			Logger.getRootLogger().info(e.toString());
			// si openjpa n'est pas disponible dans le classpath (test depuis Ant),
			// on ne peut pas exécuter ce test
			return;
		}
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-jse");

		try {
			// init
			final EntityManager emInit = emf.createEntityManager();
			try {
				final EntityTransaction transaction = emInit.getTransaction();
				transaction.begin();
				try {
					final Person p = new Person();
					p.setName(PERSON_NAME);

					emInit.persist(p);
					transaction.commit();
				} catch (final Exception e) {
					transaction.rollback();
				}
			} finally {
				emInit.close();
			}

			reset();

			// checks
			final EntityManager em = emf.createEntityManager();
			try {
				final Query namedQuery = em.createNamedQuery("Person.findByName");
				final String nameParameter = "name";
				namedQuery.setParameter(nameParameter, PERSON_NAME).getSingleResult();
				assertCounter("NamedQuery(Person.findByName)");

				final TypedQuery<Person> namedQuery2 = em.createNamedQuery("Person.findByName",
						Person.class);
				namedQuery2.setParameter(nameParameter, PERSON_NAME).getSingleResult();
				assertCounter("NamedQuery(Person.findByName, Person)");

				final Query nativeQuery = em
						.createNativeQuery("select * from Person where name = ?");
				nativeQuery.setParameter(1, PERSON_NAME).getSingleResult();
				assertCounter("NativeQuery(select * from Person where name = ?)");

				final Query nativeQuery2 = em
						.createNativeQuery("select * from Person where name = ?", Person.class);
				nativeQuery2.setParameter(1, PERSON_NAME).getSingleResult();
				assertCounter("NativeQuery(select * from Person where name = ?, Person)");

				final Query query = em.createQuery("select p from Person p where p.name = :name");
				query.setParameter(nameParameter, PERSON_NAME).getSingleResult();
				assertCounter("Query(select p from Person p where p.name = :name)");

				final TypedQuery<Person> query2 = em
						.createQuery("select p from Person p where p.name = :name", Person.class);
				query2.setParameter(nameParameter, PERSON_NAME).getSingleResult();
				assertCounter("Query(select p from Person p where p.name = :name, Person)");

				final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
				final CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery();
				final Root<Person> from = criteriaQuery.from(Person.class);
				criteriaQuery.select(from);
				final CriteriaQuery<Object> criteriaQuery2 = criteriaQuery
						.where(criteriaBuilder.equal(from.get(nameParameter), PERSON_NAME));
				em.createQuery(criteriaQuery2).getSingleResult();
				assertCounter("Query(SELECT p FROM Person p WHERE p.name = '" + PERSON_NAME + "')");
			} finally {
				em.close();
			}
		} finally {
			emf.close();
		}
	}

	private static void assertCounter(String method) {
		final Counter counter = JpaWrapper.getJpaCounter();
		assertEquals("getRequestsCount", 1, counter.getRequestsCount());
		assertEquals("requestName", method, counter.getRequests().get(0).getName());
		counter.clear();
	}
}
