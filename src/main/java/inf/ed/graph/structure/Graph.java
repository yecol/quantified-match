package inf.ed.graph.structure;

import java.util.Map;
import java.util.Set;

public interface Graph<V extends Vertex, E extends Edge> {

	boolean addVertex(V vertex);

	boolean addEdge(E edge);

	E addEdge(V from, V to);

	int getRadius(V vertex);

	/**
	 * Compute longest shortest path (treat it as undirected graph)
	 */
	int getDiameter();

	Set<V> getChildren(V vertex);

	Set<V> getParents(V vertex);

	Set<V> getNeighbours(V vertex);

	Set<Integer> getChildren(int vertexID);

	Set<Integer> getParents(int vertexID);

	Set<Integer> getNeighbours(int vertexID);

	/**
	 * Print the graph with @code{limited} nodes.
	 */
	void display(int limit);

	int edgeSize();

	int vertexSize();

	V getVertex(int vID);

	/**
	 * Get a random vertex in the graph.
	 */
	V getRandomVertex();

	Set<E> allEdges();

	Map<Integer, V> allVertices();

	/**
	 * Get a subgraph from the current one, centring at @code{centre}, with
	 * radius @code{bound}.
	 * 
	 * @return subgraph
	 */
	Graph<V, E> getSubgraph(Class<V> vertexClass, Class<E> edgeClass, V center, int bound);
	
	Graph<V, E> copy();

	/**
	 * Load graph from files. Edges are stored in xxx.e, and nodes are in xxx.v.
	 * 
	 * @param filePathWithoutExtension
	 */
	boolean loadGraphFromVEFile(String filePathWithoutExtension, boolean isTypedEdge);

	/**
	 * Removes all the edges and vertices from this graph (optional operation).
	 */
	void clear();

	void clearEdges();

	boolean contains(int vertexID);

	boolean contains(V from, V to);

	boolean contains(int fromID, int toID);

	boolean contains(E edge);

	int degree(V vertex);

	Set<E> getEdges(V from, V to);

	E getEdge(V from, V to);

	E getEdge(int fromID, int toID);

	boolean hasCycles();

	boolean removeEdge(E edge);

	boolean removeEdge(V from, V to);

	boolean removeVertex(V vertex);

	/**
	 * Finalise the graph after using it. Especially for persistent process in
	 * NeoGraph.
	 */
	void finalizeGraph();
}
