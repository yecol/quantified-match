package inf.ed.graph.quantified;

import static inf.ed.graph.quantified.QuantifierCheckMatrix.CHECKED_AND_INVALID;
import static inf.ed.graph.quantified.QuantifierCheckMatrix.CHECKED_AND_VALID;
import static inf.ed.graph.quantified.QuantifierCheckMatrix.UNCHECKED;

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
 * A State implementation for testing isomorphism using the VF2 algorithm's
 * logic.
 * 
 * TODO: Do not require Q has continuous index. but check from smaller index to
 * large one.
 * 
 * TODO: Support parameter to guide it "find a match then stop" or enumerate all
 * matches.
 * 
 */
@SuppressWarnings("rawtypes")
public class State<VQ extends Vertex, EQ extends Edge, VG extends Vertex, EG extends Edge> {

	static Logger log = LogManager.getLogger(State.class);
	public static final int NULL_NODE = -1;

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
	int t1bothLen, t2bothLen, t1inLen, t1outLen, t2inLen, t2outLen;

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
	 * The node in g1 that was most recently added.
	 */
	int addedNode1;
	/**
	 * The seen node in g2 that was most recently added.
	 */
	IntList addedSeenNode2;

	/**
	 * The number of nodes in {@code g1}
	 */
	private int n1;

	/**
	 * The number of nodes in {@code g2}
	 */
	private final int n2;

	/**
	 * largest index number of nodes in {@code g1}
	 */
	private int maxn1;

	private boolean needFurtherCheckNegationEdge = true;

	/**
	 * Whether the algorithm needs to check for edge number constraints on the
	 * graph's edges. This is stored as a global to avoid recomputing it each
	 * time {@code areEdgesCompatible} is called (a hot spot), when it is
	 * already known at state construction time.
	 */
	private boolean checkQuantifiers = false;
	private Map<String, Quantifier> quantifiers;
	private QuantifierCheckMatrix m;

	/**
	 * Creates a new {@code VF2State} with an empty mapping between the two
	 * graphs.
	 */
	public State(Graph<VQ, EQ> g1, int v1, Graph<VG, EG> g2, int v2,
			Map<String, Quantifier> quantifiers, QuantifierCheckMatrix m) {
		this.p = g1;
		this.g = g2;

		if (quantifiers != null && m != null) {
			this.checkQuantifiers = true;
		}

		n1 = g1.vertexSize();
		n2 = g2.vertexSize();

		coreLen = 0;
		origCoreLen = 0;
		t1bothLen = 0;
		t1inLen = 0;
		t1outLen = 0;
		t2bothLen = 0;
		t2inLen = 0;
		t2outLen = 0;

		maxn1 = findMaxNInQ();

		addedNode1 = NULL_NODE;

		core1 = new Int2IntOpenHashMap();
		core2 = new Int2IntOpenHashMap();
		in1 = new Int2IntOpenHashMap();
		in2 = new Int2IntOpenHashMap();
		out1 = new Int2IntOpenHashMap();
		out2 = new Int2IntOpenHashMap();

		sn2 = new IntArrayList();
		addedSeenNode2 = new IntArrayList();
		sn2k = new IntOpenHashSet();

		this.quantifiers = quantifiers;
		this.m = m;

		sn2.add(v2);
		sn2k.add(v2);
	}

