package inf.ed.graph.quantified;

import static inf.ed.graph.quantified.State.NULL_NODE;
import inf.ed.graph.structure.Edge;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
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
 * 
 * 
 * @author yecol
 *
 */
public class Opt1Matcher<VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(Opt1Matcher.class);

	/* check quantifiers in VF2 optimisation flag */
	static private final boolean flagCheckQuantifierInVF2Opt = true;

	QuantifiedPattern p;
	Graph<VG, EG> g;

	int v1;
	int v2;

	List<Int2IntMap> matches;// matches of pi graph of pattern.
	@SuppressWarnings("rawtypes")
	List<State> positiveStates;// matches of pi graph of pattern.

	Int2IntMap mapV2TypedEdgeCount;
	QuantifierCheckMatrix m;

	/* node v in G -> count of edge with u in Q, which u~>v */

	public Opt1Matcher() {
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public boolean isIsomorphic(QuantifiedPattern p, int v1, Graph<VG, EG> g, int v2) {

		this.p = p;
		this.g = g;
		this.v1 = v1;
		this.v2 = v2;

		this.mapV2TypedEdgeCount = new Int2IntOpenHashMap();
		this.matches = new ArrayList<Int2IntMap>();
		this.positiveStates = new ArrayList<State>();
		this.m = new QuantifierCheckMatrix(p);

		boolean valid = this.findMathesOfPI() && this.validateMatchesOfPi()
				&& this.checkNegatives() && this.validateMatchesOfPi();

		log.info("final matches results: size = " + matches.size());
		log.debug(matches.toString());
		return valid;
	}

	@SuppressWarnings("rawtypes")
	private boolean findMathesOfPI() {

		Queue<State> queue = new LinkedList<State>();
		State initState = new State<VertexInt, VG, TypedEdge, EG>(p.getPI(), v1, g, v2,
				p.getQuantifiers(), m, flagCheckQuantifierInVF2Opt, true);
		queue.add(initState);

		checkAndCountTypedEdgeForPercentage(v1, v2);
		return this.findMatchesWithState(queue, matches);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean findNegativeMatches(Graph<VertexInt, TypedEdge> ngGraph,
			List<Int2IntMap> ngMatches) {

		System.out.println("nggraph:");
		ngGraph.display(1000);

		Queue<State> queue = new LinkedList<State>();

		int i = 0;
		for (State s : positiveStates) {
			// if (i < 2) {
			State sn = new State(s, ngGraph);
			if (sn.checkNegativeGraphIncremental(ngGraph)) {
				// log.info("add one.");
				queue.add(sn);
			} else {
				// log.info("not add one.");
			}
			// i++;
			// }
		}
		log.info("the result below is find negative matches.");
		return this.findMatchesWithState(queue, ngMatches);
		// return true;
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
			for (int fv : aggregatedMatchesByFv.keySet()) {
				if (!quantifier.isValid(aggregatedMatchesByFv.get(fv).size(),
						mapV2TypedEdgeCount.get(fv))) {
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

	private boolean checkNegatives() {

		log.debug("before check negative:" + matches.size());

		List<Int2IntMap> ngMatches = new LinkedList<Int2IntMap>();

		for (Graph<VertexInt, TypedEdge> ngGraph : p.getNegativeGraphsForIncremental()) {
			findNegativeMatches(ngGraph, ngMatches);
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean findMatchesWithState(Queue<State> q, List<Int2IntMap> matches) {

		long start = System.currentTimeMillis();

		while (!q.isEmpty()) {

			State s = q.poll();

			if (s.isGoal()) {
				positiveStates.add(s);
				matches.add(s.getMatch());
				continue;
			}

			if (s.isDead()) {
				continue;
			}

			s.nextN1();

			int n1 = NULL_NODE, n2 = NULL_NODE;
			Pair<Integer> next = null;
			while ((next = s.nextPair(n1, n2)) != null) {
				n1 = next.x;
				n2 = next.y;
				if (s.isFeasiblePair(n1, n2)) {
					State copy = s.copy();
					copy.addPair(n1, n2);
					q.add(copy);
					checkAndCountTypedEdgeForPercentage(n1, n2);
				}
			}
		}

		// log.info("================findMatchesWithState finished==================");
		// log.info("this.mapV2TypedEdgeCount" +
		// this.mapV2TypedEdgeCount.toString());
		// log.info(m.toString());

		log.info("enumerate all matches using:" + (System.currentTimeMillis() - start)
				+ "ms, find = " + matches.size());
		return !matches.isEmpty();
	}

	private void checkAndCountTypedEdgeForPercentage(int n1, int n2) {
		if (p.getMapU2PercentageEdgeType().containsKey(n1)
				&& !this.mapV2TypedEdgeCount.containsKey(n2)) {
			// n2 has typed edge with percentage and not checked
			// before.
			int typedEdgeCount = 0;
			int attr = p.getMapU2PercentageEdgeType().get(n1);
			// TODO:make get children more efficient
			for (int target : g.getChildren(n2)) {
				// TODO: make this generic
				if (((OrthogonalEdge) g.getEdge(n2, target)).getAttr() == attr)
					typedEdgeCount++;
			}
			this.mapV2TypedEdgeCount.put(n2, typedEdgeCount);
		}
	}
}
