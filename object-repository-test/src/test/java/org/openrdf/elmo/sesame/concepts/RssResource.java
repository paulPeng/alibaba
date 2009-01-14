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
package org.openrdf.elmo.sesame.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.localized;
import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface RssResource extends DcResource {

	/**
	 * Description. A short text description of the subject.
	 */
	@localized
	@rdf("http://purl.org/rss/1.0/description")
	public abstract String getRssDescription();

	/**
	 * Description. A short text description of the subject.
	 */
	public abstract void setRssDescription(String value);

	/**
	 * Link. The URL to which an HTML rendering of the subject will link.
	 */
	@rdf("http://purl.org/rss/1.0/link")
	public abstract Set<String> getRssLinks();

	/**
	 * Link. The URL to which an HTML rendering of the subject will link.
	 */
	public abstract void setRssLinks(Set<String> value);

	/**
	 * Title. A descriptive title for the channel.
	 */
	@localized
	@rdf("http://purl.org/rss/1.0/title")
	public abstract String getRssTitle();

	/**
	 * Title. A descriptive title for the channel.
	 */
	public abstract void setRssTitle(String value);

}