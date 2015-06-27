package inf.ed.graph.quantified;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class EfficiencyTest {

	static Logger log = LogManager.getLogger(FunctionalTest.class);

	static Graph<VertexOInt, OrthogonalEdge> g;

	@BeforeClass
	public static void prepareData() {

		String filePath = "dataset/exp/graph-0";
		g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(filePath, false);
		VertexOInt fv = g.getVertex(15365);
		VertexOInt tv = g.getVertex(2120004);
		g.addEdge(fv, tv);
		fv = g.getVertex(5386);
		g.addEdge(fv, tv);
	}

	@Ignore
	@Test
	public void baselineTest() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-pokec1");
		// pp.display();

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
			log.info("=BASE===== current process " + v + "======BASE=");
			BaseMatcher<VertexOInt, OrthogonalEdge> inspector = new BaseMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}
	}

	@Test
	public void opt1Test() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-pokec1");
		// pp.display();

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
			log.info("=OPT1===== current process " + v + "=======OPT1=");
			Opt1Matcher<VertexOInt, OrthogonalEdge> inspector = new Opt1Matcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}
	}
}
