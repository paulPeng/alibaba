/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.openrdf.http.object.util.ChannelUtil;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;

/**
 * Allows an {@link ReadableByteChannel} to be used as an HttpEntity.
 * 
 * @author James Leigh
 * 
 */
public class ReadableHttpEntityChannel implements HttpEntityChannel {
	private String contentType;
	private long contentLength;
	private ByteBuffer buf = ByteBuffer.allocate(1024 * 8);
	private ReadableByteChannel cin;
	private List<Runnable> onClose;

	public ReadableHttpEntityChannel(String type, long length,
			ReadableByteChannel in) {
		this(type, length, in, (List<Runnable>) null);
	}

	public ReadableHttpEntityChannel(String type, long length,
			ReadableByteChannel in, Runnable... onClose) {
		this(type, length, in, Arrays.asList(onClose));
	}

	public ReadableHttpEntityChannel(String type, long length,
			ReadableByteChannel in, List<Runnable> onClose) {
		assert in != null;
		this.contentType = type;
		this.contentLength = length;
		this.cin = in;
		this.onClose = onClose;
	}

	public final void consumeContent() throws IOException {
		finish();
	}

	public InputStream getContent() throws IOException {
		return ChannelUtil.newInputStream(cin);
	}

	public ReadableByteChannel getReadableByteChannel() {
		return cin;
	}

	public Header getContentEncoding() {
		return new BasicHeader("Content-Encoding", "identity");
	}

	public long getContentLength() {
		return contentLength;
	}

	public Header getContentType() {
		return new BasicHeader("Content-Type", contentType);
	}

	public boolean isChunked() {
		return getContentLength() < 0;
	}

	public boolean isRepeatable() {
		return false;
	}

	public boolean isStreaming() {
		return true;
	}

	public void writeTo(OutputStream out) throws IOException {
		InputStream in = getContent();
		try {
			byte[] buf = new byte[1024];
			int read;
			while ((read = in.read(buf)) >= 0) {
				out.write(buf, 0, read);
			}
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}

	public void finish() throws IOException {
		close();
	}

	public void produceContent(ContentEncoder encoder, IOControl ioctrl)
			throws IOException {
		buf.clear();
		if (cin.read(buf) < 0) {
			encoder.complete();
		} else {
			buf.flip();
			encoder.write(buf);
		}
	}

	public void close() throws IOException {
		try {
			cin.close();
		} finally {
			if (onClose != null) {
				for (Runnable task : onClose) {
					try {
						task.run();
					} catch (RuntimeException e) {
					} catch (Error e) {
					}
				}
			}
		}
	}

	public boolean isOpen() {
		return cin.isOpen();
	}
}