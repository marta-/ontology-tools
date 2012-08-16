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
package edu.toronto.cs.ontools.utils.graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

public class DAG<T extends DAGNode> {

	private TreeMap<String, T> nodes = new TreeMap<String, T>();

	public void clear() {
		this.nodes.clear();
	}

	public void addNode(T n) {
		this.nodes.put(n.getId(), n);
	}

	public Map<String, T> getNodesMap() {
		return this.nodes;
	}

	public Set<String> getNodesIds() {
		return this.nodes.keySet();
	}

	public Collection<T> getNodes() {
		return this.nodes.values();
	}

	public DAGNode getNode(String id) {
		return this.nodes.get(id);
	}

	public int size() {
		return this.nodes.size();
	}

	public List<DAGNode> getLeaves() {
		List<DAGNode> result = new LinkedList<DAGNode>();
		for (DAGNode n : this.nodes.values()) {
			if (n.getChildren().size() == 0) {
				result.add(n);
			}
		}
		return result;
	}

	public boolean removeNode(String id) {
		return this.removeNode(getNode(id));
	}

	public boolean removeNode(DAGNode node) {
		if (node == null) {
			return false;
		}
		for (String p : node.getParents()) {
			getNode(p).removeChild(node.getId());
		}
		for (String c : node.getChildren()) {
			getNode(c).removeParent(node.getId());
		}
		return (this.nodes.remove(node.getId()) != null);

	}

	@SuppressWarnings("unchecked")
	@Override
	public DAG<T> clone() {
		DAG<T> clone = new DAG<T>();
		for (Entry<String, T> entry : this.nodes.entrySet()) {
			clone.nodes.put(entry.getKey(), (T) entry.getValue().clone());
		}
		return clone;
	}
}
