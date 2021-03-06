/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.sail.auditing.vocabulary;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * The vocabulary used by the auditing SAIL.
 */
public class Audit {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/auditing#";
	public static final URI TRANSACTION = new URIImpl(NAMESPACE + "Transaction");
	public static final URI RECENT = new URIImpl(NAMESPACE + "RecentTransaction");
	public static final URI OBSOLETE = new URIImpl(NAMESPACE + "ObsoleteTransaction");
	public static final URI CURRENT_TRX = new URIImpl(NAMESPACE + "current-trx");
	public static final URI COMMITTED_ON = new URIImpl(NAMESPACE + "committedOn");
	public static final URI REVISION = new URIImpl(NAMESPACE + "revision");
	public static final URI CONTAINED = new URIImpl(NAMESPACE + "contained");
	public static final URI MODIFIED = new URIImpl(NAMESPACE + "modified");
	public static final URI PREDECESSOR = new URIImpl(NAMESPACE + "predecessor");
	public static final URI CONTRIBUTED = new URIImpl(NAMESPACE + "contributedTo");
}
