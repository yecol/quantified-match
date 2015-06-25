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
		// p1.display();

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();

	}

	// @Ignore
	@Test
	public void basicISOTest() {

		QuantifiedPattern p1 = new QuantifiedPattern();
		p1.loadPatternFromVEFile("dataset/test/q1");

		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector1 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
		assertEquals(true, inspector1.isQuantifedIsomorphic(p1, 0, g, 1));
		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector2 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		System.out.println("---------------------------------------------------");
		assertEquals(true, inspector2.isQuantifedIsomorphic(p1, 0, g, 2));
		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector3 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		System.out.println("---------------------------------------------------");
		assertEquals(false, inspector3.isQuantifedIsomorphic(p1, 0, g, 3));

		QuantifiedPattern p2 = new QuantifiedPattern();
		p2.loadPatternFromVEFile("dataset/test/q2");
		// p2.display();
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");

		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector4 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector4.isQuantifedIsomorphic(p2, 0, g, 1));
		System.out.println("---------------------------------------------------");
		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector5 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		assertEquals(true, inspector5.isQuantifedIsomorphic(p2, 0, g, 2));
		System.out.println("---------------------------------------------------");
		QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge> inspector6 = new QuantifiedIsomorphismInspector<VertexOInt, OrthogonalEdge>();
		assertEquals(false, inspector6.isQuantifedIsomorphic(p2, 0, g, 3));

	}
}
