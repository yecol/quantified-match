package inf.ed.graph.quantified;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.SimpleGraph;
import inf.ed.graph.structure.adaptor.TypedEdge;
import inf.ed.graph.structure.adaptor.VertexInt;
import inf.ed.graph.structure.auxiliary.KeyGen;
import inf.ed.graph.structure.auxiliary.Quantifier;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author yecol
 */
public class QuantifiedPattern {

	private Graph<VertexInt, TypedEdge> graph;
	private Graph<VertexInt, TypedEdge> piGraph;

	private Map<Integer, Graph<VertexInt, TypedEdge>> negativeGraphs;

	private Map<String, Quantifier> quantifers;

	private Map<Integer, Integer> mapU2PercentageEdgeType;
	/* node u in Q -> edge attribute */

	static Logger log = LogManager.getLogger(QuantifiedPattern.class);

	private boolean isValid() {
		// TODO: check whether this pattern is valid.
		// not support for more than one kind of percentage edges from one node;
		// no more than 2 single negation edge in a path.
		return true;
	}

	private void init() {

		// copy graph to piGraph
		this.piGraph = new SimpleGraph<VertexInt, TypedEdge>(VertexInt.class, TypedEdge.class);
		for (VertexInt v : graph.allVertices().values()) {
			this.piGraph.addVertex(v);
			if (graph.getChildren(v).isEmpty()) {
			}
		}
		for (TypedEdge e : graph.allEdges()) {
			this.piGraph.addEdge(e);
		}

		// find negative edges
		List<TypedEdge> negationEdges = new ArrayList<TypedEdge>();
		for (TypedEdge e : this.graph.allEdges()) {
			if (this.quantifers.get(KeyGen.getTypedEdgeKey(e)).isNegation()) {
				negationEdges.add(e);
			}
		}

		// remove negative edges from piGraph
		for (TypedEdge e : negationEdges) {
			Queue<VertexInt> q = new LinkedList<VertexInt>();
			q.add((VertexInt) e.to());
			this.piGraph.removeEdge(e);

			while (!q.isEmpty()) {
				VertexInt v = q.poll();
				if (this.piGraph.getParents(v).isEmpty()) {
					for (VertexInt cv : this.piGraph.getChildren(v)) {
						this.piGraph.removeEdge(v, cv);
						q.add(cv);
					}
					this.piGraph.removeVertex(v);
				}
			}
		}

		// build negative graphs for original and incremental filtering.
		negativeGraphs = new HashMap<Integer, Graph<VertexInt, TypedEdge>>();
		int negativeGraphIndex = 0;

		// incremental should comes first. keep free ending index available.
		for (TypedEdge e : negationEdges) {
			Graph<VertexInt, TypedEdge> ngGraph = new SimpleGraph<VertexInt, TypedEdge>(
					VertexInt.class, TypedEdge.class);
			Queue<VertexInt> q = new LinkedList<VertexInt>();
			q.add((VertexInt) e.to());
			int begin = e.from().getID();

			Set<Integer> checked = new HashSet<Integer>();
			checked.add(begin);

			while (!q.isEmpty()) {
				VertexInt v = q.poll();
				if (!checked.contains(v.getID())) {
					ngGraph.addVertex(v);
					for (VertexInt cv : graph.getChildren(v)) {

						TypedEdge edge = graph.getEdge(v.getID(), cv.getID());

						if (!edge.equals(e) && negationEdges.contains(edge)) {
							// each ngGraph contains only one negative edge
							continue;
						}
						if (!ngGraph.contains(cv.getID())) {
							ngGraph.addVertex(cv);
						}
						if (!ngGraph.contains(v, cv)) {
							ngGraph.addEdge(edge);
						}
						if (piGraph.contains(cv.getID())) {
							continue;
						}
						q.add(cv);
					}
					for (VertexInt pv : graph.getParents(v)) {

						TypedEdge edge = graph.getEdge(pv.getID(), v.getID());
						if (!edge.equals(e) && negationEdges.contains(edge)) {
							// each ngGraph contains only one negative edge.
							continue;
						}
						if (!ngGraph.contains(pv.getID())) {
							ngGraph.addVertex(pv);
						}
						if (!ngGraph.contains(pv, v)) {
							ngGraph.addEdge(edge);
						}
						if (piGraph.contains(pv.getID()) && pv.getID() != begin) {
							continue;
						}
						q.add(pv);
					}
					checked.add(v.getID());
				}
			}
			negativeGraphs.put(negativeGraphIndex, ngGraph);
			negativeGraphIndex++;
		}

		// origin
		for (TypedEdge e : negationEdges) {
			Graph<VertexInt, TypedEdge> ngGraph = new SimpleGraph<VertexInt, TypedEdge>(
					VertexInt.class, TypedEdge.class);
			Queue<VertexInt> q = new LinkedList<VertexInt>();
			q.add((VertexInt) e.to());

			Set<Integer> checked = new HashSet<Integer>();
			checked.add(0);

			while (!q.isEmpty()) {
				VertexInt v = q.poll();
				if (!checked.contains(v.getID())) {
					ngGraph.addVertex(v);
					for (VertexInt cv : graph.getChildren(v)) {

						TypedEdge edge = graph.getEdge(v.getID(), cv.getID());
						if (!edge.equals(e) && negationEdges.contains(edge)) {
							// each ngGraph contains only one negative edge
							continue;
						}
						if (!ngGraph.contains(cv.getID())) {
							ngGraph.addVertex(cv);
						}
						if (!ngGraph.contains(v, cv)) {
							ngGraph.addEdge(edge);
						}
						q.add(cv);
					}
					for (VertexInt pv : graph.getParents(v)) {

						TypedEdge edge = graph.getEdge(pv.getID(), v.getID());
						if (!edge.equals(e) && negationEdges.contains(edge)) {
							// each ngGraph contains only one negative edge.
							continue;
						}
						if (!ngGraph.contains(pv.getID())) {
							ngGraph.addVertex(pv);
						}
						if (!ngGraph.contains(pv, v)) {
							ngGraph.addEdge(edge);
						}
						q.add(pv);
					}
					checked.add(v.getID());
				}
			}
			negativeGraphs.put(negativeGraphIndex, ngGraph);
			negativeGraphIndex++;
		}

	}

