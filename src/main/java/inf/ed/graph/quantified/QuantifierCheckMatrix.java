package inf.ed.graph.quantified;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class QuantifierCheckMatrix {

	Int2ObjectMap<IntSet> checkedValid;
	Int2ObjectMap<IntSet> checkedInvalid;

	final public static int CHECKED_AND_VALID = 1;
	final public static int CHECKED_AND_INVALID = -1;
	final public static int UNCHECKED = 0;

	public QuantifierCheckMatrix(QuantifiedPattern p) {
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
