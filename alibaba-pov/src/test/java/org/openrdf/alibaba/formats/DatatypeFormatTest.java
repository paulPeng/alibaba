package org.openrdf.alibaba.formats;

import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class DatatypeFormatTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private Repository repository;

	private ElmoManager manager;

	private GregorianCalendar cal;

	public void testnoneFormat() throws Exception {
		assertEquals("Ba ba", format("none", "Ba ba"));
	}

	public void testcapitalizeFormat() throws Exception {
		assertEquals("Ba Ba", format("capitalize", "Ba ba"));
	}

	public void testuppercaseFormat() throws Exception {
		assertEquals("BA BA", format("uppercase", "Ba ba"));
	}

	public void testlowercaseFormat() throws Exception {
		assertEquals("ba ba", format("lowercase", "Ba ba"));
	}

	public void testhiddenFormat() throws Exception {
		assertEquals("", format("hidden", "Ba ba"));
	}

	public void testsecondFirstFormat() throws Exception {
		assertEquals("Ba ba, black sheep", format("second-first", new Object[] {
				"black sheep", "Ba ba" }));
	}

	public void testsecretFormat() throws Exception {
		assertEquals("********", format("secret", "Ba ba"));
	}

	public void testshortDateTimeFormat() throws Exception {
		assertEquals("1/1/70 9:36 PM", format("short-dateTime", cal));
	}

	public void testmediumDateTimeFormat() throws Exception {
		assertEquals("Jan 1, 1970 9:36:00 PM", format("medium-dateTime", cal));
	}

	public void testlongDateTimeFormat() throws Exception {
		assertTrue(format("long-dateTime", cal).startsWith(
				"January 1, 1970 9:36:00 PM "));
	}

	public void testfullDateTimeFormat() throws Exception {
		assertTrue(format("full-dateTime", cal).startsWith(
				"Thursday, January 1, 1970 9:36:00 PM "));
	}

	public void testshortDateFormat() throws Exception {
		assertEquals("1/1/70", format("short-date", cal));
	}

	public void testmediumDateFormat() throws Exception {
		assertEquals("Jan 1, 1970", format("medium-date", cal));
	}

	public void testlongDateFormat() throws Exception {
		assertEquals("January 1, 1970", format("long-date", cal));
	}

	public void testfullDateFormat() throws Exception {
		assertEquals("Thursday, January 1, 1970", format("full-date", cal));
	}

	public void testshortTimeFormat() throws Exception {
		assertEquals("9:36 PM", format("short-time", cal));
	}

	public void testmediumTimeFormat() throws Exception {
		assertEquals("9:36:00 PM", format("medium-time", cal));
	}

	public void testlongTimeFormat() throws Exception {
		assertTrue(format("long-time", cal).startsWith("9:36:00 PM "));
	}

	public void testfullTimeFormat() throws Exception {
		assertTrue(format("full-time", cal).startsWith("9:36:00 PM "));
	}

	public void testdecimalFormat() throws Exception {
		assertEquals("10,230.65", format("decimal", new Double(10230.65)));
	}

	public void testintegerFormat() throws Exception {
		assertEquals("10,230", format("integer", new Integer(10230)));
	}

	public void testscientificFormat() throws Exception {
		assertEquals("1.02306E4", format("scientific", new Double(10230.647)));
	}

	public void testpercentFormat() throws Exception {
		assertEquals("75%", format("percent", new Double(0.752)));
	}

	public void testcurrencyFormat() throws Exception {
		assertEquals("$10,230.65", format("currency", new Double(10230.647)));

	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL list = cl.getResource(POVS_PROPERTIES);
		Properties prop = new Properties();
		prop.load(list.openStream());
		for (Object res : prop.keySet()) {
			URL url = cl.getResource(res.toString());
			RDFFormat format = RDFFormat.forFileName(url.getFile());
			conn.add(url, "", format);
		}
		conn.close();
		ElmoManagerFactory factory = new SesameManagerFactory(repository);
		manager = factory.createElmoManager(Locale.US);
		cal = new GregorianCalendar(1970, Calendar.JANUARY, 1, 21, 36, 0);
		TimeZone est = TimeZone.getTimeZone("EST");
		TimeZone.setDefault(est);
		cal.setTimeZone(est);
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}

	private String format(String name, Object value) throws AlibabaException {
		Object bean = manager.find(new QName(ALI.NS, name));
		FormatBehaviour format = (FormatBehaviour) bean;
		return format.format(value);
	}
}
