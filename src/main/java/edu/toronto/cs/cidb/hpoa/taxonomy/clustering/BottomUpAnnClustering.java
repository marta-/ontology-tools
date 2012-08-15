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
package edu.toronto.cs.cidb.hpoa.taxonomy.clustering;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import edu.toronto.cs.cidb.hpoa.annotation.AnnotationTerm;
import edu.toronto.cs.cidb.hpoa.annotation.SearchResult;
import edu.toronto.cs.cidb.hpoa.annotation.TaxonomyAnnotation;
import edu.toronto.cs.cidb.hpoa.prediction.ICPredictor;
import edu.toronto.cs.cidb.hpoa.prediction.Predictor;
import edu.toronto.cs.cidb.hpoa.taxonomy.HPO;
import edu.toronto.cs.cidb.hpoa.taxonomy.Taxonomy;
import edu.toronto.cs.cidb.hpoa.taxonomy.TaxonomyTerm;
import edu.toronto.cs.cidb.hpoa.utils.graph.DAGNode;
import edu.toronto.cs.cidb.hpoa.utils.maps.SetMap;

public class BottomUpAnnClustering {
	final Taxonomy taxonomy;
	final TaxonomyAnnotation annotation;;
	final Predictor predictor = new ICPredictor();

	private Map<String, Integer> ORIGINAL_RANKS = new HashMap<String, Integer>();
	final private File rankDataSource;
	private PrintStream log = System.out;

	// private rankData = new

	public BottomUpAnnClustering(Taxonomy taxonomy,
			TaxonomyAnnotation annotation) {
		this(taxonomy, annotation, null, null);
	}

	public BottomUpAnnClustering(Taxonomy taxonomy,
			TaxonomyAnnotation annotation, File rankDataSource, File log) {
		this.taxonomy = taxonomy;
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
			int rank = this.predictor.getRankForOwnTaxonomyTerms(d);
			out.println((counter++) + "/" + total + "\t" + d + "\t" + rank);
			this.ORIGINAL_RANKS.put(d, rank);
			out.flush();
		}
		if (!out.equals(System.out)) {
			out.close();
		}
	}

	public Taxonomy buttomUpCluster() {
		int removedNodes = 0;
		int removedArcs = 0;

		Taxonomy taxonomy = this.taxonomy;// .clone()
		this.predictor.setAnnotation(this.annotation);

		Set<String> crtLevel = new HashSet<String>();
		Set<String> nextLevel = new HashSet<String>();
		for (DAGNode t : this.taxonomy.getLeaves()) {
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
				TaxonomyTerm term = this.taxonomy.getTerm(r.getId());
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

	protected boolean remove(String phenotype, Predictor predictor) {
		AnnotationTerm aP = this.annotation.getTaxonomyNode(phenotype);
		if (aP == null) {
			return true;
		}
		TaxonomyTerm oP = HPO.getInstance().getTerm(phenotype);

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
			comparisonDiseasePool.addAll(this.annotation.getTaxonomyNode(p)
					.getNeighbors());
		}
		progress(phenotype + ": Related diseases / Comparison pool",
				relatedDiseases.size(), comparisonDiseasePool.size());

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

	protected static void generateMapping(Taxonomy hpo, String coreFileName,
			String inputFileName, String outputFileName) {
		PrintStream out;
		try {
			out = new PrintStream(getTemporaryFile(inputFileName
					+ "_hpo-core-mapping"));
		} catch (FileNotFoundException e1) {
			out = System.out;
			e1.printStackTrace();
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					getTemporaryFile(coreFileName)));
			String line;
			Set<String> core = new HashSet<String>();
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("HP:")) {
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

			in = new BufferedReader(new FileReader(
					getTemporaryFile(inputFileName)));

			int count = 0;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("HP:")) {
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
						TaxonomyTerm t = hpo.getTerm(tId);
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
			out.println("Initial DECIPHER phenotypes: " + count);
			out.println("New DECIPHER phenotypes:     " + newDHS.size());

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

	public static File getInputFileHandler(String inputLocation,
			boolean forceUpdate) {
		try {
			File result = new File(inputLocation);
			if (!result.exists()) {
				String name = inputLocation.substring(inputLocation
						.lastIndexOf('/') + 1);
				result = getTemporaryFile(name);
				if (!result.exists()) {
					result.createNewFile();
					BufferedInputStream in = new BufferedInputStream((new URL(
							inputLocation)).openStream());
					OutputStream out = new FileOutputStream(result);
					IOUtils.copy(in, out);
					out.flush();
					out.close();
				}
			}
			return result;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	protected static File getTemporaryFile(String name) {
		return getInternalFile(name, "tmp");
	}

	protected static File getInternalFile(String name, String dir) {
		File parent = new File("", dir);
		if (!parent.exists()) {
			parent.mkdirs();
		}
		return new File(parent, name);
	}
}
