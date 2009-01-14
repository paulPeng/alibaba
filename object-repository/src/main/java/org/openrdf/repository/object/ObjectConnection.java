/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.elmo.EntitySupport;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.Mergeable;
import org.openrdf.elmo.Refreshable;
import org.openrdf.elmo.ResourceManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.sesame.iterators.ConvertingIterator;
import org.openrdf.elmo.sesame.roles.SesameManagerAware;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.exceptions.ElmoCompositionException;
import org.openrdf.repository.object.exceptions.ElmoIOException;
import org.openrdf.repository.object.exceptions.ElmoPersistException;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles operations of ElmoManager and EntityManager.
 * 
 * @author James Leigh
 * 
 */
public class ObjectConnection extends ContextAwareConnection {

	final Logger logger = LoggerFactory.getLogger(ObjectConnection.class);

	private String language;

	private ResourceManager<Resource> resources;

	private LiteralManager<URI, Literal> lm;

	private RoleMapper<URI> mapper;

	private Map<Object, Resource> merged = new IdentityHashMap<Object, Resource>();

	public ObjectConnection(ObjectRepository repository,
			RepositoryConnection connection) {
		super(repository, connection);
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String lang) {
		this.language = lang;
	}

	public ResourceManager<Resource> getResourceManager() {
		return resources;
	}

	public void setResourceManager(ResourceManager<Resource> manager) {
		this.resources = manager;
	}

	public LiteralManager<URI, Literal> getLiteralManager() {
		return lm;
	}

	public void setLiteralManager(LiteralManager<URI, Literal> manager) {
		this.lm = manager;
	}

	public RoleMapper<URI> getRoleMapper() {
		return mapper;
	}

	public void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	public void close(Iterator<?> iter) {
		if (iter instanceof Closeable) {
			try {
				((Closeable) iter).close();
			} catch (IOException e) {
				throw new ElmoIOException(e);
			}
		}
	}

	public Object find(Value value) {
		if (value instanceof Resource) {
			Resource resource = (Resource)value;
			RDFObject bean = createBean(resource, resources.getEntityClass(resource));
			if (logger.isDebugEnabled()) {
				try {
					if (!this.hasMatch(resource, null,
							null))
						logger.debug("Warning: Unknown entity: " + value);
				} catch (StoreException e) {
					throw new ElmoIOException(e);
				}
			}
			return bean;
		}
		return lm.getObject((Literal) value);
	}

	public Value valueOf(Object instance) {
		if (instance instanceof RDFObject)
			return ((RDFObject) instance).getResource();
		if (instance instanceof EntitySupport) {
			EntitySupport support = (EntitySupport) instance;
			RDFObject entity = support.getSupportedElmoEntity();
			if (entity instanceof RDFObject)
				return ((RDFObject) entity).getResource();
		}
		Class<?> type = instance.getClass();
		if (lm.isTypeOfLiteral(type))
			return lm.getLiteral(instance);
		synchronized (merged) {
			if (merged.containsKey(instance))
				return merged.get(instance);
		}
		if (RDFObject.class.isAssignableFrom(type) || isEntity(type))
			return valueOf(merge(instance));
		return lm.getLiteral(instance);
	}

	public boolean contains(Object entity) {
		if (entity instanceof RDFObject) {
			RDFObject se = (RDFObject) entity;
			return this.equals(se.getObjectConnection());
		} else if (entity instanceof EntitySupport) {
			EntitySupport es = (EntitySupport) entity;
			RDFObject e = es.getSupportedElmoEntity();
			if (e instanceof RDFObject) {
				RDFObject se = (RDFObject) e;
				return this.equals(se.getObjectConnection());
			}
		}
		return false;
	}

