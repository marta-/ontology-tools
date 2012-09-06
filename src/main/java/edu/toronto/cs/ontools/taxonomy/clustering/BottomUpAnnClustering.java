/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.ontools.taxonomy.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.toronto.cs.ontools.annotation.AnnotationTerm;
import edu.toronto.cs.ontools.annotation.SearchResult;
import edu.toronto.cs.ontools.annotation.TaxonomyAnnotation;
import edu.toronto.cs.ontools.main.AbstractCommandAction;
import edu.toronto.cs.ontools.main.LocalFileUtils;
import edu.toronto.cs.ontools.prediction.ICPredictor;
import edu.toronto.cs.ontools.prediction.Predictor;
import edu.toronto.cs.ontools.taxonomy.Taxonomy;
import edu.toronto.cs.ontools.taxonomy.TaxonomyTerm;
import edu.toronto.cs.ontools.utils.graph.DAGNode;
import edu.toronto.cs.ontools.utils.maps.SetMap;

public class BottomUpAnnClustering extends AbstractCommandAction {
	final TaxonomyAnnotation annotation;;
	final Predictor predictor = new ICPredictor();

	private Map<String, Integer> ORIGINAL_RANKS = new HashMap<String, Integer>();
	final private File rankDataSource;
	private PrintStream log = System.out;

	// private rankData = new

	public BottomUpAnnClustering(TaxonomyAnnotation annotation) {
		this(annotation, null, null);
	}

	public BottomUpAnnClustering(TaxonomyAnnotation annotation,
			File rankDataSource, File log) {
		this.annotation = annotation;
		this.predictor.setAnnotation(this.annotation);
		this.rankDataSource = rankDataSource;
		initOriginalRanks();
		if (log != null) {
			try {
				this.log = new PrintStream(log);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.log = System.out;
			}
		}
	}

	private void log(String msg) {
		this.log.print(msg);
	}

	private void logln(String msg) {
		// log.println(msg);
	}

	private void logln() {
		// log.println();
	}

	private void progress(String msg, int crt, int total) {
		this.log.println(msg + ": " + crt + "/" + total);
	}

	private void initOriginalRanks() {
		int counter = 1, total = this.annotation.getAnnotationIds().size();
		PrintStream out = System.out;
		if (this.rankDataSource != null) {
			if (this.rankDataSource.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(
							this.rankDataSource));
					log("Reading ranks from "
							+ this.rankDataSource.getAbsolutePath() + "...\n");
					for (String d : this.annotation.getAnnotationIds()) {
						this.ORIGINAL_RANKS.put(d, 1);
					}
					String line;
					while ((line = in.readLine()) != null) {
						String pieces[] = line.split("\t");
						if (pieces.length == 3) {
							int rank = 1;
							try {
								rank = Integer.parseInt(pieces[2]);
							} catch (NumberFormatException e) {
								rank = 1;
							}
							this.ORIGINAL_RANKS.put(pieces[1], rank);
						}
					}
					in.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					progress("????", 1, 2);
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					progress("????", 2, 2);
					e.printStackTrace();
				}
				return;
			} else {
				try {
					out = new PrintStream(this.rankDataSource);
					log("Writing ranks to "
							+ this.rankDataSource.getAbsolutePath() + "...\n");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		}
		for (String d : this.annotation.getAnnotationIds()) {
			int rank = this.predictor.getRankForOwnTaxonomyTerms(d);
			out.println((counter++) + "/" + total + "\t" + d + "\t" + rank);
			this.ORIGINAL_RANKS.put(d, rank);
			out.flush();
			if (isDebugMode()) {
				if (counter % 100 == 0) {
					System.out.println("\b\b\b\b\b"
							+ String.format("%5d", counter));
					System.out.flush();
				}
			}
		}
		if (!out.equals(System.out)) {
			out.close();
		}
	}

	public Taxonomy buttomUpCluster() {
		int removedNodes = 0;
		int removedArcs = 0;

		Taxonomy taxonomy = this.annotation.getTaxonomy();// .clone()
		this.predictor.setAnnotation(this.annotation);

		Set<String> crtLevel = new HashSet<String>();
		Set<String> nextLevel = new HashSet<String>();
		for (DAGNode t : taxonomy.getLeaves()) {
			// System.out.println("L0 " + t.getId() + " " + t.getName());
			crtLevel.add(t.getId());
		}
		logln();
		logln("crt level size = " + crtLevel.size());
		while (!crtLevel.isEmpty()) {
			List<SearchResult> sortedResults = new LinkedList<SearchResult>();
			for (String item : crtLevel) {
				sortedResults.add(new SearchResult(item, item, this.predictor
						.getSpecificity(item)));
			}
			Collections.sort(sortedResults);
			// Collections.reverse(sortedResults);
			int lCount = 0;
			for (SearchResult r : sortedResults) {
				TaxonomyTerm term = taxonomy.getTerm(r.getId());
				if (term == null) {
					continue;
				}
				logln((++lCount) + "/" + crtLevel.size() + "\t" + term + "...");
				if (remove(term.getId(), this.predictor)) {
					logln("REMOVING: " + term);
					removedNodes++;
					removedArcs += term.getNeighborsCount();
					for (String n : term.getParents()) {
						if (taxonomy.getTerm(n).getChildren().size() == 1) {
							nextLevel.add(taxonomy.getRealId(n));
						}
					}
					taxonomy.removeNode(term.getId());

				}
				if (lCount % 10 == 0 || lCount == crtLevel.size()) {
					progress("Level:   ", lCount, crtLevel.size());
					progress("Removed: ", removedNodes, taxonomy.size());
				}
			}
			logln("REMOVED: " + removedNodes + "n " + removedArcs + "a");
			crtLevel.clear();
			crtLevel.addAll(nextLevel);
			nextLevel.clear();
		}
		logln("TOTAL REMOVED: " + removedNodes + "n " + removedArcs + "a");
		progress("TOTAL REMOVED: ", removedNodes, taxonomy.size());
		return taxonomy;
	}

