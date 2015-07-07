package inf.ed.graph.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

public class PokecQueryGen {

	private Vector<Integer> lowProperties;
	private Vector<Integer> midProperties;
	private static int pid = 0;

	private static final int PERSON_NODE_LABEL = 1;
	private static final int FRIEND_EDGE_LABEL = 1;

	static String patternTemplateDir;
	static String outputbase;

	public PokecQueryGen() {
		loadProperties("dataset/pokec_freqedge_low_count", "dataset/pokec_freqedge_middle_count");
	}

	public int getEdgeType(int nodeID) {
		if (nodeID < 2000000) {
			return FRIEND_EDGE_LABEL;
		}
		return nodeID / 10000 - 200;
	}

	public void loadProperties(String lfilename, String mfilename) {
		lowProperties = new Vector<Integer>();
		midProperties = new Vector<Integer>();
		Scanner scanner;
		try {
			scanner = new Scanner(new File(lfilename));
			while (scanner.hasNextInt()) {
				lowProperties.add(scanner.nextInt());
			}
			scanner.close();

			scanner = new Scanner(new File(mfilename));
			while (scanner.hasNextInt()) {
				midProperties.add(scanner.nextInt());
			}
			scanner.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(lowProperties.toString());
		System.out.println("load low - properties.size =" + lowProperties.size());
		System.out.println(midProperties.toString());
		System.out.println("load mid - properties.size =" + midProperties.size());
	}

	public void randomizeAPattern(String filenamebase, int countBound, int percentL, int percentU) {

		Random r = new Random();
		int patternID = pid++;

		String inputfile = patternTemplateDir + "/" + filenamebase;
		String outputfile = outputbase + "/" + filenamebase;

		Scanner scanner;
		PrintWriter writer;
		Map<Integer, Integer> records = new HashMap<Integer, Integer>();
		try {

			System.out.println(inputfile);

			// process vertex file, replace A with attribute.
			writer = new PrintWriter(outputfile + "-" + patternID + ".v", "UTF-8");
			scanner = new Scanner(new File(inputfile + ".v"));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.contains("X")) {

					int vid = Integer.parseInt(line.substring(0, line.indexOf("\t")));
					int prop;
					if (vid == 1 || vid == 2) {
						prop = lowProperties.elementAt(r.nextInt(lowProperties.size()));
						while (records.values().contains(prop)) {
							prop = lowProperties.elementAt(r.nextInt(lowProperties.size()));
						}
					} else {
						prop = midProperties.elementAt(r.nextInt(midProperties.size()));
						while (records.values().contains(prop)) {
							prop = midProperties.elementAt(r.nextInt(midProperties.size()));
						}
					}
					line = line.replace("X", String.valueOf(prop));

					records.put(vid, prop);
				}
				writer.println(line);
			}
			scanner.close();
			writer.flush();
			writer.close();

			// process edge file, replace A with attribute.

			writer = new PrintWriter(outputfile + "-" + patternID + ".e", "UTF-8");
			scanner = new Scanner(new File(inputfile + ".e"));
			while (scanner.hasNextLine()) {
				String pred, value;
				int fromID, toID, toLabel;
				fromID = scanner.nextInt();
				toID = scanner.nextInt();
				pred = scanner.next();
				value = scanner.next();
				toLabel = records.containsKey(toID) ? records.get(toID) : PERSON_NODE_LABEL;
				if (!pred.equals("=") && !value.equals("0")) {
					if (value.contains("%")) {
						value = value.replace("X",
								String.valueOf(percentL + r.nextInt(percentU - percentL)));
					} else {
						value = value.replace("X", String.valueOf(r.nextInt(countBound)));
					}
				}
				writer.println(fromID + "\t" + toID + "\t" + getEdgeType(toLabel) + "\t" + pred
						+ "\t" + value);
				// System.out.println(fromID);
			}
			scanner.close();
			writer.flush();
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	static public void main(String[] args) {
		// if (args.length < 2) {

		patternTemplateDir = "dataset/ptns/template";
		outputbase = "dataset/ptns/gen";
		int genRound = 4;
		int countBound = 25;
		int percentL = 60;
		int percentU = 80;

		PokecQueryGen queryGen = new PokecQueryGen();

		File dir = new File(patternTemplateDir);
		File[] listOfFiles = dir.listFiles();
		for (File f : listOfFiles) {
			if (f.isFile() && f.getName().endsWith(".v")) {
				String filebasename = f.getName().substring(0, f.getName().length() - 2);
				for (int i = 0; i < genRound; i++) {
					queryGen.randomizeAPattern(filebasename, countBound, percentL, percentU);
				}
			}
		}

		System.out.println("finished.");
	}

}