	protected State(State copy) {
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

		sn2 = copy.sn2;
		sn2k = copy.sn2k;

		maxn1 = copy.maxn1;

		addedNode1 = NULL_NODE;
		addedSeenNode2 = new IntArrayList();

		// NOTE: we don't need to copy these arrays because their state restored
		// via the backTrack() function after processing on the cloned state
		// finishes
		core1 = copy.core1;
		core2 = copy.core2;
		in1 = copy.in1;
		in2 = copy.in2;
		out1 = copy.out1;
		out2 = copy.out2;

		quantifiers = copy.quantifiers;
		m = copy.m;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pair<Integer> nextPair(int prevN1, int prevN2) {

		// log.debug(sn2.toString());
		//
		// log.debug("next pair,prevN1=" + prevN1 + ",prevN2=" + prevN2 +
		// " ,current sn2.size = "
		// + sn2.size());

		if (prevN1 == NULL_NODE)
			prevN1 = 0;

		if (prevN2 == NULL_NODE)
			prevN2 = 0;

		else {
			prevN2 = sn2.indexOf(prevN2);
			prevN2++;
			while (prevN2 < sn2.size()
					&& (core2.containsKey(sn2.get(prevN2)) || !out2.containsKey(sn2.get(prevN2)))
					&& (core2.containsKey(sn2.get(prevN2)) || !in2.containsKey(sn2.get(prevN2)))) {
				prevN2++;
			}
		}

		if (t1bothLen > coreLen && t2bothLen > coreLen) {
			while (prevN1 < maxn1
					&& (core1.containsKey(prevN1) || !out1.containsKey(prevN1) || !in1
							.containsKey(prevN1))) {
				prevN1++;
				prevN2 = 0;
			}
			while (prevN2 < sn2.size()
					&& (core2.containsKey(sn2.get(prevN2)) || !out2.containsKey(sn2.get(prevN2)) || !in2
							.containsKey(sn2.get(prevN2)))) {
				prevN2++;
			}
		} else if (t1outLen > coreLen && t2outLen > coreLen) {
			while (prevN1 < maxn1 && (core1.containsKey(prevN1) || !out1.containsKey(prevN1))) {
				prevN1++;
				prevN2 = 0;
			}
			while (prevN2 < sn2.size()
					&& (core2.containsKey(sn2.get(prevN2)) || !out2.containsKey(sn2.get(prevN2)))) {
				prevN2++;
			}

		} else if (t1inLen > coreLen && t2inLen > coreLen) {
			while (prevN1 < maxn1 && (core1.containsKey(prevN1) || !in1.containsKey(prevN1))) {
				prevN1++;
				prevN2 = 0;
			}
			while (prevN2 < sn2.size()
					&& (core2.containsKey(sn2.get(prevN2)) || !in2.containsKey(sn2.get(prevN2)))) {
				prevN2++;
			}
		} else {
			// log.error("should not come into this for s1.");
			while (prevN1 < maxn1 && core1.containsKey(prevN1)) {
				prevN1++;
				prevN2 = 0;
			}
			while (prevN2 < sn2.size() && core2.containsKey(sn2.get(prevN2))) {
				prevN2++;
			}
		}

		if (prevN1 < maxn1 && prevN2 < sn2.size()) {
			//
			// log.debug("prevN1=" + prevN1 + " prevN2=" + prevN2 + ", realID,"
			// + prevN1 + ":"
			// + sn2.get(prevN2));
			return new Pair<Integer>(prevN1, sn2.get(prevN2));
		} else
			return null;

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
		return p.getVertex(v1).match(g.getVertex(v2));
	}

	private boolean areCompatableQuantifiers(int v1, int v2) {
		// test for quantifiers.
		if (checkQuantifiers) {
			int checked = m.check(v1, v2);
			if (checked == CHECKED_AND_VALID) {
				return true;
			} else if (checked == CHECKED_AND_INVALID) {
				return false;
			} else {
				for (int other1 : p.getChildren(v1)) {
					TypedEdge te = (TypedEdge) p.getEdge(v1, other1);
					Quantifier f = quantifiers.get(KeyGen.getTypedEdgeKey(te));
					if (f.isExistential()) {
						m.checkedAndValid(v1, v2);
						continue;
					} else if (f.isCount()
							&& !f.isValid(getEdgePatternCount(v2, te.getAttr(),
									((VertexInt) p.getVertex(other1)).getAttr()))) {
						m.checkedAndInvalid(v1, v2);
						return false;

					} else if (f.isPercentage()
							&& !f.isValid(
									getEdgePatternCount(v2, te.getAttr(),
											((VertexInt) p.getVertex(other1)).getAttr()),
									getEdgeCount(v2, te.getAttr()))) {
						m.checkedAndInvalid(v1, v2);
						return false;
					}
				}
				m.checkedAndValid(v1, v2);
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isFeasiblePair(int node1, int node2) {

		assert node1 < maxn1;
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
			if (!core2.containsKey(other2)) {
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
			if (!core2.containsKey(other2)) {
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

		// System.out.println("add pair:" + node1 + "," + node2);

		assert node1 < maxn1;
		assert coreLen < n1;

		coreLen++;
		addedNode1 = node1;
		addedSeenNode2.clear();

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
				addedSeenNode2.add(other);
				sn2k.add(other);
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
				addedSeenNode2.add(other);
				sn2k.add(other);
			}
		}

		sn2.addAll(addedSeenNode2);

	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isGoal() {
		return coreLen == n1;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDead() {
		if (core1.size() == 1 && checkQuantifiers) {
			int u = core1.keySet().toIntArray()[0];
			return !areCompatableQuantifiers(u, core1.get(u));
		} else {
			return n1 > n2 || t1bothLen > t2bothLen || t1outLen > t2outLen || t1inLen > t2inLen;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public State copy() {
		return new State(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void backTrack() {

		assert addedNode1 != NULL_NODE;

		if (origCoreLen < coreLen) {

			int node2;

			if (in1.get(addedNode1) == coreLen)
				in1.remove(addedNode1);

			for (int other : p.getParents(addedNode1)) {
				if (in1.get(other) == coreLen)
					in1.remove(other);
			}

			if (out1.get(addedNode1) == coreLen)
				out1.remove(addedNode1);

			for (int other : p.getChildren(addedNode1)) {
				if (out1.get(other) == coreLen)
					out1.remove(other);
			}

			node2 = core1.get(addedNode1);

			if (in2.get(node2) == coreLen)
				in2.remove(node2);

			for (int other : g.getParents(node2)) {
				if (in2.get(other) == coreLen)
					in2.remove(other);
			}

			if (out2.get(node2) == coreLen)
				out2.remove(node2);

			for (int other : g.getChildren(node2)) {
				if (out2.get(other) == coreLen)
					out2.remove(other);
			}

			core1.remove(addedNode1);
			core2.remove(node2);

			sn2.removeAll(addedSeenNode2);
			sn2k.removeAll(addedSeenNode2);

			coreLen = origCoreLen;
			addedNode1 = NULL_NODE;
			addedSeenNode2.clear();
		}
	}

	private int findMaxNInQ() {
		int max = 0;
		for (int vID : this.p.allVertices().keySet()) {
			max = Math.max(max, vID);
		}
		return max + 1;
	}

	public Int2IntMap getMatch() {
		return this.core1;
	}

	private int getEdgeCount(int fromID, int edgeAttr) {
		int ret = m.getEdgeCount(fromID, edgeAttr);
		if (ret == UNCHECKED) {
			ret = 0;
			for (int toID : g.getChildren(fromID)) {
				if (((OrthogonalEdge) g.getEdge(fromID, toID)).getAttr() == edgeAttr) {
					ret++;
				}
			}
			m.setEdgeCount(fromID, edgeAttr, ret);
		} else {
			log.info("HIT-edgeCount ret = " + ret);
		}
		// log.debug("edge count=" + ret);
		return ret;
	}

	private int getEdgePatternCount(int fromID, int edgeAttr, int tnAttr) {
		int ret = m.getEdgePatternCount(fromID, edgeAttr, tnAttr);
		if (ret == UNCHECKED) {
			ret = 0;
			for (int toID : g.getChildren(fromID)) {
				if (((OrthogonalEdge) g.getEdge(fromID, toID)).getAttr() == edgeAttr
						&& ((VertexOInt) g.getVertex(toID)).getAttr() == tnAttr) {
					ret++;
				}
			}
			m.setEdgePatternCount(fromID, edgeAttr, tnAttr, ret);
		} else {
			log.info("HIT-edgePatternCount ret = " + ret);
		}
		return ret;
	}

	public State(Graph<VQ, EQ> nq, Graph<VG, EG> g, Map<String, Quantifier> quantifiers,
			QuantifierCheckMatrix m, Int2IntMap posMatch, IntSet overlaps) {
		this.p = nq;
		this.g = g;

		if (quantifiers != null && m != null) {
			this.checkQuantifiers = true;
		}

		n1 = nq.vertexSize();
		n2 = g.vertexSize();

		coreLen = 0;
		origCoreLen = 0;
		t1bothLen = 0;
		t1inLen = 0;
		t1outLen = 0;
		t2bothLen = 0;
		t2inLen = 0;
		t2outLen = 0;

		maxn1 = findMaxNInQ();

		addedNode1 = NULL_NODE;

		core1 = new Int2IntOpenHashMap();
		core2 = new Int2IntOpenHashMap();
		in1 = new Int2IntOpenHashMap();
		in2 = new Int2IntOpenHashMap();
		out1 = new Int2IntOpenHashMap();
		out2 = new Int2IntOpenHashMap();

		sn2 = new IntArrayList();
		addedSeenNode2 = new IntArrayList();
		sn2k = new IntOpenHashSet();

		this.quantifiers = quantifiers;
		this.m = m;

		/** the same as state initialisation. **/

		int beginN = NULL_NODE;
		for (int v : nq.allVertices().keySet()) {
			if (nq.getParents(v).size() == 0) {
				beginN = v;
				break;
			}
		}

		for (int v1 : posMatch.keySet()) {
			if (nq.allVertices().containsKey(v1)) {
				if (!overlaps.contains(v1) || beginN == v1) {
					// log.debug("1. add existing pair: v1=" + v1 + ", v2=" +
					// posMatch.get(v1));
					addPair(v1, posMatch.get(v1));
					// log.debug("sn2=" + sn2.toString());
					// log.debug("sn2k=" + sn2k.toString());
					// log.debug("added=" + addedSeenNode2.toString());
				}
			}
		}

		for (int ov1 : overlaps) {
			// System.out.println("ov1+" + ov1 + "beginn=" + beginN);
			if (ov1 != beginN) {
				int ov2 = posMatch.get(ov1);
				if (isFeasiblePair(ov1, ov2)) {
					// log.debug("2. add existing pair: n1=" + ov1 + ", n2=" +
					// ov2);
					addPair(ov1, ov2);
					// log.debug("sn2=" + sn2.toString());
					// log.debug("sn2k=" + sn2k.toString());
					// log.debug("added=" + addedSeenNode2.toString());
				} else {
					needFurtherCheckNegationEdge = false;
				}
			}
		}

		// System.out.println("over");

		// origCoreLen = coreLen;
	}

	boolean needFurtherCheckNegationEdge() {
		return needFurtherCheckNegationEdge;
	}
}
