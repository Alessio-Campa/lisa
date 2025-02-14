package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.outputs.DotGraph;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unive.lisa.outputs.HtmlGraphNavigator;
import it.unive.lisa.outputs.JsonGraph;
import it.unive.lisa.program.cfg.statement.Statement;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A generic graph, backed by an {@link AdjacencyMatrix}.<br>
 * <br>
 * Note that this class does not define {@link #equals(Object)} nor
 * {@link #hashCode()}, since we leave the decision to be unique instances to
 * implementers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of {@link Node}s in this graph
 * @param <E> the type of {@link Edge}s in this graph
 * @param <G> the type of this graph
 */
public abstract class Graph<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>> {

	/**
	 * The adjacency matrix of this graph, mapping nodes to the collection of
	 * edges attached to it.
	 */
	protected final AdjacencyMatrix<N, E, G> adjacencyMatrix;

	/**
	 * The nodes of this graph that are entrypoints, that is, that can be
	 * executed from other graphs.
	 */
	protected final Collection<N> entrypoints;

	/**
	 * Builds the graph.
	 */
	protected Graph() {
		this.adjacencyMatrix = new AdjacencyMatrix<>();
		this.entrypoints = new HashSet<>();
	}

	/**
	 * Builds the graph.
	 * 
	 * @param entrypoints     the nodes of this graph that will be reachable
	 *                            from other graphs
	 * @param adjacencyMatrix the matrix containing all the nodes and the edges
	 *                            that will be part of this graph
	 */
	protected Graph(Collection<N> entrypoints, AdjacencyMatrix<N, E, G> adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
		this.entrypoints = entrypoints;
	}

	/**
	 * Clones the given graph.
	 * 
	 * @param other the original graph
	 */
	protected Graph(G other) {
		this.adjacencyMatrix = new AdjacencyMatrix<>(other.adjacencyMatrix);
		this.entrypoints = new ArrayList<>(other.entrypoints);
	}

	/**
	 * Yields the adjacency matrix backing this graph.
	 * 
	 * @return the matrix
	 */
	public AdjacencyMatrix<N, E, G> getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	/**
	 * Yields the nodes of this graph that are entrypoints, that is, that can be
	 * executed from other graphs. This usually contains the first node of this
	 * graph, but might also contain other ones.
	 * 
	 * @return the entrypoints of this graph.
	 */
	public final Collection<N> getEntrypoints() {
		return entrypoints;
	}

	/**
	 * Yields the set of nodes of this graph.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<N> getNodes() {
		return adjacencyMatrix.getNodes();
	}

	/**
	 * Yields the set of edges of this graph.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<E> getEdges() {
		return adjacencyMatrix.getEdges();
	}

	/**
	 * Adds the given node to the set of nodes, optionally setting that as root.
	 * This is equivalent to invoking {@link #addNode(Node, boolean)} with
	 * {@code false} as second parameter.
	 * 
	 * @param node the node to add
	 */
	public final void addNode(N node) {
		addNode(node, false);
	}

	/**
	 * Adds the given node to the set of nodes, optionally marking this as
	 * entrypoint (that is, reachable executable from other graphs). The first
	 * node of a graph should always be marked as entrypoint. Besides, nodes
	 * that might be reached through jumps from external graphs should be marked
	 * as entrypoints as well.
	 * 
	 * @param node       the node to add
	 * @param entrypoint if {@code true} causes the given node to be considered
	 *                       as an entrypoint.
	 */
	public final void addNode(N node, boolean entrypoint) {
		adjacencyMatrix.addNode(node);
		if (entrypoint)
			this.entrypoints.add(node);
	}

	/**
	 * Adds an edge to this graph.
	 * 
	 * @param edge the edge to add
	 * 
	 * @throws UnsupportedOperationException if the source or the destination of
	 *                                           the given edge are not part of
	 *                                           this graph
	 */
	public void addEdge(E edge) {
		adjacencyMatrix.addEdge(edge);
	}

	/**
	 * Yields the total number of nodes of this graph.
	 * 
	 * @return the number of nodes
	 */
	public final int getNodesCount() {
		return getNodes().size();
	}

	/**
	 * Yields the total number of edges of this graph.
	 * 
	 * @return the number of edges
	 */
	public final int getEdgesCount() {
		return getEdges().size();
	}

	/**
	 * Yields the edge connecting the two given nodes, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two nodes is
	 * not inside this graph.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	public final E getEdgeConnecting(N source, N destination) {
		return adjacencyMatrix.getEdgeConnecting(source, destination);
	}

	/**
	 * Yields the ingoing edges to the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of ingoing edges
	 */
	public final Collection<E> getIngoingEdges(N node) {
		return adjacencyMatrix.getIngoingEdges(node);
	}

	/**
	 * Yields the outgoing edges from the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of outgoing edges
	 */
	public final Collection<E> getOutgoingEdges(N node) {
		return adjacencyMatrix.getOutgoingEdges(node);
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this control flow
	 * graph going from the given node to such node. Yields {@code null} if the
	 * node is not in this graph.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers
	 */
	public final Collection<N> followersOf(N node) {
		return adjacencyMatrix.followersOf(node);
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this control
	 * flow graph going from such node to the given one. Yields {@code null} if
	 * the node is not in this graph.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors
	 */
	public final Collection<N> predecessorsOf(N node) {
		return adjacencyMatrix.predecessorsOf(node);
	}

	/**
	 * Dumps the content of this graph in the given writer, formatted as a dot
	 * file.
	 * 
	 * @param writer the writer where the content will be written
	 * 
	 * @throws IOException if an exception happens while writing something to
	 *                         the given writer
	 */
	public void dump(Writer writer) throws IOException {
		dumpJSON(writer, node -> "");
	}

	/**
	 * Dumps the content of this graph in the given writer, formatted as a dot
	 * file. The content of each vertex will be enriched by invoking
	 * labelGenerator on the vertex itself, to obtain an extra description to be
	 * concatenated with the standard call to the vertex's {@link #toString()}.
	 * 
	 * @param writer         the writer where the content will be written
	 * @param labelGenerator the function used to generate extra labels
	 * 
	 * @throws IOException if an exception happens while writing something to
	 *                         the given writer
	 */
	public void dumpDot(Writer writer, Function<N, String> labelGenerator) throws IOException {
		toDot(labelGenerator).dump(writer);
	}

	public void dumpJSON(Writer writer, Function<N, String> labelGenerator) throws IOException {
		toJson(labelGenerator).dump(writer);
	}

	public String getJsonString(Function<N, String> labelGenerator){
		return toJson(labelGenerator).toString();
	}

	/**
	 * Converts this graph to a {@link DotGraph} instance.
	 * 
	 * @param labelGenerator the generator that the {@link DotGraph} will use to
	 *                           enrich node labels
	 * 
	 * @return the converted {@link DotGraph}
	 */
	protected abstract DotGraph<N, E, G> toDot(Function<N, String> labelGenerator);

	protected abstract JsonGraph<N, E, G> toJson(Function<N, String> labelGenerator);

	/**
	 * Checks if this graph is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * 
	 * @param graph the other graph
	 * 
	 * @return {@code true} if this graph and the given one are effectively
	 *             equals
	 */
	public boolean isEqualTo(G graph) {
		if (this == graph)
			return true;
		if (graph == null)
			return false;
		if (getClass() != graph.getClass())
			return false;
		if (entrypoints == null) {
			if (graph.entrypoints != null)
				return false;
		} else if (!entrypoints.equals(graph.entrypoints))
			return false;
		if (adjacencyMatrix == null) {
			if (graph.adjacencyMatrix != null)
				return false;
		} else if (!adjacencyMatrix.equals(graph.adjacencyMatrix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return adjacencyMatrix.toString();
	}

	/**
	 * Simplifies the adjacency matrix beneath this graph, removing all nodes
	 * that are instances of {@code <T>} and rewriting the edge set accordingly.
	 * This method will throw an {@link UnsupportedOperationException} if one of
	 * the nodes being simplified has an outgoing edge that is not simplifiable,
	 * according to {@link Edge#canBeSimplified()}.
	 *
	 * @param target        the class of the {@link Node} that needs to be
	 *                          simplified
	 * @param removedEdges  the collections of edges that got removed during the
	 *                          simplification, filled by this method (the
	 *                          collection will be cleared before simplifying)
	 * @param replacedEdges the map of edges that got replaced during the
	 *                          simplification, filled by this method (the map
	 *                          will be cleared before simplifying); each entry
	 *                          refers to a single simplified edge, and is in
	 *                          the form
	 *                          {@code <<ingoing removed, outgoing removed>, added>}
	 * 
	 * @return the set of nodes that have been simplified
	 * 
	 * @throws UnsupportedOperationException if there exists at least one node
	 *                                           being simplified with an
	 *                                           outgoing non-simplifiable edge
	 */
	protected Set<N> simplify(Class<? extends N> target, Collection<E> removedEdges,
			Map<Pair<E, E>, E> replacedEdges) {
		Set<N> targets = getNodes().stream().filter(k -> target.isAssignableFrom(k.getClass()))
				.collect(Collectors.toSet());
		targets.forEach(this::preSimplify);
		adjacencyMatrix.simplify(targets, entrypoints, removedEdges, replacedEdges);
		return targets;
	}

	/**
	 * Callback that is invoked on a node before simplifying it.
	 * 
	 * @param node the node about to be simplified
	 */
	protected void preSimplify(N node) {
		// nothing to do, but subclasses might redefine
	}

	/**
	 * Accepts the given {@link GraphVisitor}. This method first invokes
	 * {@link GraphVisitor#visit(Object, Graph)} on this graph, and then
	 * proceeds by first invoking
	 * {@link GraphVisitor#visit(Object, Graph, Node)} on all the nodes in the
	 * order they are returned by {@link #getNodes()}, and later invoking
	 * {@link GraphVisitor#visit(Object, Graph, Edge)} on all the edges in the
	 * order they are returned by {@link #getEdges()}. The visiting stops at the
	 * first of such calls that return {@code false}.
	 * 
	 * @param <V>     the type of auxiliary tool that {@code visitor} can use
	 * @param visitor the visitor that is visiting the {@link Graph} containing
	 *                    this graph
	 * @param tool    the auxiliary tool that {@code visitor} can use
	 */
	@SuppressWarnings("unchecked")
	public <V> void accept(GraphVisitor<G, N, E, V> visitor, V tool) {
		if (!visitor.visit(tool, (G) this))
			return;

		for (N node : getNodes())
			if (!node.accept(visitor, tool))
				return;

		for (E edge : getEdges())
			if (!edge.accept(visitor, tool))
				return;
	}
}
