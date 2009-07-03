package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GUnzipResponse extends HttpServletResponseWrapper {
	private HttpServletResponse response;
	private boolean compressed = false;
	private ServletOutputStream out;
	private int length = -1;
	private String size;
	private String tag;

	public GUnzipResponse(HttpServletResponse response) {
		super(response);
		this.response = response;
	}

	@Override
	public void setHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("ETag".equalsIgnoreCase(name)) {
			tag = value;
		} else if ("Content-Encoding".equalsIgnoreCase(name)) {
			if (value.contains("gzip")) {
				compressed = true;
			} else {
				super.setHeader(name, value);
			}
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void addHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("ETag".equalsIgnoreCase(name)) {
			tag = tag == null ? value : tag + "," + value;
		} else if ("Content-Encoding".equalsIgnoreCase(name)) {
			if (value.contains("gzip")) {
				compressed = true;
			} else {
				super.addHeader(name, value);
			}
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			length = value;
		} else {
			super.addIntHeader(name, value);
		}
	}

	@Override
	public void setContentLength(int len) {
		length = len;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (compressed) {
			if (out == null) {
				ServletOutputStream stream = super.getOutputStream();
				out = new OutputServletStream(GUnzipOutputStream.create(stream));
			}
			return out;
		} else {
			flush();
			return response.getOutputStream();
		}
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void flush() throws IOException {
		if (out != null) {
			out.flush();
		} else {
			if (tag != null) {
				response.setHeader("ETag", tag);
				tag = null;
			}
			if (size != null) {
				response.setHeader("Content-Length", size);
				size = null;
			} else if (length > -1) {
				response.setContentLength(length);
				length = -1;
			}
		}
	}

	@Override
	public void flushBuffer() throws IOException {
		flush();
		super.flushBuffer();
	}

}
