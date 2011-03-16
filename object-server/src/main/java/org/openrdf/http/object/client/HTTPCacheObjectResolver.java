/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, Talis Inc., Some rights reserved.
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
package org.openrdf.http.object.client;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.exceptions.BadGateway;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.repository.object.util.ObjectResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses {@link HTTPObjectClient} caching the result in a Java Object.
 * 
 * @author James Leigh
 * 
 */
public class HTTPCacheObjectResolver<T> extends ObjectResolver<T> {

	public static void resetCache() {
		resetCount++;
	}

	public static void invalidateCache() {
		invalidateCount++;
	}

	private static final Pattern SMAXAGE = Pattern
			.compile("\\bs-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("\\bmax-age\\s*=\\s*(\\d+)");
	private static final Pattern CHARSET = Pattern
			.compile("\\bcharset\\s*=\\s*([\\w-:]+)");
	private static volatile int invalidateCount;
	private static volatile int resetCount;
	private static Logger logger = LoggerFactory.getLogger(HTTPCacheObjectResolver.class);
	private int invalidateLastCount = invalidateCount;
	private int resetLastCount = resetCount;
	private String uri;
	private String tag;
	private Integer maxage;
	private long expires;
	private T object;

	@Override
	public synchronized T resolve(String systemId) throws Exception {
		if (!systemId.startsWith("http:"))
			return super.resolve(systemId);
		if (uri == null || !uri.equals(systemId)
				|| resetLastCount != resetCount) {
			uri = systemId;
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
			resetLastCount = resetCount;
		} else if (object != null
				&& (expires == 0 || expires > currentTimeMillis())
				&& invalidateLastCount == invalidateCount) {
			return object;
		}
		invalidateLastCount = invalidateCount;
		HttpResponse resp = resolve(systemId, 20);
		if (isStorable(getHeader(resp, "Cache-Control"))) {
			return object = createObject(systemId, resp);
		} else {
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
			return createObject(systemId, resp);
		}
	}

	private HttpResponse resolve(String url, int max)
			throws IOException {
		HttpRequest req = new BasicHttpRequest("GET", url);
		req.setHeader("Accept", join(getObjectFactory().getContentTypes()));
		if (tag != null && object != null) {
			req.setHeader("If-None-Match", tag);
		}
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(req);
		int code = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (code >= 300 && code < 400 && code != 304) {
			if (entity != null) {
				entity.consumeContent();
			}
			if (max < 0)
				throw new BadGateway("To Many Redirects: " + url);
			Header location = resp.getFirstHeader("Location");
			if (location == null)
				return resp;
			return resolve(location.getValue(), max - 1);
		}
		return resp;
	}

	private String getHeader(HttpResponse resp, String name) {
		if (resp.containsHeader(name))
			return resp.getFirstHeader(name).getValue();
		return null;
	}

	private String join(String[] contentTypes) {
		if (contentTypes == null)
			return "*/*";
		int iMax = contentTypes.length - 1;
		if (iMax == -1)
			return "*/*";

		StringBuilder b = new StringBuilder();
		for (int i = 0;; i++) {
			b.append(String.valueOf(contentTypes[i]));
			if (i == iMax)
				return b.toString();
			b.append(", ");
		}
	}

	private boolean isStorable(String cc) {
		if (!getObjectFactory().isReusable())
			return false;
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private T createObject(String systemId, HttpResponse con) throws Exception {
		HttpEntity entity = con.getEntity();
		InputStream in = entity == null ? null : entity.getContent();
		String type = getHeader(con, "Content-Type");
		String cacheControl = getHeader(con, "Cache-Control");
		expires = getExpires(cacheControl, expires);
		int status = con.getStatusLine().getStatusCode();
		if (status == 304 || status == 412) {
			assert object != null;
			return object; // Not Modified
		} else if (status >= 300) {
			throw ResponseException.create(con);
		}
		if (getObjectFactory().isReusable()) {
			logger.info("Compiling {}", systemId);
		}
		tag = getHeader(con, "ETag");
		Matcher m = CHARSET.matcher(type);
		if (m.find()) {
			Reader reader = new InputStreamReader(in, m.group(1));
			return getObjectFactory().create(systemId, reader);
		}
		return getObjectFactory().create(systemId, in);
	}

	private long getExpires(String cacheControl, long defaultValue) {
		if (cacheControl != null && cacheControl.contains("s-maxage")) {
			try {
				Matcher m = SMAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		} else if (cacheControl != null && cacheControl.contains("max-age")) {
			try {
				Matcher m = MAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		if (maxage != null)
			return currentTimeMillis() + maxage * 1000;
		return defaultValue;
	}

}
