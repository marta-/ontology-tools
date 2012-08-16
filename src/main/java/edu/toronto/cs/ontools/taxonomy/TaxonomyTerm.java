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
package edu.toronto.cs.ontools.taxonomy;

import edu.toronto.cs.ontools.utils.graph.DAGNode;

public class TaxonomyTerm extends DAGNode {

	public TaxonomyTerm(String id) {
		super(id);
	}

	public TaxonomyTerm(String id, String name) {
		super(id, name);
	}

	public TaxonomyTerm(TermData data) {
		this(data.getId() + "", data.getName() + "");
		for (String parentId : data.get(TermData.PARENT_FIELD_NAME)) {
			this.addParent(parentId);
		}
	}

}
