package inf.ed.graph.quantified;

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

import static inf.ed.graph.quantified.State.NULL_NODE;

public class QuantifiedIsomorphismInspector<VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(QuantifiedIsomorphismInspector.class);

	QuantifiedPattern p;
	Graph<VG, EG> g;

	int v1;
	int v2;

	List<Int2IntMap> matches;// matches of pi graph of pattern.

	Int2IntMap mapV2TypedEdgeCount;
	Int2ObjectMap<IntSet> mapU2RemovedVs;

	/* node v in G -> count of edge with u in Q, which u~>v */

	public QuantifiedIsomorphismInspector() {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isQuantifedIsomorphic(QuantifiedPattern p, int v1, Graph<VG, EG> g, int v2) {

		this.p = p;
		this.g = g;
		this.v1 = v1;
		this.v2 = v2;

		this.mapV2TypedEdgeCount = new Int2IntOpenHashMap();
		this.mapU2RemovedVs = new Int2ObjectOpenHashMap<IntSet>();
		this.matches = new ArrayList<Int2IntMap>();

		boolean iso = this.findMathesOfPI() && this.validateMatchesOfPi() && this.checkNegatives()
				&& this.validateMatchesOfPi();

		log.info("final matches results: size = " + matches.size());
		log.debug(matches.toString());
		return iso;
	}

	private boolean findMathesOfPI() {

		Queue<State> queue = new LinkedList<State>();
		State initState = makeInitialState(p.getPI(), v1, g, v2);
		queue.add(initState);

		checkAndCountTypedEdgeForPercentage(v1, v2);
		return this.findMatchesWithState(queue, matches);
	}

	/**
	 * check the matches of pi pattern with quantifiers.
	 * 
	 * @return true if need to continue.
	 */
	private boolean validateMatchesOfPi() {

		Set<Integer> checked = new HashSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.addAll(p.getBottomVertices(p.getPI()));

		while (!queue.isEmpty()) {
			int u = queue.poll();
			for (int fu : p.getPI().getParents(u)) {
				// current deal with edge fu->u in Q
				Quantifier quantifier = p.getQuantifierWithEdge(fu, u);
				boolean changedfv = this.filterMatches(fu, u, quantifier);
				if ((checked.contains(fu) && changedfv) || !checked.contains(fu)) {
					queue.add(fu);
				}
			}
			checked.add(u);
		}

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
					log.debug(quantifier.toString() + " not valid percentage, remove uFromID="
							+ ufromID + " matching: fv=" + fv);
					removedTargetMapping.add(fv);
				}
			}
		}

		else if (quantifier.isCount()) {
			for (int fv : aggregatedMatchesByFv.keySet()) {
				if (!quantifier.isValid(aggregatedMatchesByFv.get(fv).size())) {
					// remove fv;
					log.debug(quantifier.toString() + " not valid count, remove uFromID=" + ufromID
							+ " matching: fv=" + fv);
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

		for (NegationPath path : p.getNegativePathes()) {

			// log.debug("negative path = " + path.toString());

			VertexInt startu = path.getStartVertex();
			IntSet starterSet = new IntOpenHashSet();
			for (Int2IntMap match : matches) {
				starterSet.add(match.get(startu.getID()));
			}

			// log.debug("candicate startv=" + starterSet.toString());

			VertexInt u;
			IntSet vSet = new IntOpenHashSet();
			IntSet vtSet = new IntOpenHashSet();

			for (int startv : starterSet) {

				u = startu;

				vSet.clear();
				vSet.add(startv);

				vtSet.clear();

				while (path.hasNext(u)) {

					VertexInt ut = path.getNextVertex(u);
					TypedEdge e = path.getNextEdge(u);

					for (int v : vSet) {
						for (int vt : g.getChildren(v)) {
							if (ut.match(g.getVertex(vt)) && e.match(g.getEdge(v, vt))) {
								vtSet.add(vt);
							}
						}
					}

					vSet.clear();
					vSet.addAll(vtSet);
					vtSet.clear();
					u = ut;
				}

				// log.debug("vSet=" + vSet.toString());

				if (path.isFreeEnding() && !vSet.isEmpty()) {
					// is free ending, delete any matches begin with startv
					Iterator<Int2IntMap> it = matches.iterator();
					while (it.hasNext()) {
						if (it.next().get(startu.getID()) == startv) {
							it.remove();
						}
					}
				}

				else if (!path.isFreeEnding()) {
					// remove matches begin with startv and end with vSet;
					Iterator<Int2IntMap> it = matches.iterator();
					while (it.hasNext()) {
						Int2IntMap match = it.next();
						if (match.get(startu.getID()) == startv
								&& vSet.contains(match.get(u.getID())))
							it.remove();
					}
				}
			}
		}

		return !matches.isEmpty();

	}

	/**
	 * Creates an empty {@link State} for mapping the vertices of {@code g1} to
	 * {@code g2}.
	 */
	private State makeInitialState(Graph<VertexInt, TypedEdge> p, int v1, Graph<VG, EG> g, int v2) {
		return new SubVF2State<VertexInt, VG, TypedEdge, EG>(p, v1, g, v2);
	}

	private boolean findMatchesWithState(Queue<State> q, List<Int2IntMap> matches) {

		log.debug("MapU2PercentageEdgeType===============" + p.getMapU2PercentageEdgeType());

		while (!q.isEmpty()) {

			State s = q.poll();

			if (s.isGoal()) {
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

		log.info("================find all matches with state===============");
		log.info("this.mapV2TypedEdgeCount" + this.mapV2TypedEdgeCount.toString());
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
