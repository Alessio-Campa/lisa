package it.unive.lisa.analysis;

import java.util.Map;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A functional lattice that stores instances of {@link AnalysisState} computed
 * on statements. Storing states in such an object enables easy fixpoint
 * computation thanks to the function lub and widening operations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState}
 * @param <H> the type of the {@link HeapDomain}
 * @param <V> the type of the {@link ValueDomain}
 */
public class StatementStore<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>>
		extends FunctionalLattice<StatementStore<A, H, V>, Statement, AnalysisState<A, H, V>> {

	/**
	 * Builds the store.
	 * 
	 * @param state an instance of the underlying lattice
	 */
	public StatementStore(AnalysisState<A, H, V> state) {
		super(state);
	}
	
	/**
	 * Builds the store.
	 * 
	 * @param state an instance of the underlying lattice
	 */
	private StatementStore(AnalysisState<A, H, V> state, Map<Statement, AnalysisState<A, H, V>> function) {
		super(state, function);
	}

	/**
	 * Stores the given state for the given statement. This is a "forced"
	 * update, without performing any lattice operation if a mapping for the
	 * given expression already exists.
	 * 
	 * @param st    the statement whose state needs to be set
	 * @param state the state to set
	 * 
	 * @return the previous state mapped to {@code expression}, or {@code null}
	 */
	public AnalysisState<A, H, V> put(Statement st, AnalysisState<A, H, V> state) {
		return function.put(st, state);
	}

	@Override
	public StatementStore<A, H, V> top() {
		return new StatementStore<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public StatementStore<A, H, V> bottom() {
		return new StatementStore<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}

	@Override
	protected StatementStore<A, H, V> mk(AnalysisState<A, H, V> lattice,
			Map<Statement, AnalysisState<A, H, V>> function) {
		return new StatementStore<>(lattice, function);
	}
}
