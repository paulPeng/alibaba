package org.openrdf.alibaba.decor;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Decortation used by presentations and representations. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#Decoration")
public interface Decoration extends Thing, DecorationBehaviour {

}