package inf.ed.graph.quantified;

import inf.ed.graph.structure.adaptor.TypedEdge;
import inf.ed.graph.structure.adaptor.VertexInt;

import java.util.ArrayList;
import java.util.List;

public class NegationPath {

	private List<VertexInt> vertices;
	private List<TypedEdge> edges;
	private boolean freeEnding;

	public NegationPath() {
		this.vertices = new ArrayList<VertexInt>();
		this.edges = new ArrayList<TypedEdge>();
		this.freeEnding = true;
	}

	public void addVertex(VertexInt v) {
		vertices.add(v);
	}

	public void addEdge(TypedEdge e) {
		edges.add(e);
	}

	public VertexInt getNextVertex(VertexInt v) {
		int index = vertices.indexOf(v) + 1;
		if (index < vertices.size()) {
			return vertices.get(index);
		}
		return null;
	}

	public boolean hasNext(VertexInt v) {
		return vertices.indexOf(v) + 1 < vertices.size();
	}

	public VertexInt getStartVertex() {
		return vertices.get(0);
	}

	public TypedEdge getNextEdge(VertexInt v) {
		for (TypedEdge te : edges) {
			if (te.from().equals(v)) {
				return te;
			}
		}
		return null;
	}

	public boolean isFreeEnding() {
		return freeEnding;
	}

	public void setNotFreeEnding() {
		freeEnding = false;
	}

	@Override
	public String toString() {
		return "negaPath vertices=" + vertices + "; \nnegaPath edges=" + edges
				+ ", \nnegaPath freeEnding=" + freeEnding + "";
	}
}
