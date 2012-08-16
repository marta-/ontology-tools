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
package edu.toronto.cs.ontools.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.toronto.cs.ontools.main.LocalFileUtils;
import edu.toronto.cs.ontools.taxonomy.Taxonomy;
import edu.toronto.cs.ontools.utils.graph.BGraph;

public class GeneHPOAnnotations extends AbstractTaxonomyAnnotation {
	public static final Side GENE = BGraph.Side.L;
	public static final Side HPO = BGraph.Side.R;

	private static final String COMMENT_MARKER = "#";

	private static final Pattern GENE_REG_EXP = Pattern
			.compile("([A-Z0-9]+)\\(([0-9]+)\\)");

	private static final Pattern ANNOTATION_REG_EXP = Pattern
			.compile("^(.*)\\s\\((HP:[0-9]{7})\\)\t\\[("
					+ GENE_REG_EXP.pattern() + "(,\\s" + GENE_REG_EXP.pattern()
					+ ")*)\\]$");

	private static final int NAME_IDX = 1;

	private static final int ID_IDX = 2;

	private static final int LIST_IDX = 3;

	public GeneHPOAnnotations(Taxonomy hpo) {
		super(hpo);
		this
				.load(LocalFileUtils
						.getInputFileHandler(
								"http://compbio.charite.de/svn/hpo/trunk/src/annotation/phenotype_to_genes.txt",
								false));
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
				Matcher m = ANNOTATION_REG_EXP.matcher(line);
				if (m.find()) {
					// String hpoName = m.group(NAME_IDX);
					final String hpoId = this.taxonomy.getRealId(m
							.group(ID_IDX));
					String geneList = m.group(LIST_IDX);
					if (geneList != null) {
						final Matcher mi = GENE_REG_EXP.matcher(geneList);
						while (mi.find()) {
							connection.clear();
							connection.put(GENE, new AnnotationTerm(mi
									.group(ID_IDX), mi.group(NAME_IDX)));
							connection.put(TAXONOMY, new AnnotationTerm(hpoId));
							this.addConnection(connection);
						}
					}
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

	public Set<String> getHPONodesIds() {
		return this.getNodesIds(HPO);
	}

	public Collection<AnnotationTerm> getHPONodes() {
		return this.getNodes(HPO);
	}

	public AnnotationTerm getHPONode(String omimId) {
		return this.getNode(omimId, HPO);
	}
}
