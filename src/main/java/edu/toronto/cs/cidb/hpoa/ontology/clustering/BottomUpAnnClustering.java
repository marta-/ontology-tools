package edu.toronto.cs.cidb.hpoa.ontology.clustering;

import java.util.Collection;
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

		Map<String, Double> adjustedDiseasePrecision = new HashMap<String, Double>();
		for (String d : this.annotation.getAnnotationIds()) {
			adjustedDiseasePrecision.put(d, 1.0);
		}
		Map<String, Double> crtDiseasePrecisionLoss = new HashMap<String, Double>();
		Map<String, String> crtReplacement = new HashMap<String, String>();
		double PRECISION_THRESHOLD = 1.1;// .995;

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
		// Set<String> L1 = new HashSet<String>();
		while (!crtLevel.isEmpty()) {
			List<SearchResult> sortedResults = new LinkedList<SearchResult>();
			for (String item : crtLevel) {
				sortedResults.add(new SearchResult(item, item, p
						.getSpecificity(item)));
			}
			Collections.sort(sortedResults);
			Collections.reverse(sortedResults);
			int lCount = 0;
			for (SearchResult r : sortedResults) {
				OntologyTerm term = this.ontology.getTerm(r.getId());
				if (term == null) {
					continue;
				}
				logln((++lCount) + "/" + crtLevel.size() + "\t" + term + "...");
				Collection<AnnotationTerm> diseases = this.annotation
						.getAnnotations(term.getId());
				boolean canContract = true;
				int dCount = 0;
				crtDiseasePrecisionLoss.clear();
				crtReplacement.clear();
				for (AnnotationTerm d : diseases) {
					logln("\t\t" + (++dCount) + "/" + diseases.size() + "\t"
							+ d.getId() + " " + d.getName());
					Collection<String> symptoms = d.getOriginalAnnotations();
					symptoms.remove(term.getId());
					double replacementScore = 0;
					for (String n : term.getParents()) {
						String pId = ontology.getRealId(n);
						symptoms.add(pId);
					}
					replacementScore = p.getMatchScore(symptoms, d.getId());

					logln("\t\t\treplcement score " + replacementScore);
					canContract = canContract
							&& ((replacementScore * adjustedDiseasePrecision
									.get(d.getId())) >= PRECISION_THRESHOLD);
					crtDiseasePrecisionLoss.put(d.getId(), replacementScore);
					if (!canContract) {
						break;
					}
				}
				logln("can contract " + term.getId() + "? " + canContract);
				if (canContract) {
					for (AnnotationTerm d : diseases) {
						if (crtDiseasePrecisionLoss.get(d.getId()) == null) {
							System.out.println(d.getId() + " => NULL!");
							System.out.println(diseases);
							System.out.println(crtDiseasePrecisionLoss);
							System.exit(0);

						}
						adjustedDiseasePrecision.put(d.getId(),
								adjustedDiseasePrecision.get(d.getId())
										* crtDiseasePrecisionLoss
												.get(d.getId()));
						d.getOriginalAnnotations().remove(term.getId());
						for (String n : term.getParents()) {
							d.getOriginalAnnotations().add(
									ontology.getRealId(n));
						}
						d.removeNeighbor(term.getId());

					}

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
}
