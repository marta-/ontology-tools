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
package edu.toronto.cs.cidb.hpoa.taxonomy;

public class GO extends AbstractTaxonomy {

	private static GO instance;

	static {
		new GO();
	}

	private GO() {
		initialize();
	}

	public void initialize() {
		// if (this.service != null) {
		// this.load((SolrScriptService) this.service);
		// } else {
		this
				.load(getInputFileHandler(
						"http://www.geneontology.org/ontology/obo_format_1_2/gene_ontology_ext.obo",
						false));
		// }
		instance = this;
	}

	public static GO getInstance() {
		return instance;
	}
}