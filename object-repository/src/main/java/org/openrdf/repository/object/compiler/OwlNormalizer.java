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
package org.openrdf.repository.object.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a series of rules against the ontology, making it easier to convert
 * into Java classes. This includes applying some OWL reasoning on properties,
 * renaming anonymous and foreign classes.
 * 
 * @author James Leigh
 * 
 */
public class OwlNormalizer {
	private final Logger logger = LoggerFactory.getLogger(OwlNormalizer.class);

	private RDFDataSource manager;

	private Set<URI> anonymousClasses = new HashSet<URI>();

	private Map<URI, URI> aliases = new HashMap<URI, URI>();

	private Map<String, URI> ontologies;

	private Set<String> commonNS = new HashSet<String>(Arrays.asList(
			RDF.NAMESPACE, RDFS.NAMESPACE, OWL.NAMESPACE));

	private static final Pattern NS_PREFIX = Pattern
			.compile("^.*[/#](\\w+)[/#]?$");

	public OwlNormalizer(RDFDataSource manager) {
		this.manager = manager;
	}

	public URI getOriginal(URI alias) {
		if (anonymousClasses.contains(alias))
			return null;
		if (aliases.containsKey(alias))
			return aliases.get(alias);
		return alias;
	}

	public Map<URI, URI> getAliases() {
		return aliases;
	}

	public Set<URI> getAnonymousClasses() {
		return anonymousClasses;
	}

	public void normalize() {
		infer();
		ontologies = findOntologies();
		checkNamespacePrefixes();
		checkPropertyDomains();
		subClassIntersectionOf();
		subClassOneOf();
		mergeDuplicateRestrictions();
		distributeEquivalentClasses();
		renameAnonymousClasses();
		mergeUnionClasses();
		moveForiegnDomains();
	}

	private Model match(Value subj, URI pred, Value obj) {
		return manager.match((Resource) subj, pred, obj);
	}

	private boolean contains(Value subj, URI pred, Value obj) {
		return manager.contains((Resource) subj, pred, obj);
	}

