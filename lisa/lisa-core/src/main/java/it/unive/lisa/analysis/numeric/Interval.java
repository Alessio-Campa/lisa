package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.Multiplication;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * The overflow-insensitive interval abstract domain, approximating integer
 * values as the minimum integer interval containing them. It is implemented as
 * a {@link BaseNonRelationalValueDomain}, handling top and bottom values for
 * the expression evaluation and bottom values for the expression
 * satisfiability. Top and bottom cases for least upper bounds, widening and
 * less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Interval extends BaseNonRelationalValueDomain<Interval> {

	private static final Interval ZERO = new Interval(IntInterval.ZERO);
	private static final Interval TOP = new Interval(IntInterval.INFINITY);
	private static final Interval BOTTOM = new Interval(null);

	/**
	 * The interval represented by this domain element.
	 */
	public final IntInterval interval;

	/**
	 * Builds the interval.
	 * 
	 * @param interval the underlying {@link IntInterval}
	 */
	public Interval(IntInterval interval) {
		this.interval = interval;
	}

	/**
	 * Builds the interval.
	 * 
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Interval(MathNumber low, MathNumber high) {
		this(new IntInterval(low, high));
	}

	/**
	 * Builds the interval.
	 * 
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Interval(int low, int high) {
		this(new IntInterval(low, high));
	}

	/**
	 * Builds the top interval.
	 */
	public Interval() {
		this(IntInterval.INFINITY);
	}

	@Override
	public Interval top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return interval != null && interval.isInfinity();
	}

	@Override
	public Interval bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return interval == null;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.BOTTOM_REPR;

		return new StringRepresentation(interval.toJSONString());
	}

	@Override
	protected Interval evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new Interval(new MathNumber(i), new MathNumber(i));
		}

		return top();
	}

	@Override
	protected Interval evalUnaryExpression(UnaryOperator operator, Interval arg, ProgramPoint pp) {
		if (operator == NumericNegation.INSTANCE)
			if (arg.isTop())
				return top();
			else
				return new Interval(arg.interval.mul(IntInterval.MINUS_ONE));
		else if (operator == StringLength.INSTANCE)
			return new Interval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		else
			return top();
	}

	private boolean is(int n) {
		return !isBottom() && interval.is(n);
	}

	@Override
	protected Interval evalBinaryExpression(BinaryOperator operator, Interval left, Interval right, ProgramPoint pp) {
		if (!(operator instanceof DivisionOperator) && (left.isTop() || right.isTop()))
			// with div, we can return zero or bottom even if one of the
			// operands is top
			return top();

		if (operator instanceof AdditionOperator)
			return new Interval(left.interval.plus(right.interval));
		else if (operator instanceof SubtractionOperator)
			return new Interval(left.interval.diff(right.interval));
		else if (operator instanceof Multiplication)
			if (left.is(0) || right.is(0))
				return ZERO;
			else
				return new Interval(left.interval.mul(right.interval));
		else if (operator instanceof DivisionOperator)
			if (right.is(0))
				return bottom();
			else if (left.is(0))
				return ZERO;
			else if (left.isTop() || right.isTop())
				return top();
			else
				return new Interval(left.interval.div(right.interval, false, false));
		return top();
	}

	@Override
	protected Interval lubAux(Interval other) throws SemanticException {
		MathNumber newLow = interval.getLow().min(other.interval.getLow());
		MathNumber newHigh = interval.getHigh().max(other.interval.getHigh());
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	protected Interval glbAux(Interval other) {
		MathNumber newLow = interval.getLow().max(other.interval.getLow());
		MathNumber newHigh = interval.getHigh().min(other.interval.getHigh());

		if (newLow.compareTo(newHigh) > 0)
			return bottom();
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	protected Interval wideningAux(Interval other) throws SemanticException {
		MathNumber newLow, newHigh;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			newHigh = MathNumber.PLUS_INFINITY;
		else
			newHigh = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			newLow = MathNumber.MINUS_INFINITY;
		else
			newLow = interval.getLow();

		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	protected boolean lessOrEqualAux(Interval other) throws SemanticException {
		return other.interval.includes(interval);
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Interval left, Interval right,
			ProgramPoint pp) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		if (operator == ComparisonEq.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			else if (left.interval.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(ComparisonLt.INSTANCE, right, left, pp);
		else if (operator == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(ComparisonLe.INSTANCE, right, left, pp);
		else if (operator == ComparisonLe.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.interval.getHigh().compareTo(right.interval.getLow()) <= 0);
			// we might have a singleton as glb if the two intervals share a
			// bound
			if (glb.interval.isSingleton() && left.interval.getHigh().compareTo(right.interval.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonLt.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.interval.getHigh().compareTo(right.interval.getLow()) < 0);
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonNe.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((interval == null) ? 0 : interval.hashCode());
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
		Interval other = (Interval) obj;
		if (interval == null) {
			if (other.interval != null)
				return false;
		} else if (!interval.equals(other.interval))
			return false;
		return true;
	}

	@Override
	protected ValueEnvironment<Interval> assumeBinaryExpression(
			ValueEnvironment<Interval> environment, BinaryOperator operator, ValueExpression left,
			ValueExpression right, ProgramPoint pp) throws SemanticException {

		Identifier id;
		Interval eval;
		boolean rightIsExpr;
		if (left instanceof Identifier) {
			eval = eval(right, environment, pp);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, pp);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		if (eval.isBottom())
			return environment.bottom();

		boolean lowIsMinusInfinity = eval.interval.lowIsMinusInfinity();
		Interval low_inf = new Interval(eval.interval.getLow(), MathNumber.PLUS_INFINITY);
		Interval lowp1_inf = new Interval(eval.interval.getLow().add(MathNumber.ONE), MathNumber.PLUS_INFINITY);
		Interval inf_high = new Interval(MathNumber.MINUS_INFINITY, eval.interval.getHigh());
		Interval inf_highm1 = new Interval(MathNumber.MINUS_INFINITY, eval.interval.getHigh().subtract(MathNumber.ONE));

		if (operator == ComparisonEq.INSTANCE)
			return environment.putState(id, eval);
		else if (operator == ComparisonGe.INSTANCE)
			if (rightIsExpr)
				return lowIsMinusInfinity ? environment : environment.putState(id, low_inf);
			else
				return environment.putState(id, inf_high);
		else if (operator == ComparisonGt.INSTANCE)
			if (rightIsExpr)
				return lowIsMinusInfinity ? environment : environment.putState(id, lowp1_inf);
			else
				return environment.putState(id, lowIsMinusInfinity ? eval : inf_highm1);
		else if (operator == ComparisonLe.INSTANCE)
			if (rightIsExpr)
				return environment.putState(id, inf_high);
			else
				return lowIsMinusInfinity ? environment : environment.putState(id, low_inf);
		else if (operator == ComparisonLt.INSTANCE)
			if (rightIsExpr)
				return environment.putState(id, lowIsMinusInfinity ? eval : inf_highm1);
			else
				return lowIsMinusInfinity ? environment : environment.putState(id, lowp1_inf);
		else
			return environment;
	}
}
