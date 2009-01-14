/*
 * Copyright (c) 2007, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.elmo.sesame;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.sesame.converters.Marshall;
import org.openrdf.elmo.sesame.converters.impl.BigDecimalMarshall;
import org.openrdf.elmo.sesame.converters.impl.BigIntegerMarshall;
import org.openrdf.elmo.sesame.converters.impl.BooleanMarshall;
import org.openrdf.elmo.sesame.converters.impl.ByteMarshall;
import org.openrdf.elmo.sesame.converters.impl.CharacterMarshall;
import org.openrdf.elmo.sesame.converters.impl.ClassMarshall;
import org.openrdf.elmo.sesame.converters.impl.DateMarshall;
import org.openrdf.elmo.sesame.converters.impl.DoubleMarshall;
import org.openrdf.elmo.sesame.converters.impl.DurationMarshall;
import org.openrdf.elmo.sesame.converters.impl.FloatMarshall;
import org.openrdf.elmo.sesame.converters.impl.GregorianCalendarMarshall;
import org.openrdf.elmo.sesame.converters.impl.IntegerMarshall;
import org.openrdf.elmo.sesame.converters.impl.LocaleMarshall;
import org.openrdf.elmo.sesame.converters.impl.LongMarshall;
import org.openrdf.elmo.sesame.converters.impl.ObjectConstructorMarshall;
import org.openrdf.elmo.sesame.converters.impl.ObjectSerializationMarshall;
import org.openrdf.elmo.sesame.converters.impl.PatternMarshall;
import org.openrdf.elmo.sesame.converters.impl.QNameMarshall;
import org.openrdf.elmo.sesame.converters.impl.ShortMarshall;
import org.openrdf.elmo.sesame.converters.impl.SqlDateMarshall;
import org.openrdf.elmo.sesame.converters.impl.SqlTimeMarshall;
import org.openrdf.elmo.sesame.converters.impl.SqlTimestampMarshall;
import org.openrdf.elmo.sesame.converters.impl.StringMarshall;
import org.openrdf.elmo.sesame.converters.impl.ValueOfMarshall;
import org.openrdf.elmo.sesame.converters.impl.XMLGregorianCalendarMarshall;
import org.openrdf.model.Literal;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.exceptions.ElmoConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between simple Java Objects and Strings.
 * 
 * @author James Leigh
 * 
 */
public class SesameLiteralManager implements LiteralManager<URI, Literal> {

	private static final String JAVA_NS = "java:";

	private static final String LITERALS_PROPERTIES = "META-INF/org.openrdf.elmo.literals";

	private static final String DATATYPES_PROPERTIES = "META-INF/org.openrdf.elmo.datatypes";

	private final Logger logger = LoggerFactory.getLogger(SesameLiteralManager.class);

	private ClassLoader cl;

	private URIFactory uf;

	private LiteralFactory lf;

	private ConcurrentMap<URI, Class<?>> javaClasses;

	private ConcurrentMap<String, Marshall<?>> marshalls;

	private ConcurrentMap<Class<?>, URI> rdfTypes;

	public SesameLiteralManager(URIFactory uf, LiteralFactory lf) {
		this.uf = uf;
		this.lf = lf;
		javaClasses = new ConcurrentHashMap<URI, Class<?>>();
		rdfTypes = new ConcurrentHashMap<Class<?>, URI>();
		marshalls = new ConcurrentHashMap<String, Marshall<?>>();
	}

	public void init() {
		if (cl == null)
			setClassLoader(Thread.currentThread().getContextClassLoader());
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
		try {
			recordMarshall(new BigDecimalMarshall(lf));
			recordMarshall(new BigIntegerMarshall(lf));
			recordMarshall(new BooleanMarshall(lf));
			recordMarshall(new ByteMarshall(lf));
			recordMarshall(new DoubleMarshall(lf));
			recordMarshall(new FloatMarshall(lf));
			recordMarshall(new IntegerMarshall(lf));
			recordMarshall(new LongMarshall(lf));
			recordMarshall(new ShortMarshall(lf));
			recordMarshall(new CharacterMarshall(lf));
			recordMarshall(new DateMarshall(lf));
			recordMarshall(new LocaleMarshall(lf));
			recordMarshall(new PatternMarshall(lf));
			recordMarshall(new QNameMarshall(lf));
			recordMarshall(new GregorianCalendarMarshall(lf));
			recordMarshall(new SqlDateMarshall(lf));
			recordMarshall(new SqlTimeMarshall(lf));
			recordMarshall(new SqlTimestampMarshall(lf));
			recordMarshall(new ClassMarshall(lf, cl));
			DurationMarshall dm = new DurationMarshall(lf);
			recordMarshall(dm.getJavaClassName(), dm);
			recordMarshall(Duration.class, dm);
			XMLGregorianCalendarMarshall xgcm;
			xgcm = new XMLGregorianCalendarMarshall(lf);
			recordMarshall(xgcm.getJavaClassName(), xgcm);
			recordMarshall(XMLGregorianCalendar.class, xgcm);
			recordMarshall(new StringMarshall(lf, "org.codehaus.groovy.runtime.GStringImpl"));
			recordMarshall(new StringMarshall(lf, "groovy.lang.GString$1"));
			recordMarshall(new StringMarshall(lf, "groovy.lang.GString$2"));
			loadDatatypes(SesameLiteralManager.class.getClassLoader(), DATATYPES_PROPERTIES);
			loadDatatypes(cl, DATATYPES_PROPERTIES);
			loadDatatypes(cl, LITERALS_PROPERTIES);
		} catch (Exception e) {
			throw new ElmoConversionException(e);
		}
	}

