package it.unive.lisa.analysis.symbols;

import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.util.collections.CastIterable;
import java.util.Collections;
import java.util.Set;

/**
 * A {@link SetLattice} of {@link Symbol}s to be used as aliases.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Aliases extends SetLattice<Aliases, Symbol> {

	private static final Aliases TOP = new Aliases();
	private static final Aliases BOTTOM = new Aliases(Collections.emptySet(), false);

	private final boolean isTop;

	/**
	 * Builds an empty set of aliases, representing the top of the lattice.
	 */
	public Aliases() {
		this(Collections.emptySet(), true);
	}

	/**
	 * Builds the set of aliases.
	 * 
	 * @param symbol the only symbol contained in this set
	 */
	public Aliases(Symbol symbol) {
		this(Collections.singleton(symbol), false);
	}

	/**
	 * Builds the set of aliases.
	 * 
	 * @param symbols the symbols contained in this set
	 */
	public Aliases(Set<Symbol> symbols) {
		this(symbols, false);
	}

	private Aliases(Set<Symbol> symbols, boolean isTop) {
		super(symbols);
		this.isTop = isTop;
	}

	@Override
	public Aliases top() {
		return TOP;
	}

	@Override
	public Aliases bottom() {
		return BOTTOM;
	}

	@Override
	protected Aliases mk(Set<Symbol> set) {
		return new Aliases(set);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTop ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Aliases other = (Aliases) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	/**
	 * Yields an iterable over the elements of this object casted to the given
	 * type.
	 * 
	 * @param <T>  the type to cast the elements to
	 * @param type the class to which the elements of this object should be
	 *                 casted to
	 * 
	 * @return a {@link CastIterable} that casts the elements
	 */
	public <T extends Symbol> Iterable<T> castElements(Class<T> type) {
		return new CastIterable<>(this, type);
	}
}
