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
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

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
public class MtOptMatcher<VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(MtOptMatcher.class);

	/* check quantifiers in VF2 optimisation flag */
	static private final boolean flagCheckQuantifierInVF2Opt = true;

	QuantifiedPattern p;
	Graph<VG, EG> g;

	ConcurrentLinkedQueue<Integer> candidates;
	ConcurrentLinkedQueue<Integer> results;

	int du;// designate vertex in Q(g1)

	int threadNumLimit = 0;
	int threadTimeoutLimit = 0;

	/* node v in G -> count of edge with u in Q, which u~>v */

	public MtOptMatcher(QuantifiedPattern query, int du, Graph<VG, EG> graph,
			List<Integer> candidates) {
		this.p = query;
		this.g = graph;

		this.du = du;

		this.results = new ConcurrentLinkedQueue<Integer>();
		this.candidates = new ConcurrentLinkedQueue<Integer>();
		this.candidates.addAll(candidates);
	}

	public Set<Integer> findIsomorphic() {

		Set<Integer> isomorphics = new HashSet<Integer>();
		// m = new QuantifierCheckMatrix(p);

		int threadNum;
		if (this.threadNumLimit == 0) {
			threadNum = Runtime.getRuntime().availableProcessors();
		} else {
			threadNum = Math.min(this.threadNumLimit, Runtime.getRuntime().availableProcessors());
		}

		List<Thread> threadsPool = new ArrayList<Thread>();

		for (int i = 0; i < threadNum; i++) {
			log.debug("Start working thread " + i);
			WorkerThread workerThread = new WorkerThread();
			workerThread.setName("SubVF2Th-" + i);
			workerThread.start();
			threadsPool.add(workerThread);
		}

		try {
			for (Thread thread : threadsPool) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		log.info(results.size());

		isomorphics.addAll(results);
		return isomorphics;
	}

	private class WorkerThread extends Thread {

		@Override
		public void run() {

			try {
				while (!candidates.isEmpty()) {
					int can = candidates.poll();
					// log.debug("current candidate is " + can);
					if (isQuantifiedIsomorphic(can)) {
						results.add(can);
					}
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * {@inheritDoc}
	 */
	private boolean isQuantifiedIsomorphic(int cand) {

		log.debug("begin to find");

		List<Int2IntMap> matches = new LinkedList<Int2IntMap>();
		QuantifierCheckMatrix m = new QuantifierCheckMatrix(p);

		this.findMathesOfPI(cand, matches, m);
		this.validateMatchesOfPi(matches, m);
		this.checkNegatives(matches, m);
		this.validateMatchesOfPi(matches, m);

		// boolean valid = this.findMathesOfPI(cand, matches) &&
		// this.validateMatchesOfPi(matches)
		// && this.checkNegatives(matches) && this.validateMatchesOfPi(matches);
		//
		// log.info(printMatches(matches));
		log.info("final result.size = " + matches.size());
		return !matches.isEmpty();
	}

	@SuppressWarnings("rawtypes")
	private void findMathesOfPI(int cand, List<Int2IntMap> matches, QuantifierCheckMatrix m) {

		long start = System.currentTimeMillis();
		p.getPI().display(1000);

		State initState = new State<VertexInt, TypedEdge, VG, EG>(p.getPI(), du, g, cand,
				p.getQuantifiers(), m);
		match(initState, matches);
		log.info("find matches of PI, size =" + matches.size() + ", using "
				+ (System.currentTimeMillis() - start) / 1000 + "s.");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void checkNegatives(List<Int2IntMap> matches, QuantifierCheckMatrix m) {

		if (matches.isEmpty()) {
			return;
		}

		long start = System.currentTimeMillis();

		log.debug("before check negative:" + matches.size());

		List<Int2IntMap> ngMatches = new LinkedList<Int2IntMap>();

		for (Graph<VertexInt, TypedEdge> ngGraph : p.getNegativeGraphsForIncremental()) {

			IntSet overlaps = getOverlapVertices(ngGraph, p.getPI());
			ListIterator<Int2IntMap> it = matches.listIterator();
			while (it.hasNext()) {

				Int2IntMap match = it.next();
				// if (i < 2) {
				// log.info("check negative for match-" + i + "/" +
				// matches.size() + ","
				// + match.toString());
				State sn = new State(ngGraph, g, p.getQuantifiers(), m, match, overlaps);
				if (sn.needFurtherCheckNegationEdge()) {
					// avoid right-side re-computation.
					this.matchOneAndStop(sn, ngMatches);
				}
			}
		}

		log.info("find negative matches, size =" + matches.size() + ", using "
				+ (System.currentTimeMillis() - start) / 1000 + "s.");

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

	}

	/**
	 * check the matches of pi pattern with quantifiers.
	 * 
	 * @return true if need to continue.
	 */
	private void validateMatchesOfPi(List<Int2IntMap> matches, QuantifierCheckMatrix m) {

		if (matches.isEmpty()) {
			return;
		}

		log.debug("before validate matches of Pi:" + matches.size());

		Set<Integer> checked = new HashSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.addAll(p.getBottomVertices(p.getPI()));

		while (!queue.isEmpty()) {
			int u = queue.poll();
			for (int fu : p.getPI().getParents(u)) {
				// current deal with edge fu->u in Q
				Quantifier quantifier = p.getQuantifierWithEdge(fu, u);
				boolean changedfv = this.filterMatches(fu, u, quantifier, matches, m);
				// if changed the count of fv (mapping of fu).
				if ((checked.contains(fu) && changedfv) || !checked.contains(fu)) {
					queue.add(fu);
				}
			}
			checked.add(u);
		}

		log.debug("after validate matches of Pi:" + matches.size());
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
	private boolean filterMatches(int ufromID, int utoID, Quantifier quantifier,
			List<Int2IntMap> matches, QuantifierCheckMatrix m) {

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean matchOneAndStop(State s, List<Int2IntMap> ngMatches) {
		if (s.isGoal()) {
			Int2IntMap ngMatch = new Int2IntOpenHashMap(s.getMatch());
			ngMatches.add(ngMatch);
			return true;
		}

		if (s.isDead()) {
			return false;
		}

		int n1 = NULL_NODE, n2 = NULL_NODE;
		Pair<Integer> next = null;
		boolean found = false;
		while (!found && (next = s.nextPair(n1, n2)) != null) {
			n1 = next.x;
			n2 = next.y;
			if (s.isFeasiblePair(n1, n2)) {
				State copy = s.copy();
				copy.addPair(n1, n2);
				found = matchOneAndStop(copy, ngMatches);
				// If we found a match, then don't bother backtracking as it
				// would be wasted effort.
				if (!found)
					copy.backTrack();
			}
		}
		return found;
	}
}
