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
package edu.toronto.cs.cidb.hpoa.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.toronto.cs.cidb.hpoa.main.LocalFileUtils;
import edu.toronto.cs.cidb.hpoa.taxonomy.Taxonomy;
import edu.toronto.cs.cidb.hpoa.utils.graph.BGraph;

public class GeneGOAnnotations extends AbstractTaxonomyAnnotation {
	public static final Side GENE = BGraph.Side.L;
	public static final Side GO = BGraph.Side.R;

	private static final String COMMENT_MARKER = "!";

	private static final String[] VALID_RELS = { "" };

	private static final String SEPARATOR = "\t";

	private static final int MIN_EXPECTED_PIECES = 7;

	private static final int GENE_IDX = 1;

	private static final int REL_IDX = 3;

	private static final int GO_IDX = 4;

	private static final int EVIDENCE_IDX = 6;

	private List<String> validEvds = new LinkedList<String>();
	private List<String> validRels = Arrays.asList(VALID_RELS);

	public GeneGOAnnotations(Taxonomy go, List<String> evidenceSources) {
		super(go);
		if (evidenceSources != null) {
			this.validEvds.addAll(evidenceSources);
		}
		this
				.load(LocalFileUtils
						.getInputFileHandler(
								"http://www.cs.toronto.edu/~marta/d/gene_association.goa_human",
								false));
	}

	public void setValidEvidenceSources(String input) {
		this.setValidEvidenceSources(input.split("\\s*[, ]\\s*"));
	}

	public void setValidEvidenceSources(String[] input) {
		this.validEvds = Arrays.asList(input);
	}
	
	public void setValidRels(String input) {
		this.setValidRels(input.split("\\s*[, ]\\s*"));
	}

	public void setValidRels(String[] input) {
		this.validRels = Arrays.asList(input);
	}

	@Override
	public int load(File source) {
		// Make sure we can read the data
		if (source == null) {
			return -1;
		}
		// Load data
		clear();
		try {
			BufferedReader in = new BufferedReader(new FileReader(source));
			String line;
			Map<Side, AnnotationTerm> connection = new HashMap<Side, AnnotationTerm>();
			while ((line = in.readLine()) != null) {
				if (line.startsWith(COMMENT_MARKER)) {
					continue;
				}
				String pieces[] = line.split(SEPARATOR);
				if (pieces.length < MIN_EXPECTED_PIECES) {
					System.err.println("Unexpected line format: " + line);
					continue;
				}
				if (this.validEvds.contains(pieces[EVIDENCE_IDX])
						&& this.validRels.contains(pieces[REL_IDX])) {
					connection.clear();
					connection.put(GENE, new AnnotationTerm(pieces[GENE_IDX],
							pieces[GENE_IDX]));
					connection.put(GO, new AnnotationTerm(pieces[GO_IDX]));
					this.addConnection(connection);
				}
			}
			in.close();
			propagateTaxonomyAnnotations();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
			System.err.println("File does not exist");
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			System.err.println("Could not locate source file: "
					+ source.getAbsolutePath());
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return size();
	}

	public Set<String> getGeneIds() {
		return this.getNodesIds(GENE);
	}

	public Collection<AnnotationTerm> getGeneodes() {
		return this.getNodes(GENE);
	}

	public AnnotationTerm getGeneNode(String geneId) {
		return this.getNode(geneId, GENE);
	}

	public Set<String> getGONodesIds() {
		return this.getNodesIds(GO);
	}

	public Collection<AnnotationTerm> getGONodes() {
		return this.getNodes(GO);
	}

	public AnnotationTerm getGONode(String omimId) {
		return this.getNode(omimId, GO);
	}
}
