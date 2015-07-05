package inf.ed.graph.quantified;

import static inf.ed.graph.quantified.State.NULL_NODE;
import inf.ed.graph.structure.Edge;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.Vertex;
import inf.ed.graph.structure.adaptor.TypedEdge;
import inf.ed.graph.structure.adaptor.VertexInt;
import inf.ed.graph.structure.auxiliary.Pair;
import inf.ed.graph.structure.auxiliary.Quantifier;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optimisation: check quantifiers in VF2
 * 
 * @author yecol
 *
 */
public class OptMatcher<VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(OptMatcher.class);

	QuantifiedPattern p;
	Graph<VG, EG> g;

	int v1;
	int v2;

	List<Int2IntMap> matches;// matches of pi graph of pattern.
	// Int2IntMap mapV2TypedEdgeCount;
	QuantifierCheckMatrix m;

	/* node v in G -> count of edge with u in Q, which u~>v */

	public OptMatcher() {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isIsomorphic(QuantifiedPattern p, int v1, Graph<VG, EG> g, int v2) {

		this.p = p;
		this.g = g;
		this.v1 = v1;
		this.v2 = v2;

		// this.mapV2TypedEdgeCount = new Int2IntOpenHashMap();
		this.matches = new ArrayList<Int2IntMap>();
		this.m = new QuantifierCheckMatrix(p);

		boolean valid = this.findMathesOfPI() && this.validateMatchesOfPi()
				&& this.checkNegatives() && this.validateMatchesOfPi();

		log.info(printMatches(matches));
		return valid;
	}

	private String printMatches(List<Int2IntMap> matches) {
		String ret = "match results. size = " + matches.size() + "\n";
		for (Int2IntMap s : matches) {
			ret += s.toString() + ", ";
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private boolean findMathesOfPI() {

		State initState = new State<VertexInt, TypedEdge, VG, EG>(p.getPI(), v1, g, v2,
				p.getQuantifiers(), m);
		return this.match(initState, matches);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean findNegativeMatches(Graph<VertexInt, TypedEdge> ngGraph,
			List<Int2IntMap> ngMatches, IntSet overlaps) {

		System.out.println("nggraph:");
		ngGraph.display(1000);
		log.info(overlaps.toString());

		for (Int2IntMap match : matches) {
			// if (i < 2) {
			State sn = new State(ngGraph, g, p.getQuantifiers(), m, match, overlaps);
			if (sn.needFurtherCheckNegationEdge()) {
				this.match(sn, ngMatches);
			}
		}
		return !ngMatches.isEmpty();
	}

	/**
	 * check the matches of pi pattern with quantifiers.
	 * 
	 * @return true if need to continue.
	 */
	private boolean validateMatchesOfPi() {

		log.debug("before validate matches of Pi:" + matches.size());

		Set<Integer> checked = new HashSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.addAll(p.getBottomVertices(p.getPI()));

		while (!queue.isEmpty()) {
			int u = queue.poll();
			for (int fu : p.getPI().getParents(u)) {
				// current deal with edge fu->u in Q
				Quantifier quantifier = p.getQuantifierWithEdge(fu, u);
				boolean changedfv = this.filterMatches(fu, u, quantifier);
				// if changed the count of fv (mapping of fu).
				if ((checked.contains(fu) && changedfv) || !checked.contains(fu)) {
					queue.add(fu);
				}
			}
			checked.add(u);
		}

		log.debug("after validate matches of Pi:" + matches.size());

		return !matches.isEmpty();
	}

	/**
	 * 
	 * check and filter matches with quantifier.
	 * 
	 * @param ufromID
	 *            fromID in Q
	 * @param utoID
	 *            toID in Q
	 * @param quantifier
	 *            the expected valid quantifier
	 * @return if filtered any v, returns true, else false;
	 */
	private boolean filterMatches(int ufromID, int utoID, Quantifier quantifier) {

		if (quantifier.isExistential()) {
			// Do not need to filter anything.
			return false;
		}

		Int2ObjectMap<IntSet> aggregatedMatchesByFv = new Int2ObjectOpenHashMap<IntSet>();
		for (Int2IntMap match : matches) {
			int fv = match.get(ufromID);
			int tv = match.get(utoID);
			IntSet maps = aggregatedMatchesByFv.getOrDefault(fv, new IntOpenHashSet());
			maps.add(tv);
			aggregatedMatchesByFv.put(fv, maps);
		}

		IntSet removedTargetMapping = new IntOpenHashSet();

		if (quantifier.isPercentage()) {
			int edgeAttr = p.getGraph().getEdge(ufromID, utoID).getAttr();
			for (int fv : aggregatedMatchesByFv.keySet()) {
				if (!quantifier.isValid(aggregatedMatchesByFv.get(fv).size(),
						m.getEdgeCount(fv, edgeAttr))) {
					// remove fv;
					// log.debug(quantifier.toString() +
					// " not valid percentage, remove uFromID="
					// + ufromID + " matching: fv=" + fv);
					removedTargetMapping.add(fv);
				}
			}
		}

		else if (quantifier.isCount()) {
			for (int fv : aggregatedMatchesByFv.keySet()) {
				if (!quantifier.isValid(aggregatedMatchesByFv.get(fv).size())) {
					// remove fv;
					// log.debug(quantifier.toString() +
					// " not valid count, remove uFromID=" + ufromID
					// + " matching: fv=" + fv);
					removedTargetMapping.add(fv);
				}
			}

		}

		Iterator<Int2IntMap> it = matches.iterator();
		while (it.hasNext()) {
			if (removedTargetMapping.contains(it.next().get(ufromID))) {
				it.remove();
			}
		}

		return !removedTargetMapping.isEmpty();
	}

	private IntSet getOverlapVertices(Graph<VertexInt, TypedEdge> ngGraph,
			Graph<VertexInt, TypedEdge> pi) {
		IntSet overlaps = new IntOpenHashSet();
		for (int ngv : ngGraph.allVertices().keySet()) {
			if (pi.contains(ngv)) {
				overlaps.add(ngv);
			}
		}
		return overlaps;
	}

	private boolean checkNegatives() {

		log.debug("before check negative:" + matches.size());

		List<Int2IntMap> ngMatches = new LinkedList<Int2IntMap>();

		for (Graph<VertexInt, TypedEdge> ngGraph : p.getNegativeGraphsForIncremental()) {
			IntSet overlaps = getOverlapVertices(ngGraph, p.getPI());
			findNegativeMatches(ngGraph, ngMatches, overlaps);
		}

		for (Int2IntMap ngMatch : ngMatches) {
			// log.debug("current ngMatche = " + ngMatch.toString());
			if (ngMatch.size() <= 1) {
				log.error("!!!!!!!!!!!!!!!!error:ngMatch size should at least = 2.");
			}
			Iterator<Int2IntMap> it = matches.iterator();
			while (it.hasNext()) {
				Int2IntMap pMatch = it.next();
				boolean rm = true;
				for (int ngKey : ngMatch.keySet()) {
					if (pMatch.containsKey(ngKey) && pMatch.get(ngKey) != ngMatch.get(ngKey)) {
						// pMatch and ngMatch have the same key but different
						// mapping.
						rm = false;
						break;
					}
				}
				if (rm) {
					it.remove();
				}
			}
		}

		log.debug("after check negatives:" + matches.size());

		return !matches.isEmpty();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean match(State s, List<Int2IntMap> matches) {

		if (s.isGoal()) {

			// log.info("find a match:" + s.getMatch().size());
			// log.info(s.getMatch().toString());

			Int2IntMap match = new Int2IntOpenHashMap(s.getMatch());
			matches.add(match);

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
				found = match(copy, matches);
				// If we found a match, then don't bother backtracking as it
				// would be wasted effort.
				// if (!found)
				copy.backTrack();
			}
		}
		return found;
	}
}