	protected boolean remove(String taxonomyTermID, Predictor predictor) {
		AnnotationTerm aP = this.annotation.getTaxonomyNode(taxonomyTermID);
		if (aP == null) {
			return true;
		}
		TaxonomyTerm oP = this.annotation.getTaxonomy().getTerm(taxonomyTermID);

		// Annotations for this taxonomy term
		Set<String> relatedAnnotations = new HashSet<String>();
		relatedAnnotations.addAll(aP.getNeighbors());
		// For all these annotations, replace the term with its parents in the
		// annotation graph
		for (String d : relatedAnnotations) {
			AnnotationTerm aD = this.annotation.getAnnotationNode(d);
			aD.removeNeighbor(taxonomyTermID);
			if (aD.getOriginalAnnotations().remove(taxonomyTermID)) {
				aD.getOriginalAnnotations().addAll(oP.getParents());
			}
		}

		// Annotations used as a subset against which the terms minus the
		// taxonomyTermID will be searched for similarities
		Set<String> comparisonAnnotationPool = new HashSet<String>();
		comparisonAnnotationPool.addAll(relatedAnnotations);

		for (String p : oP.getParents()) {
			comparisonAnnotationPool.addAll(this.annotation.getTaxonomyNode(p)
					.getNeighbors());
		}
		progress(taxonomyTermID + ": Related annotations / Comparison pool",
				relatedAnnotations.size(), comparisonAnnotationPool.size());

		// check where each annotation ranks in the pool
		boolean canRemove = true;
		for (String d : relatedAnnotations) {
			// acceptable rank for each annotation when searching for its
			// taxonomy terms
			int RANK_LIMIT = this.ORIGINAL_RANKS.get(d) == null ? 1
					: this.ORIGINAL_RANKS.get(d);
			AnnotationTerm aD = this.annotation.getAnnotationNode(d);
			// the terms we're looking for
			Set<String> annTaxonomyTerms = new HashSet<String>();
			annTaxonomyTerms.addAll(aD.getOriginalAnnotations());

			// their score for the target annotation
			double targetAnnScore = predictor
					.getMatchScore(annTaxonomyTerms, d);
			int crtRank = 1;
			for (String cD : comparisonAnnotationPool) {
				double matchScore = predictor.getMatchScore(annTaxonomyTerms,
						cD);
				if (matchScore > targetAnnScore) {
					// increase rank every time we find a higher score in the
					// comparison pool
					// terminate if rank limit is reached
					if (++crtRank > RANK_LIMIT) {
						canRemove = false;
						break;
					}
				}
			}
			if (!canRemove) {
				break;
			}
		}
		if (!canRemove) {
			// undo changes dome to the annotation graph
			for (String d : relatedAnnotations) {
				AnnotationTerm aD = this.annotation.getAnnotationNode(d);
				aD.addNeighbor(aP);
				if (aD.getOriginalAnnotations().add(taxonomyTermID)) {
					aD.getOriginalAnnotations().removeAll(oP.getParents());
				}
			}
			return false;
		}

		progress(">>> REMOVED", 1, 1);
		return true;
	}

	protected static void generateMapping(Taxonomy taxonomy,
			String coreFileName, String inputFileName, String outputFileName) {
		PrintStream out;
		try {
			out = new PrintStream(LocalFileUtils.getTemporaryFile(inputFileName
					+ "_hpo-core-mapping"));
		} catch (FileNotFoundException e1) {
			out = System.out;
			e1.printStackTrace();
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					LocalFileUtils.getTemporaryFile(coreFileName)));
			String line;
			Set<String> core = new HashSet<String>();
			while ((line = in.readLine()) != null) {
				if (!line.startsWith(taxonomy.getIDPrefix())) {
					continue;
				}
				String id = line.substring(0, 10);
				core.add(id);
			}
			in.close();

			SetMap<String, String> obsoleteTermMapping = new SetMap<String, String>();
			obsoleteTermMapping.addTo("HP:0000489", "HP:0100886");
			obsoleteTermMapping.addTo("HP:0000489", "HP:0100887");
			obsoleteTermMapping.addTo("HP:0009885", "HP:0004322");

			Set<String> newDHS = new HashSet<String>();

			in = new BufferedReader(new FileReader(LocalFileUtils
					.getTemporaryFile(inputFileName)));

			int count = 0;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith(taxonomy.getIDPrefix())) {
					continue;
				}
				++count;
				Set<String> replacements = new HashSet<String>();
				Set<String> front = new HashSet<String>();
				Set<String> next = new HashSet<String>();
				front.addAll(obsoleteTermMapping.safeGet(line.trim()));
				if (front.isEmpty()) {
					front.add(line.trim());
				}
				while (!front.isEmpty()) {
					for (String tId : front) {
						TaxonomyTerm t = taxonomy.getTerm(tId);
						if (t != null) {
							if (core.contains(t.getId())) {
								replacements.add(t.getId());
							} else {
								for (String p : t.getParents()) {
									next.add(p);
								}
							}
						}
					}
					front.clear();
					front.addAll(next);
					next.clear();
				}
				if (!replacements.contains(line.trim())) {
					out.println(line.trim() + "\t" + replacements);
				}
				newDHS.addAll(replacements);
			}
			in.close();

			out.println();
			// System.out.println("New # of leaves in ontology: " +
			// leaves.size());
			out.println("Initial terms count: " + count);
			out.println("New terms count:     " + newDHS.size());

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (out != System.out) {
			out.close();
		}
	}
}
