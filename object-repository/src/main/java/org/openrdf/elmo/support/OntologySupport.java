/*
 * Copyright (c) 2008, James Leigh All rights reserved.
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
package org.openrdf.elmo.codegen.support;

import java.io.File;

import javax.xml.namespace.QName;

import org.openrdf.elmo.codegen.JavaNameResolver;
import org.openrdf.elmo.codegen.builder.JavaCodeBuilder;
import org.openrdf.elmo.codegen.concepts.CodeOntology;
import org.openrdf.elmo.codegen.source.JavaClassBuilder;

public abstract class OntologySupport implements CodeOntology {
	private CodeOntology self;

	public OntologySupport(CodeOntology self) {
		this.self = self;
	}

	public File generatePackageInfo(File dir, String namespace,
			JavaNameResolver resolver) throws Exception {
		String pkg = resolver.getPackageName(new QName(namespace, ""));
		File source = createSourceFile(dir, pkg, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(jcb, resolver);
		builder.packageInfo(self, namespace);
		builder.close();
		return source;
	}

	private File createSourceFile(File dir, String pkg, JavaNameResolver resolver) {
		String simple = "package-info";
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		File source = new File(folder, simple + ".java");
		return source;
	}

}
