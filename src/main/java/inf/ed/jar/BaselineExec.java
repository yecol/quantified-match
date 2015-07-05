package inf.ed.jar;

import inf.ed.graph.quantified.BaseMatcher;
import inf.ed.graph.quantified.QuantifiedPattern;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;
import inf.ed.graph.util.Dev;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaselineExec {

	static Logger log = LogManager.getLogger(BaselineExec.class);
	static private final int candidateLimit = 50;

	static private ArrayList<Integer> findCandidates(Graph<VertexOInt, OrthogonalEdge> g,
			int filterLabel) {
		// begin label always = 1;
		ArrayList<Integer> cands = new ArrayList<Integer>();
		for (VertexOInt v : g.allVertices().values()) {
			if (v.getAttr() == 1 && v.isInnerNode()) {
				for (VertexOInt child : g.getChildren(v)) {
					if (child.getAttr() == filterLabel) {
						cands.add(v.getID());
						break;
					}
				}
				if (cands.size() > candidateLimit) {
					break;
				}
			}
		}
		return cands;
	}

	public static void main(String[] args) {

		System.out.println("args: graphfilename, patterndir");
		String graphFileName, patternDir;

		if (args.length < 2) {
			graphFileName = "dataset/test/g1";
			patternDir = "dataset/ptns";
		} else {
			graphFileName = args[0];
			patternDir = args[1];
		}

		// load graph into memory.

		log.info("load graph from file:" + graphFileName);
		long start = System.currentTimeMillis();
		Graph<VertexOInt, OrthogonalEdge> g = new OrthogonalGraph<VertexOInt>(VertexOInt.class);
		g.loadGraphFromVEFile(graphFileName, true);

		log.info("finish load graph, using " + (System.currentTimeMillis() - start) / 1000 + "s");
		log.info("node_size = " + g.vertexSize() + ", edgesize =" + g.edgeSize()
				+ ", using memory:" + Dev.currentRuntimeState());

		/****************************************************************************/

		File dir = new File(patternDir);
		File[] listOfFiles = dir.listFiles();
		int totalPattern = listOfFiles.length / 2;
		int i = 0;
		for (File f : listOfFiles) {
			if (f.isFile() && f.getName().endsWith(".v")) {
				i++;
				String patternName = f.getName().substring(0, f.getName().length() - 2);
				log.info("begin to process " + patternName + ", total = " + totalPattern
						+ ", cur = " + i);

				start = System.currentTimeMillis();
				QuantifiedPattern pp = new QuantifiedPattern();
				pp.loadPatternFromVEFile(patternDir + "/" + patternName);
				pp.display();

				ArrayList<Integer> candidates = findCandidates(g, pp.getGraph().getVertex(1)
						.getAttr());

				ArrayList<Integer> verified = new ArrayList<Integer>();

				log.info("got all candidates using " + (System.currentTimeMillis() - start) / 1000
						+ "s, size = " + candidates.size());

				long start2 = System.currentTimeMillis();

				int j = 0;
				for (int v : candidates) {
					j++;
					if (j % 1000 == 0) {
						log.info("processing pattern " + i + "/" + totalPattern + ", candidates "
								+ j + "/" + candidates.size());
					}
					BaseMatcher<VertexOInt, OrthogonalEdge> inspector = new BaseMatcher<VertexOInt, OrthogonalEdge>();
					boolean iso = inspector.isIsomorphic(pp, 0, g, v);
					if (iso == true) {
						log.info("cand-" + v + " is a match.");
						verified.add(v);
					} else {
						log.info("cand-" + v + " is not a match.");
					}
				}

				log.info("-stat- " + "pattern " + patternName + " iso = " + verified.size()
						+ ", using time = " + (System.currentTimeMillis() - start2) / 1000 + "s/"
						+ (System.currentTimeMillis() - start) / 1000 + "s");
			}
		}

		System.out.println("finished process");
	}
}
