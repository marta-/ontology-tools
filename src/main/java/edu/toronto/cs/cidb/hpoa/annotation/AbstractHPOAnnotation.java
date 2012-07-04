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

import edu.toronto.cs.cidb.hpoa.ontology.Ontology;
import edu.toronto.cs.cidb.hpoa.utils.graph.BGraph;

public abstract class AbstractHPOAnnotation extends BGraph<AnnotationTerm>
		implements HPOAnnotation {
	public static final Side HPO = BGraph.Side.R;
	public static final Side ANNOTATION = BGraph.Side.L;

	protected Ontology hpo;

	public Ontology getOntology() {
		return this.hpo;
	}

	public AbstractHPOAnnotation(Ontology hpo) {
		this.hpo = hpo;
	}

	public abstract int load(File source);

	public void propagateHPOAnnotations() {
		for (AnnotationTerm t : this.getAnnotations()) {
			propagateHPOAnnotations(t);
		}
	}

	public void propagateHPOAnnotations(AnnotationTerm annTerm) {
		annTerm.propagateAnnotations(this);
	}

	public Set<String> getAnnotationIds() {
		return this.getNodesIds(ANNOTATION);
	}

	public Set<String> getHPONodesIds() {
		return this.getNodesIds(HPO);
	}

	public Collection<AnnotationTerm> getAnnotations() {
		return this.getNodes(ANNOTATION);
	}

	public Collection<AnnotationTerm> getHPONodes() {
		return this.getNodes(HPO);
	}

	public AnnotationTerm getAnnotationNode(String annId) {
		return this.getNode(annId, ANNOTATION);
	}

	public AnnotationTerm getHPONode(String id) {
		return this.getNode(id, HPO);
	}

	public Map<String, String> getPhenotypesWithAnnotation(String annId) {
		Map<String, String> results = new TreeMap<String, String>();
		AnnotationTerm omimNode = this.getAnnotationNode(annId);
		for (String hpId : omimNode.getNeighbors()) {
			String hpName = this.hpo != null ? this.hpo.getName(hpId) : hpId;
			results.put(hpId, hpName);
		}
		return results;
	}

	public Collection<AnnotationTerm> getAnnotations(String hpoId) {
		Collection<AnnotationTerm> result = new LinkedList<AnnotationTerm>();
		if (this.getHPONode(hpoId) != null
				&& this.getHPONode(hpoId).getNeighbors() != null) {
			for (String a : this.getHPONode(hpoId).getNeighbors()) {
				result.add(this.getAnnotationNode(a));
			}
		}
		return result;
	}
}
