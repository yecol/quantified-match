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
import java.util.Vector;

public class QueryGen {

	private static final int PERSON_NODE_LABEL = 1;
	private static final int FRIEND_EDGE_LABEL = 1;
	private static int pid = 0;

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

	public int getEdgeType(int property) {
		return property / 10000 - 200;
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
			edge.setAttr(FRIEND_EDGE_LABEL);
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
			quantifiers.put(edge, q);
		}

		graph.display(1000);
		System.out.println(quantifiers.toString());
		System.out.println(graph.getDiameter());
		pid++;
		String filename = graph.getDiameter() + "-" + pid;

		writeToFile(outputbase + "/" + filename, graph, quantifiers);

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
			edge.setAttr(FRIEND_EDGE_LABEL);
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
		QueryGen qg = new QueryGen("dataset");
		System.out.println("----------------------------------------------");
		for (int i = 0; i < 100; i++) {
			qg.ranDAGWithProperties(4, 3, 8, 2, 3, 20, 80);
		}
		System.out.println("finished.");
	}
}
