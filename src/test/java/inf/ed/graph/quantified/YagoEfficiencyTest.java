package inf.ed.graph.quantified;

import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;
import inf.ed.graph.structure.auxiliary.KeyGen;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class YagoEfficiencyTest {

	static Logger log = LogManager.getLogger(FunctionalTest.class);

	static Graph<VertexOInt, OrthogonalEdge> g;

	@BeforeClass
	public static void prepareData() {

		String filePath = "dataset/exp/yago2.p0";
		g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(filePath, true);
	}

	// @Ignore
	@Test
	public void baselineTest() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-yago2");
		// pp.display();

		ArrayList<Integer> candidates = this.getCandidate(pp);

		System.out.println("candidates size = " + candidates.size());

		ArrayList<Integer> yes = new ArrayList<Integer>();

		int i = 0;

		for (int v : candidates) {
			i++;
			if (i % 1000 == 0) {
				System.out.println("processed i=" + i + "/" + candidates.size());
			}
			if (i > 3000) {
				break;
			}
			log.info("=BASE===== current process" + " " + v + "======BASE=");
			BaseMatcher<VertexOInt, OrthogonalEdge> inspector = new BaseMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
				yes.add(v);
			}
		}

		System.out.println(yes.size());
	}

	@Ignore
	@Test
	public void quanCheckTest() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-pokec2");
		// pp.display();

		ArrayList<Integer> candidates = new ArrayList<Integer>();

		// for (VertexOInt n : g.allVertices().values()) {
		// if (n.getAttr() == 1) {
		// // System.out.println(n.getID());
		// candidates.add(n.getID());
		// }
		// }
		candidates.add(76);
		// candidates.add(246287);
		// candidates.add(246388);

		System.out.println("candidates size = " + candidates.size());

		ArrayList<Integer> yes = new ArrayList<Integer>();

		for (int v : candidates) {
			log.info("=QUANCHECK===== current process " + v + "======QUANCHECK=");
			QuanCheckMatcher<VertexOInt, OrthogonalEdge> inspector = new QuanCheckMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}
	}

	public ArrayList<Integer> getCandidate(QuantifiedPattern p) {
		ArrayList<Integer> cands = new ArrayList<Integer>();
		for (int vid : g.allVertices().keySet()) {
			if (g.getVertex(vid).isInnerNode()
					&& KeyGen.getYagoKey(vid) == p.getGraph().getVertex(0).getAttr()) {

				boolean hasChildLabel1 = false;
				boolean hasChildLabel2 = false;

				for (int cid : g.getChildren(vid)) {
					if (!hasChildLabel1 && p.getGraph().getVertex(1).match(g.getVertex(cid))) {
						hasChildLabel1 = true;
						if (hasChildLabel1 && hasChildLabel2) {
							break;
						}
					}
					if (!hasChildLabel2 && p.getGraph().getVertex(2).match(g.getVertex(cid))) {
						hasChildLabel2 = true;
						if (hasChildLabel1 && hasChildLabel2) {
							break;
						}
					}
					if (hasChildLabel1 && hasChildLabel2) {
						break;
					}
				}

				if (hasChildLabel1 && hasChildLabel2) {
					cands.add(vid);
				}

			}
		}
		return cands;
	}

	@Ignore
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
		candidates.add(246287);
		candidates.add(246388);

		System.out.println("candidates size = " + candidates.size());

		ArrayList<Integer> yes = new ArrayList<Integer>();

		for (int v : candidates) {
			log.info("=OPT1===== current process " + v + "=======OPT1=");
			OptMatcher<VertexOInt, OrthogonalEdge> inspector = new OptMatcher<VertexOInt, OrthogonalEdge>();
			boolean iso = inspector.isIsomorphic(pp, 0, g, v);
			if (iso == true) {
				yes.add(v);
			}
		}
	}

	@Ignore
	@Test
	public void multiThreadOptTest() {

		QuantifiedPattern pp = new QuantifiedPattern();
		pp.loadPatternFromVEFile("dataset/test/q-pokec1");

		ArrayList<Integer> candidates = new ArrayList<Integer>();

		candidates.add(76);
		candidates.add(246287);
		candidates.add(246388);

		System.out.println("candidates size = " + candidates.size());

		MtOptMatcher<VertexOInt, OrthogonalEdge> inspector = new MtOptMatcher<VertexOInt, OrthogonalEdge>(
				pp, 0, g, candidates);
		inspector.findIsomorphic();
	}
}
