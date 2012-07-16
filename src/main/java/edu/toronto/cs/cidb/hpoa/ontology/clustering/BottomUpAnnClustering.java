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
package edu.toronto.cs.cidb.hpoa.ontology.clustering;

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

import edu.toronto.cs.cidb.hpoa.annotation.AnnotationTerm;
import edu.toronto.cs.cidb.hpoa.annotation.HPOAnnotation;
import edu.toronto.cs.cidb.hpoa.annotation.SearchResult;
import edu.toronto.cs.cidb.hpoa.ontology.HPO;
import edu.toronto.cs.cidb.hpoa.ontology.Ontology;
import edu.toronto.cs.cidb.hpoa.ontology.OntologyTerm;
import edu.toronto.cs.cidb.hpoa.prediction.ICPredictor;
import edu.toronto.cs.cidb.hpoa.prediction.Predictor;
import edu.toronto.cs.cidb.hpoa.utils.graph.DAGNode;

public class BottomUpAnnClustering {
	final Ontology ontology;
	final HPOAnnotation annotation;;
	final Predictor predictor = new ICPredictor();

	private Map<String, Integer> ORIGINAL_RANKS = new HashMap<String, Integer>();
	final private File rankDataSource;
	private PrintStream log = System.out;

	// private rankData = new

	public BottomUpAnnClustering(Ontology ontology, HPOAnnotation annotation) {
		this(ontology, annotation, null, null);
	}

	public BottomUpAnnClustering(Ontology ontology, HPOAnnotation annotation,
			File rankDataSource, File log) {
		this.ontology = ontology;
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
					log("Reading OMIM ranks from "
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
					log("Writing OMIM ranks to "
							+ this.rankDataSource.getAbsolutePath() + "...\n");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		}
		for (String d : this.annotation.getAnnotationIds()) {
			int rank = this.predictor.getRankForOwnSymptoms(d);
			out.println((counter++) + "/" + total + "\t" + d + "\t" + rank);
			this.ORIGINAL_RANKS.put(d, rank);
			out.flush();
		}
		if (!out.equals(System.out)) {
			out.close();
		}
	}

	public Ontology buttomUpCluster() {
		int removedNodes = 0;
		int removedArcs = 0;

		Ontology ontology = this.ontology;// .clone()
		this.predictor.setAnnotation(this.annotation);

		Set<String> crtLevel = new HashSet<String>();
		Set<String> nextLevel = new HashSet<String>();
		for (DAGNode t : this.ontology.getLeaves()) {
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
				OntologyTerm term = this.ontology.getTerm(r.getId());
				if (term == null) {
					continue;
				}
				logln((++lCount) + "/" + crtLevel.size() + "\t" + term + "...");
				if (remove(term.getId(), this.predictor)) {
					logln("REMOVING: " + term);
					removedNodes++;
					removedArcs += term.getNeighborsCount();
					for (String n : term.getParents()) {
						if (ontology.getTerm(n).getChildren().size() == 1) {
							nextLevel.add(ontology.getRealId(n));
						}
					}
					ontology.removeNode(term.getId());

				}
				if (lCount % 10 == 0 || lCount == crtLevel.size()) {
					progress("Level:   ", lCount, crtLevel.size());
					progress("Removed: ", removedNodes, ontology.size());
				}
			}
			logln("REMOVED: " + removedNodes + "n " + removedArcs + "a");
			crtLevel.clear();
			crtLevel.addAll(nextLevel);
			nextLevel.clear();
		}
		logln("TOTAL REMOVED: " + removedNodes + "n " + removedArcs + "a");
		progress("TOTAL REMOVED: ", removedNodes, ontology.size());
		return ontology;
	}

	protected boolean remove(String phenotype, Predictor predictor) {
		AnnotationTerm aP = this.annotation.getHPONode(phenotype);
		if (aP == null) {
			return true;
		}
		OntologyTerm oP = HPO.getInstance().getTerm(phenotype);

		// Diseases presenting this phenotype
		Set<String> relatedDiseases = new HashSet<String>();
		relatedDiseases.addAll(aP.getNeighbors());
		// For all these diseases, replace the phenotype with its parents in the
		// annotation graph
		for (String d : relatedDiseases) {
			AnnotationTerm aD = this.annotation.getAnnotationNode(d);
			aD.removeNeighbor(phenotype);
			if (aD.getOriginalAnnotations().remove(phenotype)) {
				aD.getOriginalAnnotations().addAll(oP.getParents());
			}
		}

		// Diseases used as a subset against which the symptoms minus the
		// phenotype will be searched for similarities
		Set<String> comparisonDiseasePool = new HashSet<String>();
		comparisonDiseasePool.addAll(relatedDiseases);

		for (String p : oP.getParents()) {
			comparisonDiseasePool.addAll(this.annotation.getHPONode(p)
					.getNeighbors());
		}
		progress(phenotype + ": Related diseases / Comparison pool",
				relatedDiseases.size(), comparisonDiseasePool.size());

		// Diseases presenting at least one phenotype of the related diseases
		// Used as a subset against which the symptoms minus the phenotype will
		// be searched for similarities

		// Set<String> relatedPhenotypes = new HashSet<String>();
		// for (String p : oP.getParents()) {
		// relatedPhenotypes.addAll(this.ontology.getTerm(p).getChildren());
		// }
		// relatedPhenotypes.addAll(oP.getParents());
		// for (String p : relatedPhenotypes) {
		// comparisonDiseasePool.addAll(this.annotation.getHPONode(p)
		// .getNeighbors());
		/*
		 * for (String d : relatedDiseases) { for (String p :
		 * this.annotation.getAnnotationNode(d) .getOriginalAnnotations()) {
		 * comparisonDiseasePool.addAll(this.annotation.getHPONode(p)
		 * .getNeighbors()); } }
		 */

		// check where each disease ranks in the pool
		boolean canRemove = true;
		for (String d : relatedDiseases) {
			// acceptable rank for each disease when searching for its symptoms
			int RANK_LIMIT = this.ORIGINAL_RANKS.get(d) == null ? 1
					: this.ORIGINAL_RANKS.get(d);
			AnnotationTerm aD = this.annotation.getAnnotationNode(d);
			// the phenotypes we're looking for
			Set<String> annPhenotypes = new HashSet<String>();
			annPhenotypes.addAll(aD.getOriginalAnnotations());

			// their score for the taret disease
			double targetDiseaseScore = predictor.getMatchScore(annPhenotypes,
					d);
			int crtRank = 1;
			for (String cD : comparisonDiseasePool) {
				double matchScore = predictor.getMatchScore(annPhenotypes, cD);
				if (matchScore > targetDiseaseScore) {
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
			// undo changes dome to the annotation grph
			for (String d : relatedDiseases) {
				AnnotationTerm aD = this.annotation.getAnnotationNode(d);
				aD.addNeighbor(aP);
				if (aD.getOriginalAnnotations().add(phenotype)) {
					aD.getOriginalAnnotations().removeAll(oP.getParents());
				}
			}
			return false;
		}

		progress(">>> REMOVED", 1, 1);
		return true;
	}
}
