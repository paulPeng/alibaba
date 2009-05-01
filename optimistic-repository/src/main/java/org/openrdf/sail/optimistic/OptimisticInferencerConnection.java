package org.openrdf.sail.optimistic;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.UnionIteration;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.sail.SailException;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;

public class OptimisticInferencerConnection extends OptimisticConnection
		implements InferencerConnection {
	private Model added = new LinkedHashModel();
	private Model removed = new LinkedHashModel();
	private InferencerConnection delegate;

	public OptimisticInferencerConnection(OptimisticSail sail,
			InferencerConnection delegate) {
		super(sail, delegate);
		this.delegate = delegate;
	}

	public boolean addInferredStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		AddOperation op = new AddOperation() {

			public int addLater(Resource subj, URI pred, Value obj,
					Resource... contexts) {
				if (removed.contains(subj, pred, obj, contexts)) {
					removed.remove(subj, pred, obj, contexts);
				} else {
					added.add(subj, pred, obj, contexts);
				}
				return added.size();
			}

			public void addNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.addInferredStatement(subj, pred, obj, contexts);
			}
		};
		add(op, subj, pred, obj, contexts);
		return true;
	}

	public void clearInferred(Resource... contexts) throws SailException {
		removeInferredStatement(null, null, null, contexts);
	}

	public boolean removeInferredStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		RemoveOperation op = new RemoveOperation() {

			public int removeLater(Statement st) {
				added.remove(st);
				removed.add(st);
				return removed.size();
			}

			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.removeInferredStatement(subj, pred, obj, contexts);
			}
		};
		return remove(op, subj, pred, obj, true, contexts);
	}

	public void flushUpdates() throws SailException {
		// no-op
	}

	@Override
	public synchronized void begin() throws SailException {
		added.clear();
		removed.clear();
		super.begin();
	}

	@Override
	public synchronized void rollback() throws SailException {
		added.clear();
		removed.clear();
		super.rollback();
	}

	@Override
	void flush() throws SailException {
		super.flush();
		for (Statement st : removed) {
			delegate.removeInferredStatement(st.getSubject(),
					st.getPredicate(), st.getObject(), st.getContext());
		}
		removed.clear();
		for (Statement st : added) {
			delegate.addInferredStatement(st.getSubject(), st.getPredicate(),
					st.getObject(), st.getContext());
		}
		added.clear();
	}

	@Override
	public long size(Resource... contexts) throws SailException {
		long size = super.size(contexts);
		if (isAutoCommit())
			return size;
		synchronized (this) {
			int rsize = removed.filter(null, null, null, contexts).size();
			int asize = added.filter(null, null, null, contexts).size();
			return size - rsize + asize;
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(
			Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) throws SailException {
		CloseableIteration<? extends Statement, SailException> result;
		result = super.getStatements(subj, pred, obj, inf, contexts);
		synchronized (this) {
			if (!inf || isAutoCommit() || added.isEmpty() && removed.isEmpty())
				return result;
			Model excluded = removed.filter(subj, pred, obj, contexts);
			Model included = added.filter(subj, pred, obj, contexts);
			if (included.isEmpty() && excluded.isEmpty())
				return result;
			if (!excluded.isEmpty()) {
				final Set<Statement> set = new HashSet<Statement>(excluded);
				result = new FilterIteration<Statement, SailException>(result) {
					@Override
					protected boolean accept(Statement stmt)
							throws SailException {
						return !set.contains(stmt);
					}
				};
			}
			HashSet<Statement> set = new HashSet<Statement>(included);
			CloseableIteration<Statement, SailException> incl;
			incl = new CloseableIteratorIteration<Statement, SailException>(set
					.iterator());
			return new UnionIteration<Statement, SailException>(incl, result);
		}
	}

	@Override
	public synchronized CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(
			TupleExpr qry, Dataset dataset, BindingSet bindings, boolean inf)
			throws SailException {
		TupleExpr query = qry;
		synchronized (this) {
			if (inf && !isAutoCommit()
					&& (!added.isEmpty() || !removed.isEmpty())) {
				query = query.clone();
				DeltaMerger merger = new DeltaMerger(added, removed);
				merger.optimize(query, dataset, bindings);
			}
		}
		return super.evaluate(query, dataset, bindings, inf);
	}

}