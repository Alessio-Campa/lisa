package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A strategy where the runtime types of the parameters of the call are
 * evaluated against the signature of a cfg: for each parameter, if at least one
 * of the runtime types of the actual parameter can be assigned to the type of
 * the formal parameter, then
 * {@link #matches(Call, Parameter[], Expression[], ExternalSet[])} returns
 * {@code true}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RuntimeTypesMatchingStrategy extends FixedOrderMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final RuntimeTypesMatchingStrategy INSTANCE = new RuntimeTypesMatchingStrategy();

	private RuntimeTypesMatchingStrategy() {
	}

	@Override
	protected boolean matches(Call call, int pos, Parameter formal, Expression actual, ExternalSet<Type> types) {
		return types.anyMatch(rt -> rt.canBeAssignedTo(formal.getStaticType()));
	}
}
