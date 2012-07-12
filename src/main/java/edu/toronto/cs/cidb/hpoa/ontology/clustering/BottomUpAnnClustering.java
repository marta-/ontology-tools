package edu.toronto.cs.cidb.hpoa.ontology.clustering;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
	final HPOAnnotation annotation;

	public BottomUpAnnClustering(Ontology ontology, HPOAnnotation annotation) {
		this.ontology = ontology;
		this.annotation = annotation;
	}

	private void log(String msg) {
		System.out.print(msg);
	}

	private void logln(String msg) {
		// System.out.println(msg);
	}

	private void logln() {
		// System.out.println();
	}

	private void progress(String msg, int crt, int total) {
		System.out.println(msg + ": " + crt + "/" + total);
	}

	public Ontology buttomUpCluster() {
		int removedNodes = 0;
		int removedArcs = 0;

		Ontology ontology = this.ontology;// .clone();
		Predictor p = new ICPredictor();
		p.setAnnotation(this.annotation);

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
				sortedResults.add(new SearchResult(item, item, p
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
				if (remove(term.getId(), p)) {
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
			int RANK_LIMIT = 1;
			AnnotationTerm aD = this.annotation.getAnnotationNode(d);
			// the phenotypes we're looking for
			Set<String> annPhenotypes = new HashSet<String>();
			annPhenotypes.addAll(aD.getOriginalAnnotations());

			// their score for the taret disease
			double targetDiseaseScore = predictor.getMatchScore(annPhenotypes,
					d);
			int crtRank = 1;
			for (String cD : comparisonDiseasePool) {
				if (predictor.getMatchScore(annPhenotypes, cD) > targetDiseaseScore) {
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

		System.out.println(">>> REMOVED");
		return true;
	}
}
