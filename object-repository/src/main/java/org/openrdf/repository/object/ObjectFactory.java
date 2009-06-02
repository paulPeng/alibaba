/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object;

import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.helpers.ObjectQueryFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;

/**
 * Converts between {@link Value} and objects without accessing the repository.
 * 
 * @author James Leigh
 * 
 */
public class ObjectFactory {

	private LiteralManager lm;

	private ClassResolver resolver;

	private RoleMapper mapper;

	private PropertyMapper properties;

	private ClassLoader cl;

	private ObjectConnection connection;

	private Map<Class<?>, ObjectQueryFactory> factories;

	public ObjectFactory(RoleMapper mapper, PropertyMapper properties,
			LiteralManager lm, ClassResolver resolver, ClassLoader cl) {
		assert lm != null;
		assert mapper != null;
		assert properties != null;
		assert resolver != null;
		this.lm = lm;
		this.mapper = mapper;
		this.properties = properties;
		this.resolver = resolver;
		this.cl = cl;
	}

	/**
	 * @return The ClassLoader used by this ObjectFactory.
	 */
	public ClassLoader getClassLoader() {
		return cl;
	}

	/**
	 * Converts a literal into an object.
	 */
	public Object createObject(Literal literal) {
		return lm.createObject(literal);
	}

	/**
	 * Converts an object back into a literal.
	 */
	public Literal createLiteral(Object object) {
		return lm.createLiteral(object);
	}

	/**
	 * Converts an object into a literal or resource.
	 */
	public Value createValue(Object object) {
		if (object instanceof RDFObject) {
			return ((RDFObject) object).getResource();
		} else {
			return lm.createLiteral(object);
		}
	}

	/**
	 * Creates an anonymous object with no rdf:type.
	 */
	public RDFObject createObject() {
		BNode node = connection.getValueFactory().createBNode();
		return createBean(node, resolver.resolveBlankEntity());

	}

	/**
	 * Creates an object with no rdf:type.
	 */
	public RDFObject createObject(String uri) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri));
	}

	/**
	 * Creates an object with no rdf:type.
	 */
	public RDFObject createObject(Resource resource) {
		if (resource instanceof URI)
			return createBean(resource, resolver.resolveEntity((URI) resource));
		return createBean(resource, resolver.resolveBlankEntity());
	}

	/**
	 * Creates an object with an assumed rdf:type.
	 */
	public <T> T createObject(Resource resource, Class<T> type) {
		Set<URI> types = Collections.singleton(getType(type));
		return type.cast(createObject(resource, types));
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(Resource resource, URI... types) {
		assert types != null && types.length > 0;
		List<URI> list = Arrays.asList(types);
		return createObject(resource, list);
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(Resource resource, Collection<URI> types) {
		Class<?> proxy;
		if (resource instanceof URI) {
			if (types.isEmpty()) {
				proxy = resolver.resolveEntity((URI) resource);
			} else {
				proxy = resolver.resolveEntity((URI) resource, types);
			}
		} else {
			if (types.isEmpty()) {
				proxy = resolver.resolveBlankEntity();
			} else {
				proxy = resolver.resolveBlankEntity(types);
			}
		}
		return createBean(resource, proxy);
	}

	/**
	 * @return <code>true</code> If the given type can be used as a concept
	 *         parameter.
	 */
	public boolean isNamedConcept(Class<?> type) {
		return mapper.findType(type) != null;
	}

	protected boolean isDatatype(Class<?> type) {
		return lm.isDatatype(type);
	}

	protected URI getType(Class<?> concept) {
		return mapper.findType(concept);
	}

	protected String createObjectQuery(Class<?> concept, int bindings) {
		Map<String, String> subjectProperties = properties
				.findEagerProperties(concept);
		if (subjectProperties == null) {
			subjectProperties = singletonMap("class", RDF.TYPE.stringValue());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT REDUCED ?_");
		for (String name : subjectProperties.keySet()) {
			sb.append(" ?__").append(name);
		}
		sb.append("\nWHERE { ");
		URI uri = getType(concept);
		boolean typed = uri != null && bindings == 0;
		if (typed) {
			Collection<URI> types = new HashSet<URI>();
			mapper.findSubTypes(concept, types);
			Iterator<URI> iter = types.iterator();
			assert iter.hasNext();
			while (iter.hasNext()) {
				sb.append("\n{ ?_ a <");
				sb.append(iter.next().stringValue()).append(">}");
				if (iter.hasNext()) {
					sb.append(" UNION ");
				}
			}
		} else {
			sb.append("\n?_ a ?__class .");
		}
		for (String name : subjectProperties.keySet()) {
			if (!typed && "class".equals(name))
				continue;
			String pred = subjectProperties.get(name);
			sb.append("\nOPTIONAL {").append(" ?_ <");
			sb.append(pred);
			sb.append("> ?__").append(name).append(" } ");
		}
		if (bindings > 0) {
			sb.append("\nFILTER (");
			for (int i = 0; i < bindings; i++) {
				sb.append(" ?_ = $_").append(i).append(" ||");
			}
			sb.delete(sb.length() - 2, sb.length());
			sb.append(")");
		}
		sb.append(" } ");
		if (bindings > 1) {
			sb.append("\nORDER BY ?_");
		}
		return sb.toString();
	}

	protected void setObjectConnection(ObjectConnection connection) {
		this.connection = connection;
		factories = new HashMap<Class<?>, ObjectQueryFactory>();
	}

	private RDFObject createBean(Resource resource, Class<?> proxy) {
		try {
			ObjectQueryFactory factory = createObjectQueryFactory(proxy);
			Object obj = proxy.newInstance();
			ManagedRDFObject bean = (ManagedRDFObject) obj;
			bean.initRDFObject(resource, factory, connection);
			return (RDFObject) obj;
		} catch (InstantiationException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectCompositionException(e);
		}
	}

	private ObjectQueryFactory createObjectQueryFactory(Class<?> proxy) {
		if (factories == null)
			return null;
		synchronized (factories) {
			ObjectQueryFactory factory = factories.get(proxy);
			if (factory == null) {
				factory = new ObjectQueryFactory(connection, properties);
				factories.put(proxy, factory);
			}
			return factory;
		}
	}
}
