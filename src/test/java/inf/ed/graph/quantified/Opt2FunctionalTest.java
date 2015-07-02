package inf.ed.graph.quantified;

import static org.junit.Assert.assertEquals;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class Opt2FunctionalTest {

	static Logger log = LogManager.getLogger(Opt2FunctionalTest.class);

	static Graph<VertexOInt, OrthogonalEdge> g;

	@BeforeClass
	public static void prepareData() {

		String filePath = "dataset/test/g1";
		g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(filePath, true);
	}

	@Ignore
	@Test
	public void display() {

		System.out.println("this is graph g:");
		// g.display(1000);

		QuantifiedPattern p1 = new QuantifiedPattern();
		p1.loadPatternFromVEFile("dataset/test/q1");
		p1.display();

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		p2.display();

		QuantifiedPattern p5 = new QuantifiedPattern();
		p5.loadPatternFromVEFile("dataset/test/q5");
		p5.display();

	}

	@Test
	public void OptFunctionalTest() {

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();

//		System.out
//				.println("++++++++++++++++++ match tester for pattern #2 ++++++++++++++++++++++++++++++++");
//		Opt2Matcher<VertexOInt, OrthogonalEdge> inspector4 = new Opt2Matcher<VertexOInt, OrthogonalEdge>();
//		System.out
//				.println("------------------ a single match, candidate = 1 in g1 ------------------------");
//		assertEquals(false, inspector4.isIsomorphic(p2, 0, g, 1));
//		System.out
//				.println("------------------ a single match, candidate = 2 in g1 ------------------------");
//		Opt2Matcher<VertexOInt, OrthogonalEdge> inspector5 = new Opt2Matcher<VertexOInt, OrthogonalEdge>();
//		assertEquals(true, inspector5.isIsomorphic(p2, 0, g, 2));
		System.out
				.println("------------------ a single match, candidate = 3 in g1 ------------------------");
		Opt2Matcher<VertexOInt, OrthogonalEdge> inspector6 = new Opt2Matcher<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector6.isIsomorphic(p2, 0, g, 3));

	}
}
