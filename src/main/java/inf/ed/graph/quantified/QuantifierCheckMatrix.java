package inf.ed.graph.quantified;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class QuantifierCheckMatrix {

	static Logger log = LogManager.getLogger(QuantifierCheckMatrix.class);
	Int2ObjectMap<IntSet> checkedValid;
	Int2ObjectMap<IntSet> checkedInvalid;

	Object2IntMap<String> edgeCount;
	Object2IntMap<String> edgePatternCount;

	final public static int CHECKED_AND_VALID = 1;
	final public static int CHECKED_AND_INVALID = 0;
	final public static int UNCHECKED = -1;

	public QuantifierCheckMatrix(QuantifiedPattern p) {

		edgeCount = new Object2IntOpenHashMap<String>();
		edgePatternCount = new Object2IntOpenHashMap<String>();

		checkedInvalid = new Int2ObjectOpenHashMap<IntSet>();
		checkedValid = new Int2ObjectOpenHashMap<IntSet>();

		for (int u : p.getGraph().allVertices().keySet()) {
			checkedInvalid.put(u, new IntOpenHashSet());
			checkedValid.put(u, new IntOpenHashSet());
		}
	}

	public void checkedAndInvalid(int u, int v) {
		this.checkedInvalid.get(u).add(v);
	}

	public void checkedAndValid(int u, int v) {
		this.checkedValid.get(u).add(v);
	}

	public int check(int u, int v) {
		if (checkedValid.get(u).contains(v)) {
			return CHECKED_AND_VALID;
		} else if (checkedInvalid.get(u).contains(v)) {
			return CHECKED_AND_INVALID;
		} else
			return UNCHECKED;
	}

	private String getEdgeCountKey(int fromID, int edgeAttr) {
		return fromID + "-" + edgeAttr;
	}

	public int getEdgeCount(int fromID, int edgeAttr) {
		String key = getEdgeCountKey(fromID, edgeAttr);
		if (edgeCount.containsKey(key)) {
			return edgeCount.getInt(key);
		}
		return UNCHECKED;
	}

	public void setEdgeCount(int fromID, int edgeAttr, int value) {
		String key = getEdgeCountKey(fromID, edgeAttr);
		edgeCount.put(key, value);
	}

	private String getEdgePatternCountKey(int fromID, int edgeAttr, int tnAttr) {
		return fromID + "-" + edgeAttr + "-" + tnAttr;
	}

	public int getEdgePatternCount(int fromID, int edgeAttr, int tnAttr) {
		String key = getEdgePatternCountKey(fromID, edgeAttr, tnAttr);
		if (edgePatternCount.containsKey(key)) {
			return edgePatternCount.getInt(key);
		}
		return UNCHECKED;
	}

	public void setEdgePatternCount(int fromID, int edgeAttr, int tnAttr, int value) {
		String key = getEdgePatternCountKey(fromID, edgeAttr, tnAttr);
		edgePatternCount.put(key, value);
	}

	public String toString() {
		String ret = "Matrix:\n---------valid---------";
		for (int key : checkedValid.keySet()) {
			ret += "[" + key + "]: ";
			for (int v : checkedValid.get(key)) {
				ret += v + ", ";
			}
			ret += "\n";
		}
		ret += "---------invalid---------";
		for (int key : checkedInvalid.keySet()) {
			ret += "[" + key + "]: ";
			for (int v : checkedInvalid.get(key)) {
				ret += v + ", ";
			}
			ret += "\n";
		}
		return ret;
	}
}
