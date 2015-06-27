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

public class FunctionalTest {

	static Logger log = LogManager.getLogger(FunctionalTest.class);

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

	@Ignore
	@Test
	public void basicISOTest() {

		QuantifiedPattern p1 = new QuantifiedPattern();
		p1.loadPatternFromVEFile("dataset/test/q1");

		System.out
				.println("++++++++++++++++++ match tester for pattern #1 ++++++++++++++++++++++++++++++++");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector1 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 1 in g1 ------------------------");
		assertEquals(true, inspector1.isIsomorphic(p1, 0, g, 1));
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector2 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 2 in g1 ------------------------");
		assertEquals(true, inspector2.isIsomorphic(p1, 0, g, 2));
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector3 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 3 in g1 ------------------------");
		assertEquals(false, inspector3.isIsomorphic(p1, 0, g, 3));

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();

		System.out
				.println("++++++++++++++++++ match tester for pattern #2 ++++++++++++++++++++++++++++++++");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector4 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 1 in g1 ------------------------");
		assertEquals(false, inspector4.isIsomorphic(p2, 0, g, 1));
		System.out
				.println("------------------ a single match, candidate = 2 in g1 ------------------------");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector5 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		assertEquals(true, inspector5.isIsomorphic(p2, 0, g, 2));
		System.out
				.println("------------------ a single match, candidate = 3 in g1 ------------------------");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector6 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector6.isIsomorphic(p2, 0, g, 3));

	}
	
	@Test
	public void BaselineISOTest() {

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();

		System.out
				.println("++++++++++++++++++ match tester for pattern #2 ++++++++++++++++++++++++++++++++");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector4 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 1 in g1 ------------------------");
		assertEquals(false, inspector4.isIsomorphic(p2, 0, g, 1));
		System.out
				.println("------------------ a single match, candidate = 2 in g1 ------------------------");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector5 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		assertEquals(true, inspector5.isIsomorphic(p2, 0, g, 2));
		System.out
				.println("------------------ a single match, candidate = 3 in g1 ------------------------");
		BaseMatcher<VertexOInt, OrthogonalEdge> inspector6 = new BaseMatcher<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector6.isIsomorphic(p2, 0, g, 3));

	}

	@Test
	public void OptFunctionalTest() {

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();

		System.out
				.println("++++++++++++++++++ match tester for pattern #2 ++++++++++++++++++++++++++++++++");
		Opt1Matcher<VertexOInt, OrthogonalEdge> inspector4 = new Opt1Matcher<VertexOInt, OrthogonalEdge>();
		System.out
				.println("------------------ a single match, candidate = 1 in g1 ------------------------");
		assertEquals(false, inspector4.isIsomorphic(p2, 0, g, 1));
		System.out
				.println("------------------ a single match, candidate = 2 in g1 ------------------------");
		Opt1Matcher<VertexOInt, OrthogonalEdge> inspector5 = new Opt1Matcher<VertexOInt, OrthogonalEdge>();
		assertEquals(true, inspector5.isIsomorphic(p2, 0, g, 2));
		System.out
				.println("------------------ a single match, candidate = 3 in g1 ------------------------");
		Opt1Matcher<VertexOInt, OrthogonalEdge> inspector6 = new Opt1Matcher<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector6.isIsomorphic(p2, 0, g, 3));

	}
}
