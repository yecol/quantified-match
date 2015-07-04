package inf.ed.jar;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;
import inf.ed.graph.util.Dev;

public class GraphLoadTest {

	public static void main(String[] args) {
		System.out.println("args: graphfilename");
		String graphFileName = args[0];

		System.out.println("log::begin load data:" + graphFileName);
		System.out.println(Dev.currentRuntimeState());
		long start = System.currentTimeMillis();
		Graph<VertexOInt, OrthogonalEdge> g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(graphFileName, false);

		System.out.println("log::finished load data:" + (System.currentTimeMillis() - start));
		System.out.println(Dev.currentRuntimeState());
		System.out.println("node_size = " + g.vertexSize() + ", edgesize =" + g.edgeSize());

		System.out.println("begin linear scan.");
		start = System.currentTimeMillis();
		int c_size = 0;
		for (VertexOInt v : g.allVertices().values()) {

			if (g.getChildren(v).size() > 20) {
				c_size++;
			}
		}

		System.out.println("finished linear scan. with time = "
				+ (System.currentTimeMillis() - start));
		System.out.println("log::c_size:" + c_size);

	}

}
