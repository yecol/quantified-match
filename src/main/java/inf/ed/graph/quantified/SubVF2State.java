package inf.ed.graph.quantified;

import java.util.Map;

import inf.ed.graph.structure.Edge;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.Vertex;
import inf.ed.graph.structure.adaptor.TypedEdge;
import inf.ed.graph.structure.adaptor.VertexInt;
import inf.ed.graph.structure.adaptor.VertexOInt;
import inf.ed.graph.structure.auxiliary.KeyGen;
import inf.ed.graph.structure.auxiliary.Pair;
import inf.ed.graph.structure.auxiliary.Quantifier;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link State} implementation for testing isomorphism using the VF2
 * algorithm's logic. Note that this implementation requires that the graphs
 * have contiguous vertex indices (beginning at 0 to {@code g.order()}-1.
 *
 * <p>
 * This implementation is based on the vf2_state implementation in VFLib.
 * 
 * @param <V>
 */
@SuppressWarnings("rawtypes")
public class SubVF2State<VQ extends Vertex, VG extends Vertex, EQ extends Edge, EG extends Edge>
		implements State {

	static Logger log = LogManager.getLogger(SubVF2State.class);

	final private Map<String, Quantifier> quantifiers;

	/**
	 * The query graph
	 */
	private Graph<VQ, EQ> p;

	/**
	 * The base graph (usually large)
	 */
	private Graph<VG, EG> g;

	/**
	 * The number of nodes currently being matched between g1 and g2
	 */
	int coreLen;

	/**
	 * The number of nodes that were matched prior to this current pair being
	 * added, which is used in backtracking.
	 */
	int origCoreLen;

	// State information
	int t1bothLen, t2bothLen, t1inLen, t1outLen, t2inLen, t2outLen; // Core
																	// nodes are
																	// also
																	// counted
																	// by
																	// these...

	Int2IntMap core1;
	Int2IntMap core2;
	Int2IntMap in1;
	Int2IntMap in2;
	Int2IntMap out1;
	Int2IntMap out2;

	/**
	 * The seen node in g2 and its key.
	 */
	IntList sn2;
	IntSet sn2k;

	/**
	 * The number of nodes in {@code g1}
	 */
	private final int n1;

	/**
	 * largest index number of nodes in {@code g1}
	 */
	private final int maxn;

	/**
	 * current node in {@code g1}
	 */
	private int u;

	/**
	 * The number of nodes in {@code g2}
	 */
	private final int n2;

	/**
	 * Whether the algorithm needs to check for edge number constraints on the
	 * graph's edges. This is stored as a global to avoid recomputing it each
	 * time {@code areEdgesCompatible} is called (a hot spot), when it is
	 * already known at state construction time.
	 */
	private final boolean checkQuantifiers;

	/**
	 * Creates a new {@code VF2State} with an empty mapping between the two
	 * graphs.
	 */
	public SubVF2State(Graph<VQ, EQ> p, int v1, Graph<VG, EG> g, int v2,
			Map<String, Quantifier> quantifiers, boolean checkQuantifiers) {
		this.p = p;
		this.g = g;

		this.checkQuantifiers = false;
		this.quantifiers = quantifiers;

		n1 = p.vertexSize();
		n2 = g.vertexSize();
		maxn = findMaxNInQ();

		coreLen = 0;
		origCoreLen = 0;
		t1bothLen = 0;
		t1inLen = 0;
		t1outLen = 0;
		t2bothLen = 0;
		t2inLen = 0;
		t2outLen = 0;

		core1 = new Int2IntOpenHashMap();
		core2 = new Int2IntOpenHashMap();
		in1 = new Int2IntOpenHashMap();
		in2 = new Int2IntOpenHashMap();
		out1 = new Int2IntOpenHashMap();
		out2 = new Int2IntOpenHashMap();

		sn2 = new IntArrayList();
		sn2k = new IntOpenHashSet();

		u = v1;
		sn2.add(v2);
		sn2k.add(v2);

		this.addPair(v1, v2);
	}

	private int findMaxNInQ() {
		int max = 0;
		for (int vID : this.p.allVertices().keySet()) {
			max = Math.max(max, vID);
		}
		return max + 1;
	}

	@SuppressWarnings("unchecked")
	protected SubVF2State(SubVF2State copy) {
		quantifiers = copy.quantifiers;
		checkQuantifiers = copy.checkQuantifiers;
		p = copy.p;
		g = copy.g;
		coreLen = copy.coreLen;
		origCoreLen = copy.origCoreLen;
		t1bothLen = copy.t1bothLen;
		t2bothLen = copy.t2bothLen;
		t1inLen = copy.t1inLen;
		t2inLen = copy.t2inLen;
		t1outLen = copy.t1outLen;
		t2outLen = copy.t2outLen;
		n1 = copy.n1;
		n2 = copy.n2;

		u = copy.u;
		maxn = copy.maxn;

		sn2 = copy.sn2;
		sn2k = copy.sn2k;

		// NOTE: we don't need to copy these arrays because their state restored
		// via the backTrack() function after processing on the cloned state
		// finishes
		core1 = new Int2IntOpenHashMap(copy.core1);
		core2 = new Int2IntOpenHashMap(copy.core2);
		in1 = new Int2IntOpenHashMap(copy.in1);
		in2 = new Int2IntOpenHashMap(copy.in2);
		out1 = new Int2IntOpenHashMap(copy.out1);
		out2 = new Int2IntOpenHashMap(copy.out2);
	}

	protected boolean areCompatibleEdges(int v1, int v2, int v3, int v4) {

		EQ eq = this.p.getEdge(v1, v2);
		EG eg = this.g.getEdge(v3, v4);

		if (eq == null || eg == null) {
			return false;
		} else {
			return eq.match(eg);
		}
	}

	protected boolean areCompatableVertices(int v1, int v2) {
		return this.p.getVertex(v1).match(g.getVertex(v2));
	}

	private boolean areCompatableQuantifiers(int v1, int v2) {
		// test for quantifiers.
		if (checkQuantifiers) {
			for (int other1 : p.getChildren(v1)) {
				TypedEdge te = (TypedEdge) p.getEdge(v1, other1);
				Quantifier f = quantifiers.get(KeyGen.getTypedEdgeKey(te));
				if (f.isExistential()) {
					continue;
				} else if (f.isCount()
						&& !f.isValid(getEdgePatternCount(v2, te.getAttr(),
								((VertexInt) p.getVertex(other1)).getAttr()))) {
					return false;
				} else if (f.isPercentage()
						&& !f.isValid(
								getEdgePatternCount(v2, te.getAttr(),
										((VertexInt) p.getVertex(other1)).getAttr()),
								getEdgeCount(v2, te.getAttr()))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isFeasiblePair(int node1, int node2) {

		assert node1 < n1;
		assert !core1.containsKey(node1);

		if (!areCompatableVertices(node1, node2)) {
			return false;
		}

		if (!areCompatableQuantifiers(node1, node2)) {
			return false;
		}

		int termout1 = 0, termout2 = 0, termin1 = 0, termin2 = 0, new1 = 0, new2 = 0;

		// Check the 'out' edges of node1
		for (int other1 : p.getChildren(node1)) {
			if (core1.containsKey(other1)) {
				int other2 = core1.get(other1);
				// If there's node edge to the other node, or if there is some
				// edge incompatibility, then the mapping is not feasible
				if (!g.contains(node2, other2) || !areCompatibleEdges(node1, other1, node2, other2)) {
					return false;
				}
			} else {
				if (in1.containsKey(other1))
					termin1++;
				if (out1.containsKey(other1))
					termout1++;
				if ((!in1.containsKey(other1)) && (!out1.containsKey(other1)))
					new1++;
			}
		}

		// Check the 'in' edges of node1
		for (int other1 : p.getParents(node1)) {
			if (core1.containsKey(other1)) {
				int other2 = core1.get(other1);
				// If there's node edge to the other node, or if there is some
				// edge incompatibility, then the mapping is not feasible
				if (!g.contains(other2, node2) || !areCompatibleEdges(other1, node1, other2, node2)) {
					return false;
				}
			} else {
				if (in1.containsKey(other1))
					termin1++;
				if (out1.containsKey(other1))
					termout1++;
				if ((!in1.containsKey(other1)) && (!out1.containsKey(other1)))
					new1++;
			}
		}

		// Check the 'out' edges of node2
		for (int other2 : g.getChildren(node2)) {
			if (core2.containsKey(other2)) {
				int other1 = core2.get(other2);
				if (!p.contains(node1, other1))
					return false;
			} else {
				if (in2.containsKey(other2))
					termin2++;
				if (out2.containsKey(other2))
					termout2++;
				if ((!in2.containsKey(other2)) && (!out2.containsKey(other2)))
					new2++;
			}
		}

		// Check the 'in' edges of node2
		for (int other2 : g.getParents(node2)) {
			if (core2.containsKey(other2)) {
				int other1 = core2.get(other2);
				if (!p.contains(other1, node1))
					return false;
			}

			else {
				if (in2.containsKey(other2))
					termin2++;
				if (out2.containsKey(other2))
					termout2++;
				if ((!in2.containsKey(other2)) && (!out2.containsKey(other2)))
					new2++;
			}
		}

		return termin1 <= termin2 && termout1 <= termout2 && new1 <= new2;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPair(int node1, int node2) {

		assert node1 < n1;
		assert coreLen < n1;

		coreLen++;

		if (!in1.containsKey(node1)) {
			in1.put(node1, coreLen);
			t1inLen++;
			if (out1.containsKey(node1))
				t1bothLen++;
		}
		if (!out1.containsKey(node1)) {
			out1.put(node1, coreLen);
			t1outLen++;
			if (in1.containsKey(node1))
				t1bothLen++;
		}

		if (!in2.containsKey(node2)) {
			in2.put(node2, coreLen);
			t2inLen++;
			if (out2.containsKey(node2))
				t2bothLen++;
		}
		if (!out2.containsKey(node2)) {
			out2.put(node2, coreLen);
			t2outLen++;
			if (in2.containsKey(node2))
				t2bothLen++;
		}

		core1.put(node1, node2);
		core2.put(node2, node1);

		for (int other : p.getParents(node1)) {
			if (!in1.containsKey(other)) {
				in1.put(other, coreLen);
				t1inLen++;
				if (out1.containsKey(other))
					t1bothLen++;
			}
		}

		for (int other : p.getChildren(node1)) {
			if (!out1.containsKey(other)) {
				out1.put(other, coreLen);
				t1outLen++;
				if (in1.containsKey(other))
					t1bothLen++;
			}
		}

		for (int other : g.getParents(node2)) {
			if (!in2.containsKey(other)) {
				in2.put(other, coreLen);
				t2inLen++;
				if (out2.containsKey(other))
					t2bothLen++;
			}
			if (!sn2k.contains(other)) {
				sn2k.add(other);
				sn2.add(other);
			}
		}

		for (int other : g.getChildren(node2)) {
			if (!out2.containsKey(other)) {
				out2.put(other, coreLen);
				t2outLen++;
				if (in2.containsKey(other))
					t2bothLen++;
			}
			if (!sn2k.contains(other)) {
				sn2k.add(other);
				sn2.add(other);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isGoal() {
		// if (coreLen == n1) {
		// log.debug("find a match");
		// log.debug(core1.toString());
		// }
		return coreLen == n1;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDead() {
		return n1 > n2 || t1bothLen > t2bothLen || t1outLen > t2outLen || t1inLen > t2inLen;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubVF2State copy() {
		return new SubVF2State(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void backTrack() {
		throw new IllegalArgumentException("Enumeration of matches not need backtrack");
	}

	@Override
	public void nextN1() {
		if (u == NULL_NODE)
			u = 0;
		if (t1bothLen > coreLen && t2bothLen > coreLen) {
			while (u < maxn
					&& (core1.containsKey(u) || !out1.containsKey(u) || !in1.containsKey(u))) {
				u++;
			}
		} else if (t1outLen > coreLen && t2outLen > coreLen) {
			while (u < maxn && (core1.containsKey(u) || !out1.containsKey(u))) {
				u++;
			}

		} else if (t1inLen > coreLen && t2inLen > coreLen) {
			while (u < maxn && (core1.containsKey(u) || !in1.containsKey(u))) {
				u++;
			}
		} else {
			while (u < maxn && core1.containsKey(u)) {
				u++;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Pair<Integer> nextPair(int x, int prevN2) {

		if (x != NULL_NODE) {
			// if x(n1) is not change, then only iterate n2.
			if (prevN2 == NULL_NODE)
				prevN2 = 0;

			else {
				prevN2 = sn2.indexOf(prevN2);
				prevN2++;
				while (prevN2 < sn2.size()
						&& (core2.containsKey(sn2.get(prevN2)) || !out2
								.containsKey(sn2.get(prevN2)))
						&& (core2.containsKey(sn2.get(prevN2)) || !in2.containsKey(sn2.get(prevN2)))) {
					prevN2++;
				}
			}
		}

		else {
			// by calling nextN1, x will be overwrited to -1.
			prevN2 = 0;
			if (t1bothLen > coreLen && t2bothLen > coreLen) {

				while (prevN2 < sn2.size()
						&& (core2.containsKey(sn2.get(prevN2))
								|| !out2.containsKey(sn2.get(prevN2)) || !in2.containsKey(sn2
								.get(prevN2)))) {
					prevN2++;
				}
			} else if (t1outLen > coreLen && t2outLen > coreLen) {
				while (prevN2 < sn2.size()
						&& (core2.containsKey(sn2.get(prevN2)) || !out2
								.containsKey(sn2.get(prevN2)))) {
					prevN2++;
				}

			} else if (t1inLen > coreLen && t2inLen > coreLen) {
				while (prevN2 < sn2.size()
						&& (core2.containsKey(sn2.get(prevN2)) || !in2.containsKey(sn2.get(prevN2)))) {
					prevN2++;
				}
			} else {
				while (prevN2 < sn2.size() && core2.containsKey(sn2.get(prevN2))) {
					prevN2++;
				}
			}
		}

		if (u < maxn && prevN2 < sn2.size()) {
			//
			// log.debug("prevN1=" + u + " prevN2=" + prevN2 + ", realID," + u +
			// ":" + sn2.get(prevN2));
			return new Pair<Integer>(u, sn2.get(prevN2));
		} else
			return null;

	}

	@Override
	public Int2IntMap getMatch() {
		return this.core1;
	}

	@Override
	public int curN1() {
		return u;
	}

	private int getEdgeCount(int fromID, int edgeAttr) {
		int ret = 0;
		for (int toID : g.getChildren(fromID)) {
			if (((OrthogonalEdge) g.getEdge(fromID, toID)).getAttr() == edgeAttr) {
				ret++;
			}
		}
		// log.debug("edge count=" + ret);
		return ret;
	}

	private int getEdgePatternCount(int fromID, int edgeAttr, int tnAttr) {
		int ret = 0;
		for (int toID : g.getChildren(fromID)) {
			if (((OrthogonalEdge) g.getEdge(fromID, toID)).getAttr() == edgeAttr
					&& ((VertexOInt) g.getVertex(toID)).getAttr() == tnAttr) {
				ret++;
			}
		}
		// log.debug("edge pattern count=" + ret);
		return ret;
	}
}
