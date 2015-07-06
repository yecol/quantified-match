package inf.ed.graph.quantified;

import inf.ed.graph.structure.Edge;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.Vertex;
import inf.ed.graph.structure.auxiliary.Pair;
import inf.ed.graph.structure.auxiliary.Quantifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static inf.ed.graph.quantified.State.NULL_NODE;

public class VF2Tester<VQ extends Vertex, EQ extends Edge, VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(VF2Tester.class);

	public VF2Tester() {
	}

	public void rewriteQuery() {
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isSubgraphIsomorphic(Graph<VQ, EQ> g1, int v1, Graph<VG, EG> g2, int v2,
			Map<String, Quantifier> quantifiers, QuantifierCheckMatrix matrix) {

		State state = new State<VQ, EQ, VG, EG>(g1, v1, g2, v2, quantifiers, matrix);
		return match(state);
	}

	private boolean match(State s) {
		if (s.isGoal()) {

			log.info("find a match:" + s.getMatch().size());
			log.info(s.getMatch().toString());
			return true;

		}

		if (s.isDead()) {
			return false;
		}

		int n1 = NULL_NODE, n2 = NULL_NODE;
		Pair<Integer> next = null;
		boolean found = false;
		while ((next = s.nextPair(n1, n2)) != null) {
			n1 = next.x;
			n2 = next.y;
			if (s.isFeasiblePair(n1, n2)) {
				State copy = s.copy();
				copy.addPair(n1, n2);
				found = match(copy);
				// If we found a match, then don't bother backtracking as it
				// would be wasted effort.
				// if (!found)
				copy.backTrack();
			}
		}
		return found;
	}

}
