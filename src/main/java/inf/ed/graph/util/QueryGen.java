package inf.ed.graph.util;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.SimpleGraph;
import inf.ed.graph.structure.adaptor.TypedEdge;
import inf.ed.graph.structure.adaptor.VertexInt;
import inf.ed.graph.structure.auxiliary.Quantifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class QueryGen {

	private static final int PERSON_NODE_LABEL = 1;
	private static final int FRIEND_EDGE_LABEL = 1;
	private static int pid = 0;
	private static int order = 0;

	private Vector<Integer> properties;
	private String outputbase;

	public QueryGen(String outputbase) {
		loadProperties("dataset/pokec_freqedge");
		this.outputbase = outputbase;
	}

	public void loadProperties(String filename) {
		properties = new Vector<Integer>();
		Scanner scanner;
		try {
			scanner = new Scanner(new File(filename));
			while (scanner.hasNextInt()) {
				properties.add(scanner.nextInt());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(properties.toString());
		System.out.println("load properties.size =" + properties.size());
	}

	public int getEdgeType(int nodeID) {
		if (nodeID < 2000000) {
			return FRIEND_EDGE_LABEL;
		}
		return nodeID / 10000 - 200;
	}

	public void ranDAGWithProperties(int vSize, int vPropertySize, int eSize, int quanSize,
			int countBound, int percentLB, int percentUB) {

		Vector<Integer> vSet = new Vector<Integer>();

		HashSet<Integer> propertyNodes = new HashSet<Integer>();
		// store properties vertices.

		Vector<String> edgeset = new Vector<String>();

		HashMap<Integer, Integer> toprank = new HashMap<Integer, Integer>();

		HashMap<String, Quantifier> quantifiers = new HashMap<String, Quantifier>();

		Graph<VertexInt, TypedEdge> graph = new SimpleGraph<VertexInt, TypedEdge>(VertexInt.class,
				TypedEdge.class);

		// select pv vertices to add a property.
		Vector<Integer> vHasPropertySet = new Vector<Integer>();
		vHasPropertySet.add(0);
		Random randomGenerator = new Random();

		for (int i = 1; i < vPropertySize; i++) {
			vHasPropertySet.add(1 + randomGenerator.nextInt(vSize - 1));
		}

		int vindex = 0;

		for (int i = 0; i < vSize; i++) {
			VertexInt v = new VertexInt(vindex++, PERSON_NODE_LABEL);
			graph.addVertex(v);
			vSet.add(v.getID());
			toprank.put(v.getID(), 0);
			if (vHasPropertySet.contains(i)) {

				// add a random property node
				int property = properties.elementAt(randomGenerator.nextInt(properties.size()));
				VertexInt vp = new VertexInt(vindex++, property);
				graph.addVertex(vp);
				vSet.add(vp.getID());
				toprank.put(vp.getID(), 0);

				// add the edge.
				TypedEdge edge = new TypedEdge(v, vp);
				edge.setAttr(getEdgeType(property));
				graph.addEdge(edge);

				// other process
				String e = v.getID() + "." + vp.getID();
				edgeset.add(e);
				eSize--;
				propertyNodes.add(vp.getID());

			}
		}

		int vsize = vSet.size();
		String e = "";
		for (int j = 0; j < eSize; j++) {
			int max = Integer.MIN_VALUE;
			int fnode = vSet.elementAt((int) (Math.random() * vsize));
			int tnode = vSet.elementAt((int) (Math.random() * vsize));
			e = fnode + "." + tnode;
			int rf = toprank.get(fnode);
			int rt = toprank.get(tnode);
			while (propertyNodes.contains(fnode) || edgeset.contains(e) || fnode == tnode
					|| rt > rf) {
				fnode = vSet.elementAt((int) (Math.random() * vsize));
				tnode = vSet.elementAt((int) (Math.random() * vsize));
				rf = toprank.get(fnode);
				rt = toprank.get(tnode);
				e = fnode + "." + tnode;
			}
			TypedEdge edge = new TypedEdge(graph.getVertex(fnode), graph.getVertex(tnode));
			edge.setAttr(getEdgeType(tnode));
			graph.addEdge(edge);

			for (int target : graph.getChildren(fnode)) {
				int rankt = toprank.get(target);
				if (max < rankt) {
					max = rankt;
				}
			}

			toprank.put(fnode, max + 1);
			edgeset.add(e);

			// propagate update of the topological rank
			Queue<Integer> q = new LinkedList<Integer>();
			HashSet<Integer> visited = new HashSet<Integer>();
			q.add(fnode);
			while (!q.isEmpty()) {
				int start = q.poll();
				for (int source : graph.getParents(start)) {
					if (!visited.contains(source)) {
						int max1 = Integer.MIN_VALUE;
						for (int target : graph.getChildren(source)) {
							int rankt = toprank.get(target);
							if (max1 < rankt) {
								max1 = rankt;
							}
						}
						toprank.put(source, max1 + 1);
						visited.add(source);
						q.add(source);
					}
				}
			}
		}

		// add quantifiers.
		for (int i = 0; i < quanSize; i++) {
			String edge = edgeset.elementAt(randomGenerator.nextInt(edgeset.size()));
			while (quantifiers.keySet().contains(edge)) {
				edge = edgeset.elementAt(randomGenerator.nextInt(edgeset.size()));
			}
			Quantifier q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB,
					percentUB);

			int edgetype = getEdgeType(graph.getVertex(
					Integer.parseInt(edge.substring(edge.indexOf('.') + 1))).getAttr());
			if (edgetype == FRIEND_EDGE_LABEL) {
				while (q.isPercentage()) {
					q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB, percentUB);
				}
			} else {
				while (q.isCount()) {
					q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB, percentUB);
				}
			}

			quantifiers.put(edge, q);
		}

		graph.display(1000);
		System.out.println(quantifiers.toString());
		System.out.println(graph.getDiameter());
		pid++;
		String filename = graph.getDiameter() + "-d" + pid + "-" + order;

		writeToFile(outputbase + "/" + filename, graph, quantifiers);

	}

	public void ranStarWithProperties(int vSize, int vPropertySize, int eSize, int radius,
			int quanSize, int countBound, int percentLB, int percentUB) {

		Vector<Integer> vSet = new Vector<Integer>();

		HashSet<Integer> propertyNodes = new HashSet<Integer>();
		// store properties vertices.

		Vector<String> edgeset = new Vector<String>();

		HashMap<Integer, HashSet<Integer>> mapr = new HashMap<Integer, HashSet<Integer>>();

		HashMap<String, Quantifier> quantifiers = new HashMap<String, Quantifier>();

		Graph<VertexInt, TypedEdge> graph = new SimpleGraph<VertexInt, TypedEdge>(VertexInt.class,
				TypedEdge.class);

		// select pv vertices to add a property.
		Vector<Integer> vHasPropertySet = new Vector<Integer>();
		vHasPropertySet.add(0);
		Random randomGenerator = new Random();

		for (int i = 1; i < vPropertySize; i++) {
			vHasPropertySet.add(1 + randomGenerator.nextInt(vSize - 1));
		}

		int vindex = 0;

		for (int i = 0; i < vSize; i++) {
			VertexInt v = new VertexInt(vindex++, PERSON_NODE_LABEL);
			graph.addVertex(v);
			vSet.add(v.getID());
			// mapr.put(v.getID(), 0);
			if (vHasPropertySet.contains(i)) {

				// add a random property node
				int property = properties.elementAt(randomGenerator.nextInt(properties.size()));
				VertexInt vp = new VertexInt(vindex++, property);
				graph.addVertex(vp);
				vSet.add(vp.getID());

				// add the edge.
				TypedEdge edge = new TypedEdge(v, vp);
				edge.setAttr(getEdgeType(property));
				graph.addEdge(edge);

				// other process
				String e = v.getID() + "." + vp.getID();
				edgeset.add(e);
				// eSize--;
				propertyNodes.add(vp.getID());
			}
		}

		// only nodes 0 has radius = 0;
		HashSet<Integer> zero = new HashSet<Integer>();
		zero.add(0);
		mapr.put(0, zero);

		String e = "";
		for (int j = 1; j < vSet.size(); j++) {
			int hop, fnode, tnode;
			if (!propertyNodes.contains(j)) {
				tnode = vSet.elementAt(j);
				int fhop = getRandomElementOfSet(mapr.keySet());
				while (fhop == radius || (vHasPropertySet.contains(tnode) && fhop == radius - 1)) {
					fhop = getRandomElementOfSet(mapr.keySet());
				}
				hop = fhop + 1;
				fnode = getRandomElementOfSet(mapr.get(fhop));

				if (!mapr.containsKey(hop)) {
					HashSet<Integer> set = new HashSet<Integer>();
					mapr.put(hop, set);
				}
				mapr.get(hop).add(tnode);

				e = fnode + "." + tnode;
				TypedEdge edge = new TypedEdge(graph.getVertex(fnode), graph.getVertex(tnode));
				edge.setAttr(getEdgeType(tnode));
				graph.addEdge(edge);
				edgeset.add(e);
			}
		}

		int appendEdgeSize = eSize - graph.edgeSize();
		System.out.println("append edge size = " + eSize + ", graph.edge" + graph.edgeSize()
				+ ",app=" + appendEdgeSize);
		for (int i = 0; i < appendEdgeSize; i++) {
			int fnode = vSet.elementAt((int) (Math.random() * vSet.size()));
			int tnode = vSet.elementAt((int) (Math.random() * vSet.size()));
			e = fnode + "." + tnode;
			while (propertyNodes.contains(fnode) || edgeset.contains(e) || fnode == tnode) {
				fnode = vSet.elementAt((int) (Math.random() * vSet.size()));
				tnode = vSet.elementAt((int) (Math.random() * vSet.size()));
				e = fnode + "." + tnode;
			}
			TypedEdge edge = new TypedEdge(graph.getVertex(fnode), graph.getVertex(tnode));
			edge.setAttr(getEdgeType(tnode));
			graph.addEdge(edge);
			edgeset.add(e);
		}

		// add quantifiers.
		for (int i = 0; i < quanSize; i++) {
			String edge = edgeset.elementAt(randomGenerator.nextInt(edgeset.size()));
			while (quantifiers.keySet().contains(edge)) {
				edge = edgeset.elementAt(randomGenerator.nextInt(edgeset.size()));
			}
			Quantifier q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB,
					percentUB);
			int edgetype = getEdgeType(graph.getVertex(
					Integer.parseInt(edge.substring(edge.indexOf('.') + 1))).getAttr());
			if (edgetype == FRIEND_EDGE_LABEL) {
				while (q.isPercentage()) {
					q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB, percentUB);
				}
			} else {
				while (q.isCount()) {
					q = Quantifier.generateRanPositiveQuantifier(countBound, percentLB, percentUB);
				}
			}

			quantifiers.put(edge, q);
		}

		graph.display(1000);
		System.out.println(quantifiers.toString());
		pid++;
		String filename = radius + "-s" + pid + "-" + order;

		writeToFile(outputbase + "/" + filename, graph, quantifiers);

	}

	private int getRandomElementOfSet(Set<Integer> set) {
		int item = new Random().nextInt(set.size());
		int i = 0;
		for (int obj : set) {
			if (i == item)
				return obj;
			i = i + 1;
		}
		return -1;
	}

	public void writeToFile(String ofilename, Graph<VertexInt, TypedEdge> graph,
			HashMap<String, Quantifier> quans) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(ofilename + ".v", "UTF-8");
			for (VertexInt v : graph.allVertices().values()) {
				writer.println(v.getID() + "\t" + v.getAttr());
			}
			writer.flush();
			writer.close();

			writer = new PrintWriter(ofilename + ".e", "UTF-8");
			for (TypedEdge e : graph.allEdges()) {
				String os = e.from().getID() + "\t" + e.to().getID() + "\t" + e.getAttr() + "\t";
				String key = e.from().getID() + "." + e.to().getID();
				if (quans.containsKey(key)) {
					os += quans.get(key).getReadableString();
				} else {
					os += ">=\t1";
				}
				writer.println(os);
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	public void ranDAG(int vv, int ee) {

		Vector<Integer> vSet = new Vector<Integer>();
		HashSet<String> edgeset = new HashSet<String>();
		HashMap<Integer, Integer> toprank = new HashMap<Integer, Integer>();

		Graph<VertexInt, TypedEdge> graph = new SimpleGraph<VertexInt, TypedEdge>(VertexInt.class,
				TypedEdge.class);

		for (int i = 0; i < vv; i++) {

			VertexInt v = new VertexInt(i, PERSON_NODE_LABEL);
			graph.addVertex(v);
			vSet.add(v.getID());
			toprank.put(v.getID(), 0);
		}

		int vsize = vSet.size();
		String e = "";
		for (int j = 0; j < ee; j++) {
			int max = Integer.MIN_VALUE;
			int fnode = vSet.elementAt((int) (Math.random() * vsize));
			int tnode = vSet.elementAt((int) (Math.random() * vsize));
			e = fnode + "." + tnode;
			int rf = toprank.get(fnode);
			int rt = toprank.get(tnode);
			while (edgeset.contains(e) || fnode == tnode || rt > rf) {
				fnode = vSet.elementAt((int) (Math.random() * vsize));
				tnode = vSet.elementAt((int) (Math.random() * vsize));
				rf = toprank.get(fnode);
				rt = toprank.get(tnode);
				e = fnode + "." + tnode;
			}
			TypedEdge edge = new TypedEdge(graph.getVertex(fnode), graph.getVertex(tnode));
			edge.setAttr(getEdgeType(tnode));
			graph.addEdge(edge);

			for (int target : graph.getChildren(fnode)) {
				int rankt = toprank.get(target);
				if (max < rankt) {
					max = rankt;
				}
			}

			toprank.put(fnode, max + 1);
			edgeset.add(e);

			// propagate update of the topological rank
			Queue<Integer> q = new LinkedList<Integer>();
			HashSet<Integer> visited = new HashSet<Integer>();
			q.add(fnode);
			while (!q.isEmpty()) {
				int start = q.poll();
				for (int source : graph.getParents(start)) {
					if (!visited.contains(source)) {
						int max1 = Integer.MIN_VALUE;
						for (int target : graph.getChildren(source)) {
							int rankt = toprank.get(target);
							if (max1 < rankt) {
								max1 = rankt;
							}
						}
						toprank.put(source, max1 + 1);
						visited.add(source);
						q.add(source);
					}
				}
			}
		}
		graph.display(1000);
		System.out.println(graph.getDiameter());
	}

	static public void main(String[] args) {
		QueryGen qg = new QueryGen("dataset/ptns/");
		QueryGen.order = 3;
		System.out.println("----------------------------------------------");

		int vSize = 5;
		int eSize = 4;
		int property = 3;
		int hop = 2;
		int quantifiers = 3;
		int countBound = 3;
		int percentLB = 20;
		int percentUP = 90;

		for (int i = 0; i < 10; i++) {
//			qg.ranDAGWithProperties(vSize, property, eSize, quantifiers, countBound, percentLB,
//					percentUP);
			qg.ranStarWithProperties(vSize, property, eSize, hop, quantifiers, countBound,
					percentLB, percentUP);
		}
		System.out.println("finished.");
	}
}
