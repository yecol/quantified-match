package inf.ed.graph.structure.auxiliary;

import inf.ed.graph.structure.adaptor.TypedEdge;

public class KeyGen {

	public static String getTypedEdgeKey(TypedEdge e) {
		return e.from().getID() + "-" + e.getAttr() + "-" + e.to().getID();
	}

	public static String getTypedEdgeKey(int from, int to, int attr) {
		return from + "-" + attr + "-" + to;
	}
	

	
	
}
