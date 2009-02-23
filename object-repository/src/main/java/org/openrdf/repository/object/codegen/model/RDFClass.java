/*
 * Copyright (c) 2008-2009, Zepheira All rights reserved.
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
package org.openrdf.repository.object.codegen.model;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.codegen.JavaNameResolver;
import org.openrdf.repository.object.codegen.source.JavaClassBuilder;
import org.openrdf.repository.object.codegen.source.JavaCodeBuilder;
import org.openrdf.repository.object.vocabulary.ELMO;

public class RDFClass extends RDFEntity {

	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");

	public RDFClass(Model model, Resource self) {
		super(model, self);
	}

	public BigInteger getBigInteger(URI pred) {
		Value value = model.filter(self, pred, null).objectValue();
		if (value == null)
			return null;
		return new BigInteger(value.stringValue());
	}

	public RDFProperty getRDFProperty(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	private Iterable<RDFProperty> getDeclaredProperties() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (prop instanceof URI) {
				set.add(prop.stringValue());
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>(set.size());
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFClass getRange(RDFProperty property) {
		RDFClass range = null;
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					RDFClass type = c.getRDFClass(OWL.ALLVALUESFROM);
					if (type != null) {
						range = (RDFClass) type;
					}
				}
			}
		}
		if (range != null)
			return range;
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION) || c.equals(this))
				continue;
			RDFClass type = ((RDFClass) c).getRange(property);
			if (type != null) {
				range = (RDFClass) type;
			}
		}
		if (range != null)
			return range;
		for (RDFClass r : property.getRDFClasses(RDFS.RANGE)) {
			range = (RDFClass) r;
		}
		if (range != null)
			return range;
		for (RDFProperty p : property.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			RDFClass superRange = getRange(p);
			if (superRange != null) {
				range = superRange;
			}
		}
		return range;
	}

	public boolean isFunctional(RDFProperty property) {
		if (property.isA(OWL.FUNCTIONALPROPERTY))
			return true;
		boolean functional = false;
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						functional = true;
					}
				}
			}
		}
		if (functional)
			return functional;
		RDFClass range = getRange(property);
		if (range == null)
			return false;
		return NOTHING.equals(range.getURI());
	}


	public File generateSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFileC(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(jcb, resolver);
		if (isA(RDFS.DATATYPE)) {
			builder.classHeader(this);
			builder.stringConstructor(this);
		} else {
			builder.interfaceHeader(this);
			builder.constants(this);
			for (RDFProperty prop : getDeclaredProperties()) {
				if (prop instanceof RDFProperty
						&& ((RDFProperty) prop).isMethod())
					continue;
				builder.property(this, prop);
			}
			for (RDFClass type : getMessageTypes()) {
				builder.message(type);
			}
		}
		builder.close();
		return source;
	}

	private File createSourceFileC(File dir, JavaNameResolver resolver) {
		String pkg = resolver.getPackageName(getURI());
		String simple = resolver.getSimpleName(getURI());
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		File source = new File(folder, simple + ".java");
		return source;
	}

	public Iterable<RDFClass> getMessageTypes() {
		List<RDFClass> list = new ArrayList<RDFClass>();
		for (Resource res : model.filter(null, OWL.ALLVALUESFROM, self).subjects()) {
			if (model.contains(res, OWL.ONPROPERTY, ELMO.TARGET)) {
				for (Resource msg : model.filter(null, RDFS.SUBCLASSOF, res).subjects()) {
					list.add(new RDFClass(model, msg));
				}
			}
		}
		return list;
	}

	public boolean isMessageClass() {
		return isMessage(this, new HashSet<RDFClass>());
	}

	public List<RDFProperty> getParameters() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
				if (prop instanceof URI) {
					set.add(prop.stringValue());
				}
			}
		}
		URI ont = model.filter(self, RDFS.ISDEFINEDBY, null).objectURI();
		for (Value sup : model.filter(self, RDFS.SUBCLASSOF, null).objects()) {
			if (model.contains((Resource) sup, RDFS.ISDEFINEDBY, ont)) {
				for (Resource prop : model.filter(null, RDFS.DOMAIN, sup).subjects()) {
					if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
						if (prop instanceof URI) {
							set.add(prop.stringValue());
						}
					}
				}
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFProperty getResponseProperty() {
		RDFProperty obj = new RDFProperty(model, ELMO.OBJECT_RESPONSE);
		RDFProperty lit = new RDFProperty(model, ELMO.LITERAL_RESPONSE);
		boolean objUsed = false;
		boolean litUsed = false;
		boolean obj0 = false;
		boolean lit0 = false;
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
				BigInteger card = c.getBigInteger(OWL.CARDINALITY);
				BigInteger max = c.getBigInteger(OWL.MAXCARDINALITY);
				if (obj.equals(property)) {
					objUsed = true;
					if (card != null && 0 == card.intValue()) {
						obj0 = true;
					} else if (max != null && 0 == max.intValue()) {
						obj0 = true;
					}
				} else if (lit.equals(property)) {
					litUsed = true;
					if (card != null && 0 == card.intValue()) {
						lit0 = true;
					} else if (max != null &&0 == max.intValue()) {
						lit0 = true;
					}
				}
			}
		}
		if (obj0 && !lit0)
			return lit;
		if (litUsed && !objUsed)
			return lit;
		return obj;
	}

	private boolean isMessage(RDFClass message,
			Set<RDFClass> set) {
		if (ELMO.MESSAGE.equals(message.getURI()))
			return true;
		set.add(message);
		for (RDFClass sup : message.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (!set.contains(sup) && isMessage(sup, set))
				return true;
		}
		return false;
	}


	public File generateInvokeSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFileM(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(jcb, resolver);
		builder.invokeClassHeader(this);
		builder.invokeMethod(this);
		builder.close();
		return source;
	}

	private File createSourceFileM(File dir, JavaNameResolver resolver) {
		String pkg = resolver.getPackageName(getURI());
		String simple = resolver.getSimpleName(getURI());
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		File source = new File(folder, simple + JavaCodeBuilder.INVOKE_SUFFIX + ".java");
		return source;
	}
}