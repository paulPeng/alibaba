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
package org.openrdf.sail.auditing.config;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * Schema for the auditing configuration.
 *
 * @author James Leigh
 */
public class AuditingSchema {

	/** http://www.openrdf.org/config/sail/auditing# */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/auditing#";

	public static final URI TRX_NAMESPACE = new URIImpl(NAMESPACE
			+ "trxNamespace");
	public static final URI ARCHIVING = new URIImpl(NAMESPACE + "archiving");
	public static final URI MAX_ARCHIVE = new URIImpl(NAMESPACE + "maximumArchiveTriples");
	public static final URI MIN_RECENT = new URIImpl(NAMESPACE + "minimumRecentTransactions");
	public static final URI MAX_RECENT = new URIImpl(NAMESPACE + "maximumRecentTransactions");

	private AuditingSchema() {
		// no constructor
	}
}
