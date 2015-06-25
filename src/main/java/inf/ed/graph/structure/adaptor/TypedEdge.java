package inf.ed.graph.structure.adaptor;

import inf.ed.graph.structure.Edge;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.Vertex;

import java.io.Serializable;

public class TypedEdge implements Edge, Serializable {

	/**
	 * Directed edge.
	 */
	private static final long serialVersionUID = 1L;
	Vertex from;
	Vertex to;
	int attribute;

	public TypedEdge(Object from, Object to) {
		this.from = (Vertex) from;
		this.to = (Vertex) to;
	}

	public TypedEdge(Vertex from, Vertex to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean match(Object o) {
		if (o instanceof TypedEdge) {
			TypedEdge other = (TypedEdge) o;
			return this.attribute == other.getAttr();
		}

		else if (o instanceof OrthogonalEdge) {
			OrthogonalEdge other = (OrthogonalEdge) o;
			return this.attribute == other.getAttr();
		}

		throw new IllegalArgumentException("Unmatchable");
	}

	@Override
	public Vertex from() {
		return from;
	}

	@Override
	public Vertex to() {
		return to;
	}

	public int getAttr() {
		return this.attribute;
	}

	public void setAttr(int attr) {
		this.attribute = attr;
	}

	@Override
	public String toString() {
		return "dEdge [f=" + from.getID() + ", t=" + to.getID() + ", attr=" + this.attribute + " ]";
	}

}