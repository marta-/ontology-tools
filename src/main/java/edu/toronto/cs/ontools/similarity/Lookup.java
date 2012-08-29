package edu.toronto.cs.ontools.similarity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.toronto.cs.ontools.annotation.GeneGOAnnotations;
import edu.toronto.cs.ontools.annotation.OmimHPOAnnotations;
import edu.toronto.cs.ontools.prediction.ICPredictor;
import edu.toronto.cs.ontools.prediction.Predictor;
import edu.toronto.cs.ontools.taxonomy.GO;
import edu.toronto.cs.ontools.taxonomy.HPO;

public class Lookup {
	private List<List<String>> referenceGO = new ArrayList<List<String>>();
	private List<List<String>> referenceHPO = new ArrayList<List<String>>();

	public void run(String queryFileName, String refFileName,
			String outputFileName, List<String> evidenceSources) {
		Predictor hP = new ICPredictor(), gP = new ICPredictor();

		System.out.print("Loading OMIM HPO annotations... ");
		System.out.flush();
		hP.setAnnotation(new OmimHPOAnnotations(new HPO()));
		System.out.println("Done");
		System.out.print("Loading Gene GO annotations... ");
		System.out.flush();
		gP.setAnnotation(new GeneGOAnnotations(new GO(), evidenceSources));
		System.out.println("Done");

		try {

			BufferedReader rIn = new BufferedReader(new FileReader(refFileName));

			String line;
			int EXPECTED_LINE_PIECES = 2;
			int GO_SET_POSITION = 0;
			int HPO_SET_POSITION = 1;
			String SET_SEPARATOR = "\t+", ITEM_SEPARATOR = "\\s*[, ]\\s*", COMMENT_MARKER = "##";
			System.out.print("Loading reference file... ");
			System.out.flush();
			// load the reference
			int counter = 0;
			while ((line = rIn.readLine()) != null) {
				++counter;
				if (counter % 20 == 0) {
					System.out.print(".");
					System.out.flush();
				}
				if (line.startsWith(COMMENT_MARKER)) {
					continue;
				}
				String pieces[] = line.split(SET_SEPARATOR);
				if (pieces.length < EXPECTED_LINE_PIECES) {
					System.err.println("[" + refFileName
							+ "] Unexpected format for line " + counter + ":\n"
							+ line + "\n\n");
					continue;
				}
				this.referenceGO.add(Arrays.asList(pieces[GO_SET_POSITION]
						.split(ITEM_SEPARATOR)));
				this.referenceHPO.add(Arrays.asList(pieces[HPO_SET_POSITION]
						.split(ITEM_SEPARATOR)));
			}
			int refSize = this.referenceGO.size();
			rIn.close();
			System.out.println(" Done");
			System.out.print("Processing query file... ");
			System.out.flush();
			BufferedReader qIn = new BufferedReader(new FileReader(
					queryFileName));
			PrintStream out = new PrintStream(outputFileName);
			counter = 0;
			while ((line = qIn.readLine()) != null) {
				++counter;
				System.out.print(".");
				System.out.flush();
				if (counter % 100 == 0) {
					System.out.print(counter);
					System.out.flush();
				}
				if (line.startsWith(COMMENT_MARKER)) {
					continue;
				}
				String pieces[] = line.split(SET_SEPARATOR);
				if (pieces.length < EXPECTED_LINE_PIECES) {
					System.err.println("[" + queryFileName
							+ "] Unexpected format for line " + counter + ":\n"
							+ line + "\n\n");
					continue;
				}
				List<String> qGo = Arrays.asList(pieces[GO_SET_POSITION]
						.split(ITEM_SEPARATOR));
				List<String> qHpo = Arrays.asList(pieces[HPO_SET_POSITION]
						.split(ITEM_SEPARATOR));
				double avgScore = 0;
				double maxScore = 0;
				for (int i = 0; i < refSize; ++i) {
					// System.out.println(qHpo + " " +
					// this.referenceHPO.get(i));
					double scoreHpo = hP.getSimilarityScore(qHpo,
							this.referenceHPO.get(i), false);
					// System.out.println("-->>" + scoreHpo);
					// System.out.println(qGo + " " + this.referenceGO.get(i));
					double scoreGo = (scoreHpo > 0) ? gP.getSimilarityScore(
							qGo, this.referenceGO.get(i), false) : 0;
					// System.out.println("-->>" + scoreGo);
					double score = scoreHpo * scoreGo;
					avgScore += score;
					maxScore = Math.max(score, maxScore);
				}
				avgScore /= refSize;
				out.println(line + "\t" + maxScore + "\t" + avgScore);
			}
			System.out.println(" Done");
			out.flush();
			out.close();
			qIn.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
