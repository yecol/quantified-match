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

	QuantifierCheckMatrix m;

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
		m = new QuantifierCheckMatrix(p);

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
	@SuppressWarnings("rawtypes")
	private boolean isQuantifiedIsomorphic(int cand) {

		log.debug("begin to find");

		List<State> matches = new ArrayList<State>();

		boolean valid = this.findMathesOfPI(cand, matches) && this.validateMatchesOfPi(matches)
				&& this.checkNegatives(matches) && this.validateMatchesOfPi(matches);

		log.info(printMatches(matches));
		return valid;
	}

	@SuppressWarnings("rawtypes")
	private String printMatches(List<State> matches) {
		String ret = "match results. size = " + matches.size() + "\n";
		for (State s : matches) {
			ret += s.getMatch().toString() + ", ";
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private boolean findMathesOfPI(int can, List<State> matches) {

		Queue<State> queue = new LinkedList<State>();
		State initState = new State<VertexInt, VG, TypedEdge, EG>(p.getPI(), du, g, can,
				p.getQuantifiers(), m, flagCheckQuantifierInVF2Opt, true);
		queue.add(initState);
		return this.findMatchesWithState(queue, matches);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean findNegativeMatches(Graph<VertexInt, TypedEdge> ngGraph, List<State> matches,
			List<State> ngMatches) {

		System.out.println("nggraph:");
		ngGraph.display(1000);

		Queue<State> queue = new LinkedList<State>();

		for (State s : matches) {
			// if (i < 2) {
			State sn = new State(s, ngGraph);
			if (sn.checkNegativeGraphIncremental(ngGraph)) {
				queue.add(sn);
			}
		}
		log.info("default queue size = " + queue.size());
		log.info("the result below is find negative matches.");
		return this.findMatchesWithState(queue, ngMatches);
	}

	/**
	 * check the matches of pi pattern with quantifiers.
	 * 
	 * @return true if need to continue.
	 */
	@SuppressWarnings("rawtypes")
	private boolean validateMatchesOfPi(List<State> matches) {

		log.debug("before validate matches of Pi:" + matches.size());

		Set<Integer> checked = new HashSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.addAll(p.getBottomVertices(p.getPI()));

		while (!queue.isEmpty()) {
			int u = queue.poll();
			for (int fu : p.getPI().getParents(u)) {
				// current deal with edge fu->u in Q
				Quantifier quantifier = p.getQuantifierWithEdge(fu, u);
				boolean changedfv = this.filterMatches(matches, fu, u, quantifier);
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
	@SuppressWarnings("rawtypes")
	private boolean filterMatches(List<State> matches, int ufromID, int utoID, Quantifier quantifier) {

		if (quantifier.isExistential()) {
			return false;
		}

		Int2ObjectMap<IntSet> aggregatedMatchesByFv = new Int2ObjectOpenHashMap<IntSet>();
		for (State matchState : matches) {
			Int2IntMap match = matchState.getMatch();
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

		Iterator<State> it = matches.iterator();
		while (it.hasNext()) {
			if (removedTargetMapping.contains(it.next().getMatch().get(ufromID))) {
				it.remove();
			}
		}

		return !removedTargetMapping.isEmpty();
	}

	@SuppressWarnings("rawtypes")
	private boolean checkNegatives(List<State> matches) {

		log.debug("before check negative:" + matches.size());

		List<State> ngMatches = new LinkedList<State>();

		for (Graph<VertexInt, TypedEdge> ngGraph : p.getNegativeGraphsForIncremental()) {
			findNegativeMatches(ngGraph, matches, ngMatches);
		}

		for (State ngMatchState : ngMatches) {
			// log.debug("current ngMatche = " + ngMatch.toString());
			Int2IntMap ngMatch = ngMatchState.getMatch();
			if (ngMatch.size() <= 1) {
				log.error("!!!!!!!!!!!!!!!!error:ngMatch size should at least = 2.");
			}
			Iterator<State> it = matches.iterator();
			while (it.hasNext()) {
				Int2IntMap pMatch = it.next().getMatch();
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
	private boolean findMatchesWithState(Queue<State> q, List<State> matches) {

		long start = System.currentTimeMillis();

		while (!q.isEmpty()) {

			State s = q.poll();

			if (s.isGoal()) {
				matches.add(s);
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
					// checkAndCountTypedEdgeForPercentage(n1, n2);
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
}
