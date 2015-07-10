package inf.ed.graph.structure.auxiliary;

import inf.ed.graph.structure.adaptor.TypedEdge;

public class KeyGen {

	public static String getTypedEdgeKey(TypedEdge e) {
		return e.from().getID() + "-" + e.getAttr() + "-" + e.to().getID();
	}

	public static String getTypedEdgeKey(int from, int to, int attr) {
		return from + "-" + attr + "-" + to;
	}

	public static boolean isYagoAttrTypeOnly(int attr) {
		return attr < 50 && attr >= 10;
	}

	public static int getYagoKey(int attr) {
		return attr / 10000000;
	}

	public static boolean compareAttr(int attr1, int attr2) {
		if (attr1 > 100000000 && (attr2 >= 10 && attr2 < 50)) {
			return getYagoKey(attr1) == attr2;
		} else if ((attr1 >= 10 && attr1 < 50) && attr2 > 100000000) {
			return getYagoKey(attr2) == attr1;
		}
		return attr1 == attr2;
	}
}
