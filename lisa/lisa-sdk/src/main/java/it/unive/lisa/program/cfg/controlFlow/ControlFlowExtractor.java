package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.algorithms.Dominators;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * An extractor of {@link ControlFlowStructure}s from {@link CFG}s. It uses
 * {@link Dominators} to extract {@link Loop}s, and a graph visiting heuristics
 * to find {@link IfThenElse}s.<br>
 * <br>
 * Extracting control flows should be a last-resort: if the cfg contains
 * arbitrary jumps (like {@code goto, break, continue, ...}) the aforementioned
 * algorithms will fail to properly infer some of the structures. It is always
 * preferable to have frontends define each structure relying on source code
 * information.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ControlFlowExtractor {

	private final CFG target;

	private final Collection<ControlFlowStructure> extracted;

	/**
	 * Builds the extractor.
	 * 
	 * @param target the cfg whose control flows structures are to be extracted
	 */
	public ControlFlowExtractor(CFG target) {
		this.target = target;
		this.extracted = new ArrayList<>();
	}

	/**
	 * Runs the algorithms for extracting {@link ControlFlowStructure}s.
	 * 
	 * @return the collection of extracted structures
	 */
	public Collection<ControlFlowStructure> extract() {
		extracted.clear();

		LinkedList<Statement> conditionals = new LinkedList<>();
		target.accept(new ConditionalsExtractor(), conditionals);

		if (conditionals.isEmpty())
			return extracted;

		// first, we find the loops using back-edges:
		// https://www.cs.utexas.edu/~pingali/CS375/2010Sp/lectures/LoopOptimizations.pdf
		// http://pages.cs.wisc.edu/~fischer/cs701.f14/finding.loops.html
		Map<Statement, ControlFlowStructure> result = new HashMap<>();
		Map<Statement, Set<Statement>> dominators = new Dominators<CFG, Statement, Edge>().build(target);
		for (Statement conditional : conditionals)
			for (Statement pred : target.predecessorsOf(conditional))
				if (dominators.get(pred).contains(conditional))
					new LoopReconstructor(conditional, pred, result).build();

		// now we scan for if statements
		for (Statement conditional : conditionals)
			if (!result.containsKey(conditional))
				new IfReconstructor(conditional, result).build();

		extracted.addAll(result.values());
		return extracted;
	}

	private final class LoopReconstructor {
		private final Statement conditional;
		private final Statement tail;

		private final Map<Statement, ControlFlowStructure> computed;

		private LoopReconstructor(Statement conditional, Statement tail,
				Map<Statement, ControlFlowStructure> computed) {
			this.conditional = conditional;
			this.tail = tail;
			this.computed = computed;
		}

		private void build() {
			AdjacencyMatrix<Statement, Edge, CFG> body = new AdjacencyMatrix<>();

			// with empty loops, we can skip the whole reasoning
			if (tail != conditional) {
				WorkingSet<Edge> ws = VisitOnceWorkingSet.mk(FIFOWorkingSet.mk());
				target.getIngoingEdges(tail).forEach(ws::push);
				body.addNode(tail);
				while (!ws.isEmpty()) {
					// TODO this does not take into account arbitrary jumps
					// inside the loop body
					Edge current = ws.pop();
					if (current.getSource() != conditional) {
						body.addNode(current.getSource());
						body.addEdge(current);
						target.getIngoingEdges(current.getSource()).forEach(ws::push);
					}
				}
			}

			Edge exit = findExitEdge(body);
			computed.put(conditional, new Loop(target.getAdjacencyMatrix(),
					conditional, exit.getDestination(), body.getNodes()));
		}

		private Edge findExitEdge(AdjacencyMatrix<Statement, Edge, CFG> body) {
			Edge exit = null;
			for (Edge out : target.getOutgoingEdges(conditional))
				// in empty loops, the conditional is a follower of itself
				// and it is not in the body of the loop, so we have to
				// manually exclude it
				if (out.getDestination() != conditional && !body.containsNode(out.getDestination())) {
					exit = out;
					break;
				}

			return exit;
		}
	}

	private final class IfReconstructor {
		private final Statement conditional;

		private final Edge trueEdgeStartingEdge;
		private final Edge falseEdgeStartingEdge;

		private final AdjacencyMatrix<Statement, Edge, CFG> trueBranch;
		private final AdjacencyMatrix<Statement, Edge, CFG> falseBranch;

		private final Map<Statement, ControlFlowStructure> computed;

		private IfReconstructor(Statement conditional, Map<Statement, ControlFlowStructure> computed) {
			this.conditional = conditional;
			this.computed = computed;

			trueBranch = new AdjacencyMatrix<>();
			falseBranch = new AdjacencyMatrix<>();

			Iterator<Edge> it = target.getOutgoingEdges(conditional).iterator();
			Edge trueEdge = it.next();
			Edge falseEdge = it.next();

			if (trueEdge instanceof FalseEdge) {
				Edge tmp = trueEdge;
				trueEdge = falseEdge;
				falseEdge = tmp;
			}

			trueEdgeStartingEdge = trueEdge;
			falseEdgeStartingEdge = falseEdge;
		}

		private ControlFlowStructure build() {
			if (computed.containsKey(conditional))
				return computed.get(conditional);

			Edge trueNext = trueEdgeStartingEdge;
			Edge falseNext = falseEdgeStartingEdge;
			Edge trueLast = null;
			Edge falseLast = null;

			boolean first = true;
			ControlFlowStructure struct;
			while (true) {
				if (trueNext != null && falseNext != null
						&& (struct = tryClose(trueNext.getDestination(), falseNext.getDestination())) != null)
					return store(struct);

				boolean trueCond = trueNext != null && isConditional(target, trueNext.getDestination()),
						trueCondProcessed = false;
				boolean falseCond = falseNext != null && isConditional(target, falseNext.getDestination()),
						falseCondProcessed = false;

				// we can move on one branch at a time with no penalties:
				// the tryClose will inspect also for join points in the
				// middle of the branches
				if (trueCond) {
					struct = computed.containsKey(trueNext.getDestination())
							? computed.get(trueNext.getDestination())
							: new IfReconstructor(trueNext.getDestination(), computed).build();
					AdjacencyMatrix<Statement, Edge, CFG> completeStructure = struct.getCompleteStructure();
					trueBranch.mergeWith(completeStructure);
					trueBranch.addEdge(trueNext);

					if (struct.getFirstFollower() == null)
						// the execution ends inside the body of the
						// conditional, we cannot proceed
						trueNext = null;
					else {
						Collection<Edge> ins = completeStructure.getIngoingEdges(struct.getFirstFollower());
						if (ins.isEmpty())
							throw new IllegalStateException(
									"The first follower of " + struct + " does not have ingoing edges");

						// we just take one of the edges, we do not care
						trueNext = ins.iterator().next();
					}
					trueCondProcessed = true;
				} else if (falseCond) {
					struct = computed.containsKey(falseNext.getDestination())
							? computed.get(falseNext.getDestination())
							: new IfReconstructor(falseNext.getDestination(), computed).build();
					AdjacencyMatrix<Statement, Edge, CFG> completeStructure = struct.getCompleteStructure();
					falseBranch.mergeWith(completeStructure);
					falseBranch.addEdge(falseNext);

					if (struct.getFirstFollower() == null)
						// the execution ends inside the body of the
						// conditional, we cannot proceed
						falseNext = null;
					else {
						Collection<Edge> ins = completeStructure.getIngoingEdges(struct.getFirstFollower());
						if (ins.isEmpty())
							throw new IllegalStateException(
									"The first follower of " + struct + " does not have ingoing edges");

						// we just take one of the edges, we do not care
						falseNext = ins.iterator().next();
					}
					falseCondProcessed = true;
				}

				Collection<Edge> trueOuts = null, falseOuts = null;
				if (!falseCondProcessed && trueNext != null && trueNext != trueLast) {
					// update only if we moved forward last time
					trueLast = trueNext;

					if (!trueCondProcessed) {
						trueBranch.addNode(trueNext.getDestination());
						if (!first)
							trueBranch.addEdge(trueNext);
					}

					trueOuts = target.getOutgoingEdges(trueNext.getDestination());
					if (!trueOuts.isEmpty())
						trueNext = trueOuts.iterator().next();
				} else
					trueOuts = null;

				if (!trueCondProcessed && falseNext != null && falseNext != falseLast) {
					// update only if we moved forward last time
					falseLast = falseNext;

					if (!falseCondProcessed) {
						falseBranch.addNode(falseNext.getDestination());
						if (!first)
							falseBranch.addEdge(falseNext);
					}

					falseOuts = target.getOutgoingEdges(falseNext.getDestination());
					if (!falseOuts.isEmpty())
						falseNext = falseOuts.iterator().next();
				} else
					falseOuts = null;

				if (!trueCondProcessed && !falseCondProcessed
						&& (trueOuts == null || trueOuts.isEmpty()) && (falseOuts == null || falseOuts.isEmpty()))
					// we reached the end of both branches: this is just a
					// conditional that goes on until the end of cfg
					return store(new IfThenElse(target.getAdjacencyMatrix(), conditional, null, trueBranch.getNodes(),
							falseBranch.getNodes()));

				first = false;
			}
		}

		private ControlFlowStructure tryClose(Statement trueNext, Statement falseNext) {
			if (falseBranch.containsNode(trueNext)) {
				// need to cut the extra part from the false branch
				falseBranch.removeFrom(trueNext);
				return new IfThenElse(target.getAdjacencyMatrix(), conditional, trueNext, trueBranch.getNodes(),
						falseBranch.getNodes());
			}

			if (trueBranch.containsNode(falseNext)) {
				// need to cut the extra part from the false branch
				trueBranch.removeFrom(falseNext);
				return new IfThenElse(target.getAdjacencyMatrix(), conditional, falseNext, trueBranch.getNodes(),
						falseBranch.getNodes());
			}

			if (trueNext.equals(falseNext))
				// this only holds for the symmetric if - same number of
				// statements in both branches
				return new IfThenElse(target.getAdjacencyMatrix(), conditional, falseNext, trueBranch.getNodes(),
						falseBranch.getNodes());

			return null;
		}

		private ControlFlowStructure store(ControlFlowStructure struct) {
			computed.put(struct.getCondition(), struct);
			return struct;
		}
	}

	private static boolean isConditional(CFG graph, Statement node) {
		Collection<Edge> out = graph.getOutgoingEdges(node);
		if (out.size() != 2)
			return false;

		Iterator<Edge> it = out.iterator();
		Edge first = it.next();
		Edge second = it.next();

		if (first instanceof TrueEdge && second instanceof FalseEdge)
			return true;

		if (second instanceof TrueEdge && first instanceof FalseEdge)
			return true;

		return false;
	}

	private static class ConditionalsExtractor implements GraphVisitor<CFG, Statement, Edge, Collection<Statement>> {

		@Override
		public boolean visit(Collection<Statement> tool, CFG graph) {
			return true;
		}

		@Override
		public boolean visit(Collection<Statement> tool, CFG graph, Statement node) {
			if (node instanceof Expression && ((Expression) node).getRootStatement() != node)
				// we only consider root statements
				return true;

			if (isConditional(graph, node))
				tool.add(node);

			return true;
		}

		@Override
		public boolean visit(Collection<Statement> tool, CFG graph, Edge edge) {
			return true;
		}
	}
}
