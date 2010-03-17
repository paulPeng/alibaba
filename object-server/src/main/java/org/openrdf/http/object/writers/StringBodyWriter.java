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
package org.openrdf.http.object.writers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.openrdf.repository.object.ObjectFactory;

/**
 * Writes a {@link String}.
 * 
 * @author James Leigh
 * 
 */
public class StringBodyWriter implements MessageBodyWriter<String> {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (!String.class.equals(type))
			return false;
		return mimeType == null || mimeType.startsWith("text/")
				|| mimeType.startsWith("*");
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, String str, Charset charset) {
		if (charset == null)
			return str.length(); // UTF-8
		return charset.encode(str).limit();
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (charset == null) {
			charset = UTF8;
		}
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/plain";
		}
		if (mimeType.contains("charset=") || !mimeType.startsWith("text/"))
			return mimeType;
		return mimeType + ";charset=" + charset.name();
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, String result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException {
		if (charset == null) {
			charset = UTF8;
		}
		Writer writer = new OutputStreamWriter(out, charset);
		writer.write(result);
		writer.flush();
	}

	public ReadableByteChannel write(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, String result, String base,
			Charset charset) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeTo(mimeType, type, genericType, of, result, base, charset, out,
				1024);
		return Channels.newChannel(new ByteArrayInputStream(out.toByteArray()));
	}
}
