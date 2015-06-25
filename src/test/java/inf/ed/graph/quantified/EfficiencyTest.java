package inf.ed.graph.quantified;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class EfficiencyTest {

	static Logger log = LogManager.getLogger(FunctionalTest.class);

	static Graph<VertexOInt, OrthogonalEdge> g;

	@BeforeClass
	public static void prepareData() {

		String filePath = "dataset/exp/graph-0";
		g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(filePath, false);
	}

	@Test
	public void bigGraphTest() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-pokec");
		pp.display();

		ArrayList<Integer> candidates = new ArrayList<Integer>();

		// for (VertexOInt n : g.allVertices().values()) {
		// if (n.getAttr() == 1) {
		// candidates.add(n.getID());
		// }
		// }
		candidates.add(76);

		System.out.println("candidates size = " + candidates.size());

		ArrayList<Integer> yes = new ArrayList<Integer>();

		for (int v : candidates) {
			log.info("=====================current process " + v + "====================");
			BaseMatcher<VertexOInt, OrthogonalEdge> inspector = new BaseMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}

		VertexOInt fv = g.getVertex(76);
		VertexOInt tv = g.getVertex(2120027);
		g.addEdge(fv, tv);

		for (int v : candidates) {
			log.info("=====================current process " + v + "====================");
			BaseMatcher<VertexOInt, OrthogonalEdge> inspector = new BaseMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}
	}
}
