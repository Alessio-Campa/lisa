package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 * @param <T> the type of {@link TypeDomain} embedded in this state
 */
public class SimpleAbstractState<H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends BaseLattice<SimpleAbstractState<H, V, T>>
		implements AbstractState<SimpleAbstractState<H, V, T>, H, V, T> {

	/**
	 * The domain containing information regarding heap structures
	 */
	private final H heapState;

	/**
	 * The domain containing information regarding values of program variables
	 * and concretized memory locations
	 */
	private final V valueState;

	/**
	 * The domain containing runtime types information regarding runtime types
	 * of program variables and concretized memory locations
	 */
	private final T typeState;

	/**
	 * Builds a new abstract state.
	 * 
	 * @param heapState  the domain containing information regarding heap
	 *                       structures
	 * @param valueState the domain containing information regarding values of
	 *                       program variables and concretized memory locations
	 * @param typeState  the domain containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	public SimpleAbstractState(H heapState, V valueState, T typeState) {
		this.heapState = heapState;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	@Override
	public H getHeapState() {
		return heapState;
	}

	@Override
	public V getValueState() {
		return valueState;
	}

	@Override
	public T getTypeState() {
		return typeState;
	}

	@Override
	public SimpleAbstractState<H, V, T> assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.assign(id, expression, pp);
		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, pp);

		T type = typeState;
		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty()) {
			type = type.applySubstitution(heap.getSubstitution(), pp);
			value = value.applySubstitution(heap.getSubstitution(), pp);
		}

		for (ValueExpression expr : exprs) {
			type = type.assign(id, expr, pp);

			ExternalSet<Type> rt = type.getInferredRuntimeTypes();
			id.setRuntimeTypes(rt);
			expr.setRuntimeTypes(rt);

			value = value.assign(id, expr, pp);
		}

		return new SimpleAbstractState<>(heap, value, type);
	}

	@Override
	public SimpleAbstractState<H, V, T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.smallStepSemantics(expression, pp);
		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, pp);

		T type = typeState;
		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty()) {
			type = type.applySubstitution(heap.getSubstitution(), pp);
			value = value.applySubstitution(heap.getSubstitution(), pp);
		}

		for (ValueExpression expr : exprs) {
			type = type.smallStepSemantics(expr, pp);

			ExternalSet<Type> rt = type.getInferredRuntimeTypes();
			expr.setRuntimeTypes(rt);

			value = value.smallStepSemantics(expr, pp);
		}

		return new SimpleAbstractState<>(heap, value, type);
	}

	@Override
	public SimpleAbstractState<H, V, T> assume(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.assume(expression, pp);
		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, pp);

		T type = typeState;
		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty()) {
			type = type.applySubstitution(heap.getSubstitution(), pp);
			value = value.applySubstitution(heap.getSubstitution(), pp);
		}

		for (ValueExpression expr : exprs) {
			T tmp = type.smallStepSemantics(expr, pp);
			ExternalSet<Type> rt = tmp.getInferredRuntimeTypes();
			expr.setRuntimeTypes(rt);

			type = type.assume(expr, pp);
			value = value.assume(expr, pp);
		}

		return new SimpleAbstractState<>(heap, value, type);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		ExpressionSet<ValueExpression> rewritten = heapState.rewrite(expression, pp);
		Satisfiability typeResult = Satisfiability.BOTTOM;
		Satisfiability valueResult = Satisfiability.BOTTOM;
		for (ValueExpression expr : rewritten) {
			T tmp = typeState.smallStepSemantics(expr, pp);
			ExternalSet<Type> rt = tmp.getInferredRuntimeTypes();
			expr.setRuntimeTypes(rt);

			typeResult = typeResult.lub(typeState.satisfies(expr, pp));
			valueResult = valueResult.lub(valueState.satisfies(expr, pp));
		}
		return heapState.satisfies(expression, pp).glb(typeResult).glb(valueResult);
	}

	@Override
	public SimpleAbstractState<H, V, T> pushScope(ScopeToken scope) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.pushScope(scope),
				valueState.pushScope(scope),
				typeState.pushScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> popScope(ScopeToken scope) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.popScope(scope),
				valueState.popScope(scope),
				typeState.popScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> lubAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.lub(other.heapState),
				valueState.lub(other.valueState),
				typeState.lub(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> wideningAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.widening(other.heapState),
				valueState.widening(other.valueState),
				typeState.widening(other.typeState));
	}

	@Override
	public boolean lessOrEqualAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return heapState.lessOrEqual(other.heapState)
				&& valueState.lessOrEqual(other.valueState)
				&& typeState.lessOrEqual(other.typeState);
	}

	@Override
	public SimpleAbstractState<H, V, T> top() {
		return new SimpleAbstractState<>(heapState.top(), valueState.top(), typeState.top());
	}

	@Override
	public SimpleAbstractState<H, V, T> bottom() {
		return new SimpleAbstractState<>(heapState.bottom(), valueState.bottom(), typeState.bottom());
	}

	@Override
	public boolean isTop() {
		return heapState.isTop() && valueState.isTop() && typeState.isTop();
	}

	@Override
	public boolean isBottom() {
		return heapState.isBottom() && valueState.isBottom() && typeState.isBottom();
	}

	@Override
	public SimpleAbstractState<H, V, T> forgetIdentifier(Identifier id) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.forgetIdentifier(id),
				valueState.forgetIdentifier(id),
				typeState.forgetIdentifier(id));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapState == null) ? 0 : heapState.hashCode());
		result = prime * result + ((valueState == null) ? 0 : valueState.hashCode());
		result = prime * result + ((typeState == null) ? 0 : typeState.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleAbstractState<?, ?, ?> other = (SimpleAbstractState<?, ?, ?>) obj;
		if (heapState == null) {
			if (other.heapState != null)
				return false;
		} else if (!heapState.equals(other.heapState))
			return false;
		if (valueState == null) {
			if (other.valueState != null)
				return false;
		} else if (!valueState.equals(other.valueState))
			return false;
		if (typeState == null) {
			if (other.typeState != null)
				return false;
		} else if (!typeState.equals(other.typeState))
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		return new StateRepresentation(heapState.representation(), valueState.representation());
	}

	@Override
	public DomainRepresentation typeRepresentation() {
		return new StateRepresentation(heapState.representation(), typeState.representation());
	}

	/**
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	private static final class StateRepresentation extends DomainRepresentation {
		private final DomainRepresentation heap;
		private final DomainRepresentation value;

		private StateRepresentation(DomainRepresentation heap, DomainRepresentation value) {
			this.heap = heap;
			this.value = value;
		}

		@Override
		public String toJSONString() {
			String out = String.format("\"heap\" : [%s], \"value\" : [%s]", heap, value);
			return out.replaceAll(",]", "]");
		}

		@Override
		public String toDotString() {
			return "heap [[ " + heap + " ]]\nvalue [[ " + value + " ]]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((heap == null) ? 0 : heap.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StateRepresentation other = (StateRepresentation) obj;
			if (heap == null) {
				if (other.heap != null)
					return false;
			} else if (!heap.equals(other.heap))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <D> D getDomainInstance(Class<D> domain) {
		if (domain.isAssignableFrom(getClass()))
			return (D) this;

		D di = heapState.getDomainInstance(domain);
		if (di != null)
			return di;

		di = typeState.getDomainInstance(domain);
		if (di != null)
			return di;

		return valueState.getDomainInstance(domain);
	}
}
