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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.toronto.cs.cidb.hpoa.taxonomy.Taxonomy;
import edu.toronto.cs.cidb.hpoa.utils.graph.BGraph;

public abstract class AbstractTaxonomyAnnotation extends BGraph<AnnotationTerm>
		implements TaxonomyAnnotation {
	public static final Side TAXONOMY = BGraph.Side.R;
	public static final Side ANNOTATION = BGraph.Side.L;

	protected Taxonomy taxonomy;

	public Taxonomy getTaxonomy() {
		return this.taxonomy;
	}

	public AbstractTaxonomyAnnotation(Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
	}

	public abstract int load(File source);

	public void propagateTaxonomyAnnotations() {
		for (AnnotationTerm t : this.getAnnotations()) {
			propagateTaxonomyAnnotations(t);
		}
	}

	public void propagateTaxonomyAnnotations(AnnotationTerm annTerm) {
		annTerm.propagateAnnotations(this, this.taxonomy);
	}

	public Set<String> getAnnotationIds() {
		return this.getNodesIds(ANNOTATION);
	}

	public Set<String> getTaxonomyNodesIds() {
		return this.getNodesIds(TAXONOMY);
	}

	public Collection<AnnotationTerm> getAnnotations() {
		return this.getNodes(ANNOTATION);
	}

	public Collection<AnnotationTerm> getTaxonomyNodes() {
		return this.getNodes(TAXONOMY);
	}

	public AnnotationTerm getAnnotationNode(String annId) {
		return this.getNode(annId, ANNOTATION);
	}

	public AnnotationTerm getTaxonomyNode(String id) {
		return this.getNode(id, TAXONOMY);
	}

	public Map<String, String> getTaxonomyTermsWithAnnotation(String annId) {
		Map<String, String> results = new TreeMap<String, String>();
		AnnotationTerm annNode = this.getAnnotationNode(annId);
		for (String tId : annNode.getNeighbors()) {
			String tName = this.taxonomy != null ? this.taxonomy.getName(tId)
					: tId;
			results.put(tId, tName);
		}
		return results;
	}

	public Collection<AnnotationTerm> getAnnotations(String taconomyTermID) {
		Collection<AnnotationTerm> result = new LinkedList<AnnotationTerm>();
		if (this.getTaxonomyNode(taconomyTermID) != null
				&& this.getTaxonomyNode(taconomyTermID).getNeighbors() != null) {
			for (String a : this.getTaxonomyNode(taconomyTermID).getNeighbors()) {
				result.add(this.getAnnotationNode(a));
			}
		}
		return result;
	}
}