	public <T> T create(Class<T> concept, Class<?>... concepts) {
		Resource resource = getValueFactory().createBNode();
		Class<?> proxy = resources.persistRole(resource, concept, concepts);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	public <T> T create(Resource resource, Class<T> concept, Class<?>... concepts) {
		Class<?> proxy = resources.persistRole(resource, concept, concepts);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	public <T> T designate(Object entity, Class<T> concept, Class<?>... concepts) {
		Resource resource = getSesameResource(entity);
		Class<?>[] roles = combine(concept, concepts);
		Class<?> proxy = resources.persistRole(resource, entity.getClass(), roles);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	public Object removeDesignation(Object entity, Class<?>... concepts) {
		Resource resource = getSesameResource(entity);
		return createBean(resource, resources.removeRole(resource, concepts));
	}

	@SuppressWarnings("unchecked")
	public <T> T rename(T bean, Resource dest) {
		Resource before = getSesameResource(bean);
		resources.renameResource(before, dest);
		return (T) createBean(dest, resources.getEntityClass(dest));
	}

	public void refresh(Object entity) {
		if (entity instanceof Refreshable) {
			((Refreshable) entity).refresh();
		}
	}

	public <T> T merge(T bean) {
		if (bean == null) {
			return null;
		} else if (bean instanceof Set<?>) {
			// so we can merge both a List and a Set
			Set<?> old = (Set<?>) bean;
			Set<Object> set = new HashSet<Object>(old.size());
			for (Object o : old) {
				set.add(merge(o));
			}
			return (T) set;
		} else {
			Resource resource = assignResource(bean);
			Class<?> role = bean.getClass();
			Class<?> proxy;
			if (resource instanceof URI) {
				proxy = resources.mergeRole(resource, role);
			} else {
				proxy = resources.persistRole(resource, role);
			}
			RDFObject result = createBean(resource, proxy);
			assert result instanceof Mergeable;
			((Mergeable) result).merge(bean);
			return (T) result;
		}
	}

	public void persist(Object bean) {
		Resource resource = assignResource(bean);
		Class<?> role = bean.getClass();
		Class<?> proxy = resources.persistRole(resource, role);
		RDFObject result = createBean(resource, proxy);
		assert result instanceof Mergeable;
		((Mergeable) result).merge(bean);
	}

	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query,
			String baseURI) throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query, baseURI));
	}

	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query));
	}

	public ObjectQuery prepareObjectQuery(String query)
			throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(query));
	}

	public <T> Iterable<T> findAll(final Class<T> javaClass) {
		final ResourceManager<Resource> resources = this.resources;
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				Iterator<Resource> iter = resources.createRoleQuery(javaClass);
				return new ConvertingIterator<Resource, T>(iter) {
					@Override
					public T convert(Resource resource) {
						return (T) find(resource);
					}
				};
			}
		};
	}

	public void remove(Object entity) {
		Resource resource = getSesameResource(entity);
		resources.removeResource(resource);
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		for (Class<?> face : type.getInterfaces()) {
			if (mapper.findType(face) != null)
				return true;
		}
		if (mapper.findType(type) != null)
			return true;
		return isEntity(type.getSuperclass());
	}

	private boolean assertConceptsRecorded(RDFObject bean, Class<?>... concepts) {
		for (Class<?> concept : concepts) {
			assert !concept.isInterface()
					|| concept.isAssignableFrom(bean.getClass()) : "Concept has not bean recorded: "
					+ concept.getSimpleName();
		}
		return true;
	}

	private Resource assignResource(Object bean) {
		synchronized (merged) {
			if (merged.containsKey(bean))
				return merged.get(bean);
			Resource resource = findResource(bean);
			if (resource == null)
				resource = getValueFactory().createBNode();
			merged.put(bean, resource);
			return resource;
		}
	}

	private Resource getSesameResource(Object entity) {
		Resource resource = getResource(entity);
		if (resource == null)
			throw new ElmoPersistException("Unknown Entity: " + entity);
		return resource;
	}

	private Resource findResource(Object bean) {
		Resource resource = getResource(bean);
		if (resource != null)
			return resource;
		if (bean instanceof RDFObject) {
			Resource name = ((RDFObject) bean).getResource();
			if (name == null)
				return null;
			return name;
		} else {
			try {
				Method m = bean.getClass().getMethod("getURI");
				URI name = (URI) m.invoke(bean);
				if (name == null)
					return null;
				return name;
			} catch (Exception e) {
				return null;
			}
		}
	}

	private Resource getResource(Object bean) {
		if (bean instanceof RDFObject) {
			return ((RDFObject) bean).getResource();
		} else if (bean instanceof EntitySupport) {
			EntitySupport support = (EntitySupport) bean;
			RDFObject entity = support.getSupportedElmoEntity();
			if (entity instanceof RDFObject)
				return ((RDFObject) entity).getResource();
		}
		return null;
	}

	private RDFObject createBean(Resource resource, Class<?> type) {
		try {
			Object obj = type.newInstance();
			assert obj instanceof SesameManagerAware : "core roles are not registered, check your deployed classpath";
			SesameManagerAware bean = (SesameManagerAware) obj;
			bean.initObjectConnection(this, resource);
			return bean;
		} catch (InstantiationException e) {
			throw new ElmoCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ElmoCompositionException(e);
		}
	}

	private <T> Class<?>[] combine(Class<T> concept, Class<?>... concepts) {
		Class<?>[] roles;
		if (concepts == null || concepts.length == 0) {
			roles = new Class<?>[]{concept};
		} else {
			roles = new Class<?>[concepts.length + 1];
			roles[0] = concept;
			System.arraycopy(concepts, 0, roles, 1, concepts.length);
		}
		return roles;
	}

}