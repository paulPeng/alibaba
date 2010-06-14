/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
package org.openrdf.sail.optimistic.helpers;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;

/**
 * Changeset that occurred during a transaction and all the read operations that
 * were performed since the changeset appeared.
 * 
 * @author James Leigh
 * 
 */
public class ChangeWithReadSet {
	private Model added;
	private Model removed;
	private Set<EvaluateOperation> read = new HashSet<EvaluateOperation>();

	public ChangeWithReadSet(Model added, Model removed) {
		this.added = new LinkedHashModel(added);
		this.removed = new LinkedHashModel(removed);
	}

	public Model getAdded() {
		return added;
	}

	public Model getRemoved() {
		return removed;
	}

	public Set<EvaluateOperation> getReadOperations() {
		return read;
	}

	public void addRead(EvaluateOperation op) {
		read.add(op);
	}
}