	public Graph<VertexInt, TypedEdge> getPI() {
		return this.piGraph;
	}

	public Graph<VertexInt, TypedEdge> getGraph() {
		return this.graph;
	}

	public Map<Integer, Graph<VertexInt, TypedEdge>> getNegativePathes() {
		return this.negativeGraphs;
	}

	public List<Graph<VertexInt, TypedEdge>> getNegativeGraphsForIncremental() {
		List<Graph<VertexInt, TypedEdge>> ret = new ArrayList<Graph<VertexInt, TypedEdge>>();
		int endKey = negativeGraphs.size() / 2;
		for (int k = 0; k < endKey; k++) {
			ret.add(negativeGraphs.get(k));
		}
		return ret;
	}

	public List<Graph<VertexInt, TypedEdge>> getNegativeGraphsForOriginal() {
		List<Graph<VertexInt, TypedEdge>> ret = new ArrayList<Graph<VertexInt, TypedEdge>>();
		int beginKey = negativeGraphs.size() / 2;
		for (int k = beginKey; k < negativeGraphs.size(); k++) {
			ret.add(negativeGraphs.get(k));
		}
		return ret;
	}

	public Map<Integer, Integer> getMapU2PercentageEdgeType() {
		return this.mapU2PercentageEdgeType;
	}

	public Set<Integer> getBottomVertices(Graph<VertexInt, TypedEdge> g) {
		Set<Integer> btms = new HashSet<Integer>();
		for (int vID : g.allVertices().keySet()) {
			if (g.getChildren(vID).isEmpty()) {
				btms.add(vID);
			}
		}
		return btms;
	}

	public QuantifiedPattern() {
		this.graph = new SimpleGraph<VertexInt, TypedEdge>(VertexInt.class, TypedEdge.class);
		this.quantifers = new HashMap<String, Quantifier>();
	}

	public boolean loadPatternFromVEFile(String filePath) {

		this.graph.loadGraphFromVEFile(filePath, true);

		// mapU2PercentageEdgeType = new HashMap<Integer, Integer>();
		FileInputStream fileInputStream = null;
		Scanner sc = null;

		try {

			/** load edges quantifiers */
			fileInputStream = new FileInputStream(filePath + ".e");
			sc = new Scanner(fileInputStream, "UTF-8");
			while (sc.hasNextLine()) {

				String[] elements = sc.nextLine().split("\t");
				int from = Integer.parseInt(elements[0].trim());
				int to = Integer.parseInt(elements[1].trim());
				int attr = Integer.parseInt(elements[2].trim());
				Quantifier quantifier = new Quantifier(elements[3].trim(), elements[4].trim());
				this.quantifers.put(KeyGen.getTypedEdgeKey(from, to, attr), quantifier);
				if (quantifier.isPercentage()) {
					mapU2PercentageEdgeType.put(from, attr);
				}
			}

			if (fileInputStream != null) {
				fileInputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
			log.info("quantified pattern loaded.");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.init();
		return true;
	}

	public void display() {

		System.out.println("======================= a single pattern ==========================");
		System.out.println("print original pattern:");
		System.out.println(this.graph.allVertices().size() + " vertices.");
		for (VertexInt v : graph.allVertices().values()) {
			System.out.println(v.toString());
		}
		System.out.println(this.graph.allEdges().size() + " edges.");
		for (TypedEdge e : graph.allEdges()) {
			String estr = e.toString();
			estr += " ";
			estr += this.quantifers.get(KeyGen.getTypedEdgeKey(e));
			System.out.println(estr);
		}
		System.out.println("--------------- pi graph ----------------------------");
		this.piGraph.display(1000);
		System.out.println("--------------" + this.getNegativeGraphsForOriginal().size()
				+ " original negative graph -------------");
		for (Graph<VertexInt, TypedEdge> ngGraph : this.getNegativeGraphsForOriginal()) {
			ngGraph.display(1000);
			System.out.println("-----------------");
		}
		List<Graph<VertexInt, TypedEdge>> incNegativeGraphs = this
				.getNegativeGraphsForIncremental();
		System.out.println("---------------" + incNegativeGraphs.size()
				+ " increamental negative graph---------------");

		for (int i = 0; i < incNegativeGraphs.size(); i++) {
			incNegativeGraphs.get(i).display(1000);
			System.out.println("-----------------");
		}

	}

	public Quantifier getQuantifierWithEdge(int fromID, int toID) {
		String key = KeyGen.getTypedEdgeKey(this.graph.getEdge(fromID, toID));
		return quantifers.get(key);
	}

	public Map<String, Quantifier> getQuantifiers() {
		return this.quantifers;
	}
}