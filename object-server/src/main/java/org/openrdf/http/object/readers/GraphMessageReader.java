/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.readers;

import java.io.InputStream;
import org.openrdf.http.object.util.ChannelUtil;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;

import org.openrdf.http.object.readers.base.MessageReaderBase;
import org.openrdf.http.object.util.BackgroundGraphResult;
import org.openrdf.http.object.util.SharedExecutors;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;

/**
 * Reads RDF graph messages.
 * 
 * @author James Leigh
 * 
 */
public class GraphMessageReader extends
		MessageReaderBase<RDFFormat, RDFParserFactory, GraphQueryResult> {
	private static Executor executor = SharedExecutors.getParserThreadPool();

	public GraphMessageReader() {
		super(RDFParserRegistry.getInstance(), GraphQueryResult.class);
	}

	@Override
	public GraphQueryResult readFrom(RDFParserFactory factory, ReadableByteChannel cin,
			Charset charset, String base) {
		assert cin != null;
		RDFParser parser = factory.getParser();
		InputStream in = ChannelUtil.newInputStream(cin);
		BackgroundGraphResult result = new BackgroundGraphResult(parser, in,
				charset, base);
		executor.execute(result);
		return result;
	}

}
