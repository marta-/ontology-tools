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

import edu.toronto.cs.cidb.hpoa.ontology.HPO;
import edu.toronto.cs.cidb.hpoa.ontology.Ontology;
import edu.toronto.cs.cidb.hpoa.ontology.OntologyTerm;
import edu.toronto.cs.cidb.hpoa.utils.graph.IDAGNode;
import edu.toronto.cs.cidb.hpoa.utils.graph.Node;

public class AnnotationTerm extends Node {

	private OntologyTerm ontologyTerm;

	private List<String> originalAnnotations = new LinkedList<String>();

	public AnnotationTerm(String id) {
		super(id);
	}

	public AnnotationTerm(String id, String name) {
		super(id, name);
	}

	public void setOntologyTerm(OntologyTerm ontologyTerm) {
		this.ontologyTerm = ontologyTerm;
	}

	public OntologyTerm getOntologyTerm() {
		return this.ontologyTerm;
	}

	public List<String> getOriginalAnnotations() {
		return this.originalAnnotations;
	}

	protected void propagateAnnotations(HPOAnnotation ann) {
		this.originalAnnotations.addAll(this.getNeighbors());

		Set<String> newAnnotations = new HashSet<String>();
		Set<String> front = new HashSet<String>();

		front.addAll(this.getNeighbors());
		Set<String> newFront = new HashSet<String>();
		while (!front.isEmpty()) {
			for (String nextTermId : front) {
				IDAGNode nextNode = HPO.getInstance().getTerm(nextTermId);
				if (nextNode == null) {
					System.err.println("No matching term found in HPO for "
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
		for (String hpoId : newAnnotations) {
			ann.addConnection(this, new AnnotationTerm(HPO.getInstance()
					.getRealId(hpoId)));
		}
	}

	@Override
	public String toString() {
		Ontology hpo = HPO.getInstance();
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(" ").append(this.name).append("\n");
		for (String nodeId : this.getNeighbors()) {
			str.append("            ").append(nodeId).append("\t").append(
					hpo.getName(nodeId)).append("\n");
		}
		return str.toString();
	}
}
