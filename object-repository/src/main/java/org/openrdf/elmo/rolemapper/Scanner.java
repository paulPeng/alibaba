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
package org.openrdf.elmo.rolemapper;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;

import org.openrdf.repository.object.annotations.rdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans directories and jars for classes that have one or more Elmo annotation.
 * 
 * @author James Leigh
 * 
 */
class Scanner {
	private final Logger logger = LoggerFactory.getLogger(HierarchicalRoleMapper.class);

	private ClassLoader cl;

	private String pkgName;

	private String resource;

	public Scanner(ClassLoader cl) {
		this(cl, null);
	}

	public Scanner(ClassLoader cl, String resource) {
		this.cl = cl;
		this.resource = resource;
		pkgName = rdf.class.getPackage().getName();
	}

	public List<String> scan(URL url, String... marker) throws IOException {
		String urlPath = URLDecoder.decode(url.getFile(), "UTF-8");
		if (resource != null) {
			assert urlPath.endsWith(resource);
			urlPath = urlPath.substring(0, urlPath.length() - resource.length());
		}
		if (urlPath.startsWith("file:")) {
			urlPath = urlPath.substring(5);
		}
		if (urlPath.lastIndexOf('!') > 0) {
			urlPath = urlPath.substring(0, urlPath.lastIndexOf('!'));
		}
		List<String> roles = new ArrayList<String>();
		File file = new File(urlPath);
		logger.info("Scanning {}", file);
		if (file.isDirectory()) {
			if (!exists(file, marker)) {
				roles.addAll(scanDirectory(file, null));
			}
		} else {
			ZipFile zip = new ZipFile(file);
			if (!exists(zip, marker)) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = getClassName(entry.getName());
					if (name != null) {
						roles.add(name);
					}
				}
			}
		}
		return roles;
	}

	private boolean exists(ZipFile zip, String... marks){
		if (marks == null || marks.length == 0)
			return false;
		for (String marker : marks) {
			if (zip.getEntry(marker) != null)
				return true;
		}
		return false;
	}

	private boolean exists(File file, String... marks) {
		if (marks == null || marks.length == 0)
			return false;
		for (String marker : marks) {
			if (new File(file, marker).exists())
				return true;
		}
		return false;
	}

	private List<String> scanDirectory(File file, String path)
			throws IOException {
		List<String> roles = new ArrayList<String>();
		for (File child : file.listFiles()) {
			String newPath = path == null ? child.getName() : path + '/'
					+ child.getName();
			if (child.isDirectory()) {
				roles.addAll(scanDirectory(child, newPath));
			} else {
				String name = getClassName(newPath);
				if (name != null) {
					roles.add(name);
				}
			}
		}
		return roles;
	}

	private String getClassName(String name) throws IOException {
		// NOTE package-info.class should be excluded
		if (!name.endsWith(".class") || name.contains("-"))
			return null;
		InputStream stream = cl.getResourceAsStream(name);
		assert stream != null : name;
		DataInputStream dstream = new DataInputStream(stream);
		try {
			ClassFile cf = new ClassFile(dstream);
			AnnotationsAttribute attr = (AnnotationsAttribute) cf
					.getAttribute(AnnotationsAttribute.visibleTag);
			if (attr != null) {
				Annotation[] annotations = attr.getAnnotations();
				if (annotations != null) {
					for (Annotation ann : annotations) {
						if (ann.getTypeName().startsWith(pkgName))
							return cf.getName();
					}
				}
			}
			for (String fname : cf.getInterfaces()) {
				String cn = fname.replace('.', '/') + ".class";
				if (getClassName(cn) != null)
					return cf.getName();
			}
		} catch (Exception e) {
			logger.warn("Cannot decode {}", name);
			logger.debug("Cannot decode " + name, e);
		} finally {
			dstream.close();
			stream.close();
		}
		return null;
	}
}