	private void infer() {
		logger.debug("inferring");
		ValueFactory vf = getValueFactory();
		propagateSubClassType(RDFS.CLASS);
		symmetric(OWL.INVERSEOF);
		symmetric(OWL.EQUIVALENTCLASS);
		symmetric(OWL.EQUIVALENTPROPERTY);
		symmetric(OWL.DISJOINTWITH);
		setSubjectType(RDF.FIRST, null, RDF.LIST);
		setSubjectType(RDF.REST, null, RDF.LIST);
		setSubjectType(OWL.UNIONOF, null, OWL.CLASS);
		setSubjectType(OWL.DISJOINTWITH, null, OWL.CLASS);
		setSubjectType(OWL.COMPLEMENTOF, null, OWL.CLASS);
		setSubjectType(OWL.EQUIVALENTCLASS, null, OWL.CLASS);
		setSubjectType(OWL.INTERSECTIONOF, null, OWL.CLASS);
		setSubjectType(OWL.ONPROPERTY, null, OWL.RESTRICTION);
		setSubjectType(RDF.TYPE, RDFS.CLASS, OWL.CLASS);
		setSubjectType(RDF.TYPE, OWL.DEPRECATEDCLASS, OWL.CLASS);
		setSubjectType(RDF.TYPE, OWL.RESTRICTION, OWL.CLASS);
		setObjectType(RDFS.SUBCLASSOF, OWL.CLASS);
		setObjectType(OWL.UNIONOF, RDF.LIST);
		setObjectType(RDFS.ISDEFINEDBY, OWL.ONTOLOGY);
		setSubjectType(OWL.INVERSEOF, null, OWL.OBJECTPROPERTY);
		setObjectType(OWL.INVERSEOF, OWL.OBJECTPROPERTY);
		setSubjectType(RDFS.RANGE, null, RDF.PROPERTY);
		setSubjectType(RDFS.DOMAIN, null, RDF.PROPERTY);
		setSubjectType(RDFS.SUBPROPERTYOF, null, RDF.PROPERTY);
		setObjectType(RDFS.SUBPROPERTYOF, RDF.PROPERTY);
		setDatatype(vf, OWL.CARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, OWL.MINCARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, OWL.MAXCARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
	}

	private Map<String, URI> findOntologies() {
		Map<String, URI> ontologies = new HashMap<String, URI>();
		assignOrphansToTheirOntology(ontologies);
		findNamespacesOfOntologies(ontologies);
		assignOrphansToNewOntology(ontologies);
		return ontologies;
	}

	private void assignOrphansToTheirOntology(Map<String, URI> ontologies) {
		for (Statement st : match(null, RDF.TYPE, null)) {
			Resource subj = st.getSubject();
			if (subj instanceof URI && !contains(subj, RDFS.ISDEFINEDBY, null)) {
				if (st.getContext() == null)
					continue;
				for (Resource ont : manager.match(null, RDF.TYPE, OWL.ONTOLOGY,
						st.getContext()).subjects()) {
					logger.debug("assigning {} {}", subj, ont);
					manager.add(subj, RDFS.ISDEFINEDBY, ont);
				}
			}
		}
	}

	private void findNamespacesOfOntologies(Map<String, URI> ontologies) {
		for (Resource subj : match(null, RDF.TYPE, OWL.ONTOLOGY).subjects()) {
			if (subj instanceof BNode)
				continue;
			URI ont = (URI) subj;
			logger.debug("found ontology {}", ont);
			ontologies.put(ont.toString(), ont);
			ontologies.put(ont.getNamespace(), ont);
			ontologies.put(ont.toString() + '#', ont);
			Set<String> spaces = new HashSet<String>();
			for (Resource bean : match(null, RDFS.ISDEFINEDBY, ont).subjects()) {
				if (bean instanceof URI)
					spaces.add(((URI) bean).getNamespace());
			}
			if (spaces.size() > 0) {
				for (String ns : spaces) {
					ontologies.put(ns, ont);
				}
			} else {
				ontologies.put(guessNamespace(ont), ont);
			}
		}
	}

	private void assignOrphansToNewOntology(Map<String, URI> ontologies) {
		for (Resource subj : match(null, RDF.TYPE, null).subjects()) {
			if (subj instanceof URI && !contains(subj, RDFS.ISDEFINEDBY, null)) {
				URI uri = (URI) subj;
				String ns = uri.getNamespace();
				URI ont = findOntology(ns, ontologies);
				logger.debug("assigning {} {}", uri, ont);
				manager.add(uri, RDFS.ISDEFINEDBY, ont);
			}
		}
	}

	private String guessNamespace(URI URI) {
		String ns = URI.getNamespace();
		String local = URI.getLocalName();
		if (local.endsWith("#") || local.endsWith("/")) {
			return ns + local;
		}
		if (ns.endsWith("#")) {
			return ns;
		}
		return ns + local + "#";
	}

	private URI findOntology(String ns, Map<String, URI> ontologies) {
		if (ontologies.containsKey(ns)) {
			return ontologies.get(ns);
		}
		for (Map.Entry<String, URI> e : ontologies.entrySet()) {
			String key = e.getKey();
			if (key.indexOf('#') > 0
					&& ns.startsWith(key.substring(0, key.indexOf('#'))))
				return e.getValue();
		}
		URI URI = new URIImpl(ns);
		if (ns.endsWith("#")) {
			URI = new URIImpl(ns.substring(0, ns.length() - 1));
		}
		ontologies.put(ns, URI);
		return URI;
	}

	private void propagateSubClassType(Resource classDef) {
		for (Resource c : findClasses(Collections.singleton(classDef))) {
			if (c.equals(RDFS.DATATYPE))
				continue;
			for (Statement stmt : match(null, RDF.TYPE, c)) {
				Resource subj = stmt.getSubject();
				manager.add(subj, RDF.TYPE, classDef);
			}
		}
	}

	private Set<Resource> findClasses(Collection<Resource> classes) {
		Set<Resource> set = new HashSet<Resource>(classes);
		for (Resource c : classes) {
			for (Statement stmt : match(null, RDFS.SUBCLASSOF, c)) {
				Resource subj = stmt.getSubject();
				set.add(subj);
			}
		}
		if (set.size() > classes.size()) {
			return findClasses(set);
		} else {
			return set;
		}
	}

	private void symmetric(URI pred) {
		for (Statement stmt : match(null, pred, null)) {
			if (stmt.getObject() instanceof Resource) {
				Resource subj = (Resource) stmt.getObject();
				manager.add(subj, pred, stmt.getSubject());
			} else {
				logger.warn("Invalid statement {}", stmt);
			}
		}
	}

	private void setSubjectType(URI pred, Value obj, URI type) {
		for (Statement stmt : match(null, pred, obj)) {
			manager.add(stmt.getSubject(), RDF.TYPE, type);
		}
	}

	private void setObjectType(URI pred, URI type) {
		for (Statement st : match(null, pred, null)) {
			if (st.getObject() instanceof Resource) {
				Resource subj = (Resource) st.getObject();
				manager.add(subj, RDF.TYPE, type);
			} else {
				logger.warn("Invalid statement {}", st);
			}
		}
	}

	private void addBaseClass(URI base) {
		for (Value obj : match(base, RDFS.ISDEFINEDBY, null).objects()) {
			if (obj instanceof URI) {
				URI ont = (URI) obj;
				for (Resource bean : match(null, RDFS.ISDEFINEDBY, ont)
						.subjects()) {
					if (contains(bean, RDF.TYPE, OWL.CLASS)) {
						if (!bean.equals(base)) {
							boolean isBase = true;
							for (Value e : match(bean, RDFS.SUBCLASSOF, null)
									.objects()) {
								if (contains(e, RDFS.ISDEFINEDBY, ont)) {
									isBase = false;
								}
							}
							if (isBase) {
								logger.debug("extending {} {}", bean, base);
								manager.add(bean, RDFS.SUBCLASSOF, base);
							}
						}
					}
				}
			}
		}
	}

	private void setDatatype(ValueFactory vf, URI pred, URI datatype) {
		for (Statement stmt : match(null, pred, null)) {
			String label = ((Literal) stmt.getObject()).getLabel();
			Literal literal = vf.createLiteral(label, datatype);
			manager.remove(stmt.getSubject(), pred, stmt.getObject());
			manager.add(stmt.getSubject(), pred, literal);
		}
	}

	private void checkPropertyDomains() {
		for (Statement st : match(null, RDF.TYPE, RDF.PROPERTY)) {
			Resource p = st.getSubject();
			if (!contains(p, RDFS.DOMAIN, null)) {
				loop: for (Value sup : match(p, RDFS.SUBPROPERTYOF, null)
						.objects()) {
					for (Value obj : match(sup, RDFS.DOMAIN, null).objects()) {
						manager.add(p, RDFS.DOMAIN, obj);
						break loop;
					}
				}
				if (!contains(p, RDFS.DOMAIN, null)) {
					manager.add(p, RDFS.DOMAIN, RDFS.RESOURCE);
				}
			}
		}
	}

	private void moveForiegnDomains() {
		for (Statement stmt : match(null, RDFS.DOMAIN, null)) {
			if (stmt.getSubject() instanceof URI
					&& stmt.getObject() instanceof URI) {
				URI subj = (URI) stmt.getSubject();
				URI obj = (URI) stmt.getObject();
				for (Map.Entry<String, URI> e : ontologies.entrySet()) {
					String ns = e.getKey();
					URI ont = e.getValue();
					if (isInOntology(subj, ns, ont)
							&& !isInSameOntology(subj, obj)) {
						URI nc = createLocalClass(obj, ont);
						logger.debug("moving {} {}", subj, nc);
						manager.remove(subj, RDFS.DOMAIN, obj);
						manager.add(subj, RDFS.DOMAIN, nc);
						manager.add(nc, RDF.TYPE, OWL.CLASS);
						if (!obj.equals(RDFS.RESOURCE)) {
							// {} rdfs:domain rdfs:Resource
							manager.add(nc, RDFS.SUBCLASSOF, obj);
						}
						manager.add(nc, RDFS.ISDEFINEDBY, ont);
					}
				}
			}
		}
	}

	private boolean isInOntology(URI subj, String ns, URI ont) {
		if (subj.getNamespace().equals(ns))
			return true;
		return contains(subj, RDFS.ISDEFINEDBY, ont);
	}

	private boolean isInSameOntology(URI subj, URI obj) {
		if (subj.getNamespace().equals(obj.getNamespace()))
			return true;
		for (Statement stmt : match(subj, RDFS.ISDEFINEDBY, null)) {
			if (contains(obj, RDFS.ISDEFINEDBY, stmt.getObject()))
				return true;
		}
		return false;
	}

	private URI createLocalClass(URI obj, URI ont) {
		String localName = obj.getLocalName();
		ValueFactory vf = getValueFactory();
		String prefix = findPrefix(ont);
		if (prefix != null)
			localName = initcap(prefix) + initcap(localName);
		URI nc = vf.createURI(findNamespace(ont), localName);
		aliases.put(nc, obj);
		if (obj.equals(RDFS.RESOURCE)) {
			manager.add(nc, RDF.TYPE, OWL.CLASS);
			manager.add(nc, RDFS.ISDEFINEDBY, ont);
			addBaseClass(nc);
		}
		return nc;
	}

	private ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	private String findPrefix(URI ont) {
		Map<String, String> spaces;
		spaces = manager.getNamespaces();
		for (Map.Entry<String, String> next : spaces.entrySet()) {
			if (next.getValue().equals(ont.getNamespace()))
				return next.getKey();
			for (Map.Entry<String, URI> e : ontologies.entrySet()) {
				if (e.getValue().equals(ont)
						&& next.getValue().equals(e.getKey()))
					return next.getKey();
			}
		}
		return null;
	}

	private String findNamespace(URI ont) {
		String prefix = findPrefix(ont);
		if (prefix != null) {
			String ns = manager.getNamespace(prefix);
			if (ns.endsWith("#") || ns.endsWith("/") || ns.endsWith(":"))
				return ns;
			if (ns.contains("#"))
				return ns.substring(0, ns.indexOf('#') + 1);
			return ns + "#";
		}
		String ns = ont.toString();
		if (ns.contains("#"))
			return ns.substring(0, ns.indexOf('#') + 1);
		return ont.toString() + '#';
	}

	private void renameClass(URI obj, URI nc) {
		logger.debug("renaming {} {}", obj, nc);
		aliases.put(nc, obj);
		for (Statement stmt : match(null, null, obj)) {
			Resource subj = stmt.getSubject();
			URI pred = stmt.getPredicate();
			if (isLocal(nc, subj)) {
				if (!pred.equals(RDFS.RANGE)
						|| !stmt.getObject().equals(RDFS.RESOURCE)) {
					if (!pred.equals(RDF.TYPE))
						manager.remove(subj, pred, obj);
					manager.add(subj, pred, nc);
				}
			}
		}
		if (obj.equals(RDFS.RESOURCE)) {
			addBaseClass(nc);
		}
	}

	private boolean isLocal(Resource nc, Resource obj) {
		if (obj instanceof BNode)
			return true;
		if (nc instanceof BNode)
			return true;
		return isInSameOntology((URI) nc, (URI) obj);
	}

	private void subClassIntersectionOf() {
		for (Resource subj : match(null, OWL.INTERSECTIONOF, null).subjects()) {
			for (Value of : match(subj, OWL.INTERSECTIONOF, null).objects()) {
				manager.add(subj, RDFS.SUBCLASSOF, of);
			}
		}
	}

	private void subClassOneOf() {
		for (Resource subj : match(null, OWL.ONEOF, null).subjects()) {
			List<Value> list = new ArrayList<Value>();
			for (Value of : new RDFList(manager, match(subj, OWL.ONEOF, null)
					.objectResource()).asList()) {
				if (of instanceof Resource) {
					if (contains(of, RDF.TYPE, null)) {
						for (Value type : match(of, RDF.TYPE, null).objects()) {
							if (type instanceof Resource) {
								list.add(type);
							}
						}
					} else {
						list.add(new URIImpl(OWL.NAMESPACE + "Thing"));
					}
				}
			}
			for (Value s : findCommonSupers(list)) {
				manager.add(subj, RDFS.SUBCLASSOF, s);
			}
		}
	}

	private void renameAnonymousClasses() {
		for (Resource res : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			if (res instanceof URI)
				continue;
			// if not already moved
			nameAnonymous(res);
		}
	}

	private URI nameAnonymous(Resource clazz) {
		for (Value eq : match(clazz, OWL.EQUIVALENTCLASS, null).objects()) {
			if  (eq instanceof URI) {
				rename(clazz, (URI) eq);
				return (URI) eq;
			}
		}
		Resource unionOf = match(clazz, OWL.UNIONOF, null).objectResource();
		if (unionOf != null) {
			return renameClass(clazz, "Or", new RDFList(manager, unionOf)
					.asList());
		}
		Resource intersectionOf = match(clazz, OWL.INTERSECTIONOF, null)
				.objectResource();
		if (intersectionOf != null) {
			return renameClass(clazz, "And", new RDFList(manager,
					intersectionOf).asList());
		}
		Resource oneOf = match(clazz, OWL.ONEOF, null).objectResource();
		if (oneOf != null) {
			return renameClass(clazz, "Or", new RDFList(manager, oneOf)
					.asList());
		}
		Resource complement = match(clazz, OWL.COMPLEMENTOF, null)
				.objectResource();
		if (complement != null) {
			URI comp = complement instanceof URI ? (URI) complement : null;
			if (comp == null) {
				comp = nameAnonymous(complement);
				if (comp == null)
					return null;
			}
			String name = "Not" + comp.getLocalName();
			URI uri = new URIImpl(comp.getNamespace() + name);
			rename(clazz, uri);
			return uri;
		}
		return null;
	}

	private void mergeDuplicateRestrictions() {
		Model model = match(null, OWL.ONPROPERTY, null);
		for (Statement st : model) {
			Value property = st.getObject();
			for (Resource r2 : model.filter(null, null, property).subjects()) {
				Resource r1 = st.getSubject();
				if (!r1.equals(r2)) {
					if (equivalent(r1, r2, 10)) {
						manager.add(r1, OWL.EQUIVALENTCLASS, r2);
						manager.add(r2, OWL.EQUIVALENTCLASS, r1);
					}
				}
			}
		}
	}

	private boolean equivalent(Value v1, Value v2, int depth) {
		if (depth < 0)
			return false;
		if (v1.equals(v2))
			return true;
		if (v1 instanceof Literal || v2 instanceof Literal)
			return false;
		Resource r1 = (Resource) v1;
		Resource r2 = (Resource) v2;
		if (contains(r1, OWL.EQUIVALENTCLASS, r2))
			return true;
		if (!equivalentObjects(r1, r2, OWL.ONPROPERTY, depth - 1))
			return false;
		if (equivalentObjects(r1, r2, OWL.HASVALUE, depth - 1))
			return true;
		if (equivalentObjects(r1, r2, OWL.ALLVALUESFROM, depth - 1))
			return true;
		if (equivalentObjects(r1, r2, OWL.SOMEVALUESFROM, depth - 1))
			return true;
		return false;
	}

	private boolean equivalentObjects(Resource r1, Resource r2, URI pred, int depth) {
		Set<Value> s1 = match(r1, pred, null).objects();
		Set<Value> s2 = match(r2, pred, null).objects();
		if (s1.isEmpty() || s2.isEmpty())
			return false;
		for (Value v1 : s1) {
			boolean equivalent = false;
			for (Value v2 : s2) {
				if (equivalent(v1, v2, depth - 1)) {
					equivalent = true;
					break;
				}
			}
			if (!equivalent)
				return false;
		}
		return true;
	}

	private void distributeEquivalentClasses() {
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			for (Value equiv : match(subj, OWL.EQUIVALENTCLASS, null).objects()) {
				for (Value v : match(equiv, OWL.EQUIVALENTCLASS, null)
						.objects()) {
					manager.add(subj, OWL.EQUIVALENTCLASS, v);
				}
			}
			manager.remove(subj, OWL.EQUIVALENTCLASS, subj);
		}
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			for (Value e : match(subj, OWL.EQUIVALENTCLASS, null).objects()) {
				for (Value d : match(e, OWL.DISJOINTWITH, null).objects()) {
					manager.add(subj, OWL.DISJOINTWITH, d);
				}
				if (contains(e, OWL.INTERSECTIONOF, null)) {
					Resource cinter = match(subj, OWL.INTERSECTIONOF, null)
							.objectResource();
					Resource inter = match(e, OWL.INTERSECTIONOF, null)
							.objectResource();
					if (cinter == null) {
						manager.add(subj, OWL.INTERSECTIONOF, inter);
					} else if (!inter.equals(cinter)) {
						new RDFList(manager, cinter).addAllOthers(new RDFList(
								manager, inter));
					}
				}
				if (contains(e, OWL.ONEOF, null)) {
					Resource co = match(subj, OWL.ONEOF, null).objectResource();
					Resource eo = match(e, OWL.ONEOF, null).objectResource();
					if (co == null) {
						manager.add(subj, OWL.ONEOF, match(e, OWL.ONEOF, null)
								.objectResource());
					} else if (!eo.equals(co)) {
						new RDFList(manager, co).addAllOthers(new RDFList(
								manager, eo));
					}
				}
				if (contains(e, OWL.UNIONOF, null)) {
					for (Value elist : match(e, OWL.UNIONOF, null).objects()) {
						if (!contains(subj, OWL.UNIONOF, null)) {
							manager.add(subj, OWL.UNIONOF, elist);
						} else if (!contains(subj, OWL.UNIONOF, elist)) {
							for (Value clist : match(subj, OWL.UNIONOF, null)
									.objects()) {
								new RDFList(manager, (Resource) clist)
										.addAllOthers(new RDFList(manager,
												(Resource) elist));
							}
						}
					}
				}
				if (contains(e, OWL.COMPLEMENTOF, null)) {
					if (!contains(subj, OWL.COMPLEMENTOF, null)) {
						Resource comp = match(e, OWL.COMPLEMENTOF, null)
								.objectResource();
						manager.add(subj, OWL.COMPLEMENTOF, comp);
					}
				}
				if (contains(e, OWL.DISJOINTWITH, null)) {
					for (Value d : match(e, OWL.DISJOINTWITH, null).objects()) {
						manager.add(subj, OWL.DISJOINTWITH, d);
					}
				}
			}
		}
	}

	private void mergeUnionClasses() {
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			List<Value> unionOf = new ArrayList<Value>();
			for (Value obj : match(subj, OWL.UNIONOF, null).objects()) {
				if (obj instanceof Resource) {
					List<? extends Value> list = new RDFList(manager,
							(Resource) obj).asList();
					list.removeAll(unionOf);
					unionOf.addAll(list);
				}
			}
			if (!unionOf.isEmpty()) {
				Set<URI> common = findCommonSupers(unionOf);
				if (common.contains(subj)) {
					// if union contains itself then remove it
					manager.remove(subj, OWL.UNIONOF, null);
					continue;
				} else if (findCommon(common, unionOf) != null) {
					// if union includes the common super class then fold
					// together
					URI sup = findCommon(common, unionOf);
					manager.remove(subj, OWL.UNIONOF, null);
					rename(subj, sup);
					continue;
				}
				for (URI c : common) {
					manager.add(subj, RDFS.SUBCLASSOF, c);
				}
				for (Value ofValue : unionOf) {
					if (contains(ofValue, RDF.TYPE, RDFS.DATATYPE)
							&& ofValue instanceof URI) {
						// don't use anonymous class for datatypes
						rename(subj, (URI) ofValue);
					} else if (isLocal(subj, (Resource) ofValue)) {
						manager.add((Resource) ofValue, RDFS.SUBCLASSOF, subj);
					} else {
						URI ont = match(subj, RDFS.ISDEFINEDBY, null)
								.objectURI();
						URI nc = createLocalClass((URI) ofValue, ont);
						manager.add(nc, RDF.TYPE, OWL.CLASS);
						manager.add(nc, RDFS.SUBCLASSOF, ofValue);
						manager.add(nc, RDFS.SUBCLASSOF, subj);
						manager.add(nc, RDFS.ISDEFINEDBY, ont);
						renameClass((URI) ofValue, nc);
					}
				}
			}
		}
	}

	private URI findCommon(Set<URI> common, Collection<? extends Value> unionOf) {
		URI result = null;
		for (Value e : unionOf) {
			if (common.contains(e)) {
				result = (URI) e;
			}
		}
		return result;
	}

	private Set<URI> findCommonSupers(List<? extends Value> unionOf) {
		Set<? extends Value> common = null;
		for (Value of : unionOf) {
			if (of instanceof Resource) {
				Set supers = findSuperClasses((Resource) of);
				if (common == null) {
					common = new HashSet<Value>(supers);
				} else {
					common.retainAll(supers);
				}
			}
		}
		if (common == null)
			return Collections.emptySet();
		Iterator<? extends Value> iter = common.iterator();
		while (iter.hasNext()) {
			if (!(iter.next() instanceof URI)) {
				iter.remove();
			}
		}
		return (Set<URI>) common;
	}

	private Set findSuperClasses(Resource of) {
		HashSet set = new HashSet();
		set.add(of);
		return findSuperClasses(of, set);
	}

	private Set findSuperClasses(Resource of, Set supers) {
		Set<Value> parent = match(of, RDFS.SUBCLASSOF, null).objects();
		if (supers.addAll(parent)) {
			for (Value s : parent) {
				if (s instanceof Resource) {
					findSuperClasses((Resource) s, supers);
				}
			}
		}
		return supers;
	}

	private URI renameClass(Resource clazz, String and,
			List<? extends Value> list) {
		String namespace = null;
		Set<String> names = new TreeSet<String>();
		for (Value of : list) {
			URI URI = null;
			if (of instanceof URI) {
				URI = (URI) of;
			} else {
				if (!contains(of, RDF.TYPE, OWL.CLASS))
					return null;
				URI = nameAnonymous((Resource) of);
				if (URI == null)
					return null;
			}
			if (namespace == null || commonNS.contains(namespace)) {
				namespace = URI.getNamespace();
			}
			names.add(URI.getLocalName());
		}
		StringBuilder sb = new StringBuilder();
		for (String localPart : names) {
			sb.append(initcap(localPart));
			sb.append(and);
		}
		sb.setLength(sb.length() - and.length());
		URIImpl dest = new URIImpl(namespace + sb.toString());
		rename(clazz, dest);
		return dest;
	}

	private void rename(Resource orig, URI dest) {
		if (contains(dest, RDF.TYPE, OWL.CLASS)) {
			logger.debug("merging {} {}", orig, dest);
		} else {
			logger.debug("renaming {} {}", orig, dest);
			manager.add(dest, RDF.TYPE, OWL.CLASS);
			URI ont = findOntology(dest.getNamespace(), ontologies);
			manager.add(dest, RDFS.ISDEFINEDBY, ont);
			anonymousClasses.add(dest);
		}
		for (Statement stmt : match(orig, null, null)) {
			manager.add(dest, stmt.getPredicate(), stmt.getObject());
		}
		manager.remove(orig, null, null);
		for (Statement stmt : match(null, null, orig)) {
			manager.add(stmt.getSubject(), stmt.getPredicate(), dest);
		}
		manager.remove((Resource) null, null, orig);
	}

	private String initcap(String str) {
		if (str.length() < 2)
			return str.toUpperCase();
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private void checkNamespacePrefixes() {
		for (Statement st : match(null, RDFS.ISDEFINEDBY, null)) {
			if (!st.getSubject().equals(st.getObject())) {
				Value value = st.getSubject();
				if (value instanceof BNode)
					continue;
				String ns = ((URI) value).getNamespace();
				String prefix = getPrefix(ns);
				if (prefix == null) {
					Matcher matcher = NS_PREFIX.matcher(ns);
					if (matcher.find()) {
						prefix = matcher.group(1);
						if (Character.isLetter(prefix.charAt(0))) {
							logger.debug("creating prefix {} {}", prefix, ns);
							manager.setNamespace(prefix, ns);
						}
					}
				}
			}
		}
	}

	private String getPrefix(String namespace) {
		for (Map.Entry<String, String> ns : manager.getNamespaces().entrySet()) {
			if (namespace.equals(ns.getValue()))
				return ns.getKey();
		}
		return null;
	}
}