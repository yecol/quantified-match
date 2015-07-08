package inf.ed.jar;

import inf.ed.graph.quantified.MtOptMatcher;
import inf.ed.graph.quantified.QuantifiedPattern;
import inf.ed.graph.structure.Graph;
import inf.ed.graph.structure.OrthogonalEdge;
import inf.ed.graph.structure.OrthogonalGraph;
import inf.ed.graph.structure.adaptor.VertexOInt;
import inf.ed.graph.util.Dev;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DQMatchExec {

	static Logger log = LogManager.getLogger(DQMatchExec.class);
	static private int candidateLimit = 0;
	static private int secondsLimit = 0;
	static private int threadLimit = 0;

	static public int getLabelOfBeginNode(int attr) {
		if (attr < 100000000) {
			// pokec others
			return attr;
		} else {
			return attr / 10000000;
		}
	}

	static public ArrayList<Integer> findCandidates(Graph<VertexOInt, OrthogonalEdge> g,
			QuantifiedPattern p) {
		ArrayList<Integer> cands = new ArrayList<Integer>();
		for (int vid : g.allVertices().keySet()) {
			if (g.getVertex(vid).isInnerNode()
					&& getLabelOfBeginNode(g.getVertex(vid).getAttr()) == p.getGraph().getVertex(0)
							.getAttr()) {

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
				}

				if (hasChildLabel1 && hasChildLabel2) {
					cands.add(vid);
				}

			}
		}
		return cands;
	}

	public static void main(String[] args) {

		System.out
				.println("args: graphfilename, patterndir, candidate-limit, time-limit, thread-limit");
		String graphFileName, patternDir;

		if (args.length < 2) {
			graphFileName = "dataset/test/g1";
			patternDir = "dataset/ptns";
			candidateLimit = 50;
			secondsLimit = 100;
		} else {
			graphFileName = args[0];
			patternDir = args[1];
			candidateLimit = Integer.parseInt(args[2]);
			secondsLimit = Integer.parseInt(args[3]);
			threadLimit = Integer.parseInt(args[4]);
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
				log.info("-stat- begin to process " + patternName + ", total = " + totalPattern
						+ ", cur = " + i);

				QuantifiedPattern pp = new QuantifiedPattern();
				pp.loadPatternFromVEFile(patternDir + "/" + patternName);
				pp.display();

				ArrayList<Integer> candidates = findCandidates(g, pp);

				long getCandidatesTime = (System.currentTimeMillis() - start) / 1000;

				log.info("-stat- got all candidates using " + getCandidatesTime / 1000
						+ "s, size = " + candidates.size());

				start = System.currentTimeMillis();

				MtOptMatcher<VertexOInt, OrthogonalEdge> inspector = new MtOptMatcher<VertexOInt, OrthogonalEdge>(
						pp, 0, g, candidates);
				inspector.setCandidateLimit(candidateLimit);
				inspector.setTimeout(secondsLimit);
				inspector.setThreadNumber(threadLimit);
				Set<Integer> verified = inspector.findIsomorphic();

				long usetime = System.currentTimeMillis() - start;
				double estTime = usetime * (candidates.size() * 1.0 / inspector.getCheckedSize());

				log.info("-stat- " + "pattern " + patternName + " verified = " + verified.size()
						+ ", check candidates = " + inspector.getCheckedSize() + "/"
						+ candidates.size() + ", using time = " + usetime / 1000 + "." + usetime
						% 1000 + "s, est.Time =" + estTime / 1000 + "s.");
			}
		}

		System.out.println("finished process");
	}
}
