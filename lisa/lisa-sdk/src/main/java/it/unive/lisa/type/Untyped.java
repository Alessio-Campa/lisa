package it.unive.lisa.type;

import it.unive.lisa.caches.Caches;
import java.util.Collection;

/**
 * The untyped type, corresponding to an unknown/untyped type. This type is used
 * as default when no type information is provided for LiSA constructs (e.g.,
 * expression, variable, CFG return type). It implements the singleton design
 * pattern, that is the instances of this type are unique. The unique instance
 * of this type can be retrieved by {@link Untyped#INSTANCE}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Untyped implements Type {

	/**
	 * Unique instance of Untyped type.
	 */
	public static final Untyped INSTANCE = new Untyped();

	private Untyped() {
	}

	@Override
	public String toString() {
		return "untyped";
	}

	@Override
	public final boolean equals(Object other) {
		return other instanceof Untyped;
	}

	@Override
	public final int hashCode() {
		return Untyped.class.hashCode();
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other == this;
	}

	@Override
	public Type commonSupertype(Type other) {
		return this;
	}

	@Override
	public Collection<Type> allInstances() {
		return Caches.types().mkUniversalSet();
	}
}
