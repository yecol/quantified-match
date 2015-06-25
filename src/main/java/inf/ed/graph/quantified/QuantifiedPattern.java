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
	// private List<Graph<VertexInt, TypedEdge>> negativeGraphs;
	private List<NegationPath> negativePathes;

	private Map<String, Quantifier> quantifers;

	private Map<Integer, Integer> mapU2PercentageEdgeType;
	/* node u in Q -> edge attribute */

	static Logger log = LogManager.getLogger(QuantifiedPattern.class);

	private boolean isValid() {
		// TODO: check whether this pattern is valid.
		// not support for more than one kind of percentage edges from one node;
		// not support branches in the successors of a negation edge;
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

		// build negative graphs
		negativePathes = new ArrayList<NegationPath>();
		for (TypedEdge e : negationEdges) {

			NegationPath negationPath = new NegationPath();

			VertexInt cf = (VertexInt) e.from();
			VertexInt ct = (VertexInt) e.to();
			negationPath.addVertex(cf);
			negationPath.addVertex(ct);
			negationPath.addEdge(e);

			while (!graph.getChildren(ct).isEmpty()) {

				if (graph.getChildren(ct).size() != 1) {
					throw new IllegalArgumentException("invalid pattern. ct=" + ct
							+ ", children size = " + graph.getChildren(ct).size());
				}
				cf = ct;
				ct = graph.getChildren(ct).iterator().next();
				negationPath.addVertex(ct);
				TypedEdge ce = graph.getEdge(cf.getID(), ct.getID());
				negationPath.addEdge(ce);

				if (piGraph.contains(ct.getID())) {
					negationPath.setNotFreeEnding();
					break;
				}
			}
			negativePathes.add(negationPath);
		}
	}

	public Graph<VertexInt, TypedEdge> getPI() {
		return this.piGraph;
	}

	public List<NegationPath> getNegativePathes() {
		return this.negativePathes;
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

		mapU2PercentageEdgeType = new HashMap<Integer, Integer>();

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

		System.out.println("=======================================");
		System.out.println("print pattern:");
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
		System.out.println("---------------------------------------");
		System.out.println("print pigraph:");
		this.piGraph.display(1000);
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("there are " + negativePathes.size() + " negative graphs.");
		for (NegationPath path : negativePathes) {
			System.out.println("----------------------------------------");
			System.out.println("print nagetive graph :");
			System.out.println(path.toString());
		}

	}

	public Quantifier getQuantifierWithEdge(int fromID, int toID) {
		String key = KeyGen.getTypedEdgeKey(this.graph.getEdge(fromID, toID));
		return quantifers.get(key);
	}

	public Map<String, Quantifier> getQuantifiers() {
		return this.quantifers;
	}

	public static void main(String[] args) {
		QuantifiedPattern p1 = new QuantifiedPattern();
		p1.loadPatternFromVEFile("dataset/quantified/q1");
		p1.display();

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/quantified/q2");
		p2.display();

		QuantifiedPattern p5 = new QuantifiedPattern();
		p5.loadPatternFromVEFile("dataset/quantified/q5");
		p5.display();
	}
}