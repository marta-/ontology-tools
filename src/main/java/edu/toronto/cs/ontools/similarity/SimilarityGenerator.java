package edu.toronto.cs.ontools.similarity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import edu.toronto.cs.ontools.annotation.TaxonomyAnnotation;
import edu.toronto.cs.ontools.prediction.ICPredictor;
import edu.toronto.cs.ontools.prediction.Predictor;

public class SimilarityGenerator {
	public void generateSimilarityScores(TaxonomyAnnotation ann,
			String inputFileName, String outputFileName, boolean symmetric) {
		Predictor p = new ICPredictor();
		p.setAnnotation(ann);

		try {
			BufferedReader in = new BufferedReader(
					new FileReader(inputFileName));
			PrintStream out = new PrintStream(outputFileName);
			String line;
			int EXPECTED_LINE_PIECES = 2;
			int ANNOTATION_SET_POSITION = 1;
			int QUERY_SET_POSITION = 0;
			String SET_SEPARATOR = "\t+", ITEM_SEPARATOR = "\\s*[, ]\\s*", COMMENT_MARKER = "##";
			int counter = 0;
			while ((line = in.readLine()) != null) {
				++counter;
				if (line.startsWith(COMMENT_MARKER)) {
					continue;
				}
				String pieces[] = line.split(SET_SEPARATOR);
				if (pieces.length < EXPECTED_LINE_PIECES) {
					System.err.println("Unexpected format for line " + counter
							+ ":\n" + line + "\n\n");
					continue;
				}
				List<String> query = Arrays.asList(pieces[QUERY_SET_POSITION]
						.split(ITEM_SEPARATOR));
				List<String> ref = Arrays
						.asList(pieces[ANNOTATION_SET_POSITION]
								.split(ITEM_SEPARATOR));
				// System.err.println(query);
				// System.err.println(ref);
				out.println(line + "\t"
						+ p.getSimilarityScore(query, ref, symmetric));
			}
			out.flush();
			out.close();
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