	public Class<?> getClass(URI datatype) {
		if (javaClasses.containsKey(datatype))
			return javaClasses.get(datatype);
		try {
			if (datatype.getNamespace().equals(JAVA_NS))
				return Class.forName(datatype.getLocalName(), true, cl);
		} catch (ClassNotFoundException e) {
			throw new ElmoConversionException(e);
		}
		throw new ElmoConversionException("Unknown datatype: " + datatype);
	}

	public URI getDatatype(Class<?> type) {
		if (type.equals(String.class))
			return null;
		if (rdfTypes.containsKey(type))
			return rdfTypes.get(type);
		URI datatype = uf.createURI(JAVA_NS, type.getName());
		recordType(type, datatype);
		return datatype;
	}

	@SuppressWarnings("unchecked")
	public Literal getLiteral(Object object) {
		if (object instanceof String)
			return lf.createLiteral((String) object);
		Marshall marshall = findMarshall(object.getClass());
		return marshall.serialize(object);
	}

	public Literal getLiteral(String value, String language) {
		return lf.createLiteral(value, language);
	}

	@SuppressWarnings("unchecked")
	public Object getObject(Literal literal) {
		URI datatype = literal.getDatatype();
		if (datatype == null)
			return literal.getLabel();
		Marshall marshall = findMarshall(datatype);
		return marshall.deserialize(literal);
	}

	public void recordMarshall(String javaClassName, Marshall<?> marshall) {
		marshalls.put(javaClassName, marshall);
	}

	public void recordMarshall(Class<?> javaClass, Marshall<?> marshall) {
		recordMarshall(javaClass.getName(), marshall);
	}

	public void recordType(Class<?> javaClass, String datatype) {
		recordType(javaClass, uf.createURI(datatype));
	}

	public boolean isTypeOfLiteral(Class<?> type) {
		return rdfTypes.containsKey(type);
	}

	private void recordType(Class<?> javaClass, URI datatype) {
		if (!javaClasses.containsKey(datatype)) {
			javaClasses.putIfAbsent(datatype, javaClass);
		}
		if (rdfTypes.putIfAbsent(javaClass, datatype) == null) {
			Marshall<?> marshall = findMarshall(javaClass);
			marshall.setDatatype(datatype);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Marshall<T> findMarshall(Class<T> type) {
		String name = type.getName();
		if (marshalls.containsKey(name))
			return (Marshall<T>) marshalls.get(name);
		Marshall<T> marshall;
		try {
			marshall = new ValueOfMarshall<T>(lf, type);
		} catch (NoSuchMethodException e1) {
			try {
				marshall = new ObjectConstructorMarshall<T>(lf, type);
			} catch (NoSuchMethodException e2) {
				if (Serializable.class.isAssignableFrom(type)) {
					marshall = new ObjectSerializationMarshall<T>(lf, type);
				} else {
					throw new ElmoConversionException(e1);
				}
			}
		}
		Marshall<?> o = marshalls.putIfAbsent(name, marshall);
		if (o != null) {
			marshall = (Marshall<T>) o;
		}
		return marshall;
	}

	private Marshall<?> findMarshall(URI datatype) {
		Class<?> type;
		if (javaClasses.containsKey(datatype)) {
			type = javaClasses.get(datatype);
		} else if (datatype.getNamespace().equals(JAVA_NS)) {
			try {
				type = Class.forName(datatype.getLocalName(), true, cl);
			} catch (ClassNotFoundException e) {
				throw new ElmoConversionException(e);
			}
		} else {
			throw new ElmoConversionException("Unknown datatype: " + datatype);
		}
		return findMarshall(type);
	}

	private void loadDatatypes(ClassLoader cl, String properties) throws IOException,
			ClassNotFoundException {
		if (cl == null)
			return;
		Enumeration<URL> resources = cl.getResources(properties);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			try {
				Properties p = new Properties();
				p.load(url.openStream());
				for (Map.Entry<?, ?> e : p.entrySet()) {
					String className = (String) e.getKey();
					String types = (String) e.getValue();
					Class<?> lc = Class.forName(className, true, cl);
					boolean present = lc.isAnnotationPresent(rdf.class);
					for (String rdf : types.split("\\s+")) {
						if (rdf.length() == 0 && present) {
							rdf = lc.getAnnotation(rdf.class).value();
							recordType(lc, uf.createURI(rdf));
						} else if (rdf.length() == 0) {
							logger.warn("Unkown datatype mapping {}", className);
						} else {
							recordType(lc, uf.createURI(rdf));
						}
					}
				}
			} catch (IOException e) {
				String msg = e.getMessage() + " in: " + url;
				IOException ioe = new IOException(msg);
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	private void recordMarshall(Marshall<?> marshall) {
		recordMarshall(marshall.getJavaClassName(), marshall);
	}

}