package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

/**
 * An environment for a {@link NonRelationalTypeDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key. The runtime types computed for the last processed
 * expression are exposed through {@link #getInferredRuntimeTypes()} (and
 * {@link #getInferredDynamicType()} yields the lub of such types).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalTypeDomain} whose
 *                instances are mapped in this environment
 */
public class TypeEnvironment<T extends NonRelationalTypeDomain<T>>
		extends Environment<TypeEnvironment<T>, ValueExpression, T, T>
		implements TypeDomain<TypeEnvironment<T>> {

	private final T stack;

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public TypeEnvironment(T domain) {
		super(domain);
		this.stack = domain.bottom();
	}

	private TypeEnvironment(T domain, Map<Identifier, T> function, T stack) {
		super(domain, function);
		this.stack = stack;
	}

	@Override
	protected TypeEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new TypeEnvironment<>(lattice, function, stack);
	}

	@Override
	protected TypeEnvironment<T> copy() {
		return new TypeEnvironment<>(lattice, mkNewFunction(function), stack);
	}

	@Override
	protected Pair<T, T> eval(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		return Pair.of(eval, eval);
	}

	@Override
	protected TypeEnvironment<T> assignAux(Identifier id, ValueExpression expression, Map<Identifier, T> function,
			T value, T eval, ProgramPoint pp) {
		return new TypeEnvironment<>(lattice, function, value);
	}

	@Override
	public TypeEnvironment<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new TypeEnvironment<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	protected TypeEnvironment<T> assumeSatisfied(T eval) {
		return this;
	}

	@Override
	protected TypeEnvironment<T> glbAux(T lattice, Map<Identifier, T> function, TypeEnvironment<T> other)
			throws SemanticException {
		return new TypeEnvironment<>(lattice, function, stack.glb(other.stack));
	}

	@Override
	public TypeEnvironment<T> top() {
		return isTop() ? this : new TypeEnvironment<>(lattice.top(), null, lattice.top());
	}

	@Override
	public TypeEnvironment<T> bottom() {
		return isBottom() ? this : new TypeEnvironment<>(lattice.bottom(), null, lattice.bottom());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((stack == null) ? 0 : stack.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TypeEnvironment))
			return false;
		TypeEnvironment<?> other = (TypeEnvironment<?>) obj;
		if (stack == null) {
			if (other.stack != null)
				return false;
		} else if (!stack.equals(other.stack))
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ValueRepresentation(super.representation(), stack.representation());
	}

	private static class ValueRepresentation extends DomainRepresentation {

		private final DomainRepresentation map;
		private final DomainRepresentation stack;

		public ValueRepresentation(DomainRepresentation map, DomainRepresentation stack) {
			this.map = map;
			this.stack = stack;
		}

		@Override
		public String toJSONString() {
			if(Objects.equals(stack.toString(), "\"_|_\"") || Objects.equals(stack.toString(), "\"#TOP#\""))
				return map + "{\"type\" : \"typeEnvironment\", \"value\" : " + stack + "}";
			return map + "{\"type\" : \"typeEnvironment\", \"value\" : {" + stack + "}}";
		}

		@Override
		public String toDotString() {
			return map + "\n[stack: " + stack + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((stack == null) ? 0 : stack.hashCode());
			result = prime * result + ((map == null) ? 0 : map.hashCode());
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
			ValueRepresentation other = (ValueRepresentation) obj;
			if (stack == null) {
				if (other.stack != null)
					return false;
			} else if (!stack.equals(other.stack))
				return false;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			return true;
		}
	}

	@Override
	public ExternalSet<Type> getInferredRuntimeTypes() {
		return stack.getRuntimeTypes();
	}

	@Override
	public Type getInferredDynamicType() {
		ExternalSet<Type> types = stack.getRuntimeTypes();
		if (stack.isTop() || stack.isBottom() || types.isEmpty())
			return Untyped.INSTANCE;
		return types.reduce(types.first(), (result, t) -> result.commonSupertype(t));
	}
}
