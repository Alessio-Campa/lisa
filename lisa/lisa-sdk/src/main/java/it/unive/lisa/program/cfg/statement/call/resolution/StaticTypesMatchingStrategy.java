package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A strategy where the static types of the parameters of the call are evaluated
 * against the signature of a cfg: for each parameter, if the static type of the
 * actual parameter can be assigned to the type of the formal parameter, then
 * {@link #matches(Call, Parameter[], Expression[], ExternalSet[])} return
 * {@code true}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StaticTypesMatchingStrategy extends FixedOrderMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final StaticTypesMatchingStrategy INSTANCE = new StaticTypesMatchingStrategy();

	private StaticTypesMatchingStrategy() {
	}

	@Override
	protected boolean matches(Call call, int pos, Parameter formal, Expression actual, ExternalSet<Type> types) {
		return actual.getStaticType().canBeAssignedTo(formal.getStaticType());
	}
}
