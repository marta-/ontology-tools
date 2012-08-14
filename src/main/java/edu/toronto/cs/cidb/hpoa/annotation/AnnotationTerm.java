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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.toronto.cs.cidb.hpoa.taxonomy.Taxonomy;
import edu.toronto.cs.cidb.hpoa.taxonomy.TaxonomyTerm;
import edu.toronto.cs.cidb.hpoa.utils.graph.IDAGNode;
import edu.toronto.cs.cidb.hpoa.utils.graph.Node;

public class AnnotationTerm extends Node {

	private TaxonomyTerm taxonomyTerm;
	private Taxonomy taxonomy = null;

	private List<String> originalAnnotations = new LinkedList<String>();

	public AnnotationTerm(String id) {
		super(id);
	}

	public AnnotationTerm(String id, String name) {
		super(id, name);
	}

	public void setTaxonomyTerm(TaxonomyTerm taxonomyTerm) {
		this.taxonomyTerm = taxonomyTerm;
	}

	public TaxonomyTerm getTaxonomyyTerm() {
		return this.taxonomyTerm;
	}

	public List<String> getOriginalAnnotations() {
		return this.originalAnnotations;
	}

	protected void propagateAnnotations(TaxonomyAnnotation ann,
			Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
		this.originalAnnotations.addAll(this.getNeighbors());

		Set<String> newAnnotations = new HashSet<String>();
		Set<String> front = new HashSet<String>();

		front.addAll(this.getNeighbors());
		Set<String> newFront = new HashSet<String>();
		while (!front.isEmpty()) {
			for (String nextTermId : front) {
				IDAGNode nextNode = taxonomy.getTerm(nextTermId);
				if (nextNode == null) {
					System.err
							.println("No matching term found in the taxonomy for "
									+ nextTermId + " (" + this + ")");
					continue;
				}
				for (String parentTermId : nextNode.getParents()) {
					if (!newAnnotations.contains(parentTermId)) {
						newFront.add(parentTermId);
						newAnnotations.add(parentTermId);
					}
				}
			}
			front.clear();
			front.addAll(newFront);
			newFront.clear();
		}
		newAnnotations.removeAll(this.getNeighbors());
		for (String tID : newAnnotations) {
			ann
					.addConnection(this, new AnnotationTerm(taxonomy
							.getRealId(tID)));
		}
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(" ").append(this.name).append("\n");
		for (String nodeId : this.getNeighbors()) {
			str.append("            ").append(nodeId);
			if (this.taxonomy != null) {
				str.append("\t").append(this.taxonomy.getName(nodeId));
			}
			str.append("\n");
		}
		return str.toString();
	}
}
