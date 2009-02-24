package org.openrdf.repository.object;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Test;

import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.RepositoryTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;

public class SesameQueryTest extends RepositoryTestCase {
	private static final int OFFSET = TimeZone.getDefault().getOffset(new Date(2007, Calendar.NOVEMBER, 6).getTime()) / 1000 / 60;
	private static final String SELECT_BY_DATE = "SELECT ?c WHERE { ?c <http://example.org/rdf/date> ?d . FILTER (?d <= ?date) }";
	private static final String NS = "http://example.org/rdf/";

	@rdf(NS + "Concept")
	public interface Concept {
		@rdf(NS + "date")
		XMLGregorianCalendar getDate();
		void setDate(XMLGregorianCalendar xcal);
	}

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(SesameQueryTest.class);
	}

	private ObjectConnection manager;
	private DatatypeFactory data;
	private ObjectRepositoryConfig module;

	public void testXmlCalendarZ() throws Exception {
		XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
		xcal.setYear(2007);
		xcal.setMonth(11);
		xcal.setDay(6);
		xcal.setTimezone(OFFSET);
		ObjectQuery query = manager.prepareObjectQuery(SELECT_BY_DATE);
		query.setParameter("date", xcal);
		List list = query.evaluate().asList();
		assertEquals(7, list.size());
	}

	public void testXmlCalendar() throws Exception {
		XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
		xcal.setYear(2000);
		xcal.setMonth(11);
		xcal.setDay(6);
		ObjectQuery query = manager.prepareObjectQuery(SELECT_BY_DATE);
		query.setParameter("date", xcal);
		List list = query.evaluate().asList();
		assertEquals(3, list.size());
	}

	@Override
	protected void setUp() throws Exception {
		module = new ObjectRepositoryConfig().addConcept(Concept.class);
		super.setUp();
		ObjectRepository factory = (ObjectRepository) repository;
		manager = factory.getConnection();
		data = DatatypeFactory.newInstance();
		for (int i=1;i<5;i++) {
			Class<?>[] concepts = {};
			Concept concept = manager.addType(manager.getObject(ValueFactoryImpl.getInstance().createURI(NS, "concept" + i)), Concept.class);
			XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2000);
			xcal.setMonth(11);
			xcal.setDay(i*2);
			concept.setDate(xcal);
			Class<?>[] concepts1 = {};
			concept = manager.addType(manager.getObject(ValueFactoryImpl.getInstance().createURI(NS, "conceptZ" + i)), Concept.class);
			xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2007);
			xcal.setMonth(11);
			xcal.setDay(i*2);
			xcal.setTimezone(OFFSET);
			concept.setDate(xcal);
		}
	}

	@Override
	protected Repository createRepository() throws Exception {
		return new ObjectRepositoryFactory().createRepository(module,super.createRepository());
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		super.tearDown();
	}

}
