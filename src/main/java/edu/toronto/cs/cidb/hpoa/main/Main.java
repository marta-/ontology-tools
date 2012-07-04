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
package edu.toronto.cs.cidb.hpoa.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import edu.toronto.cs.cidb.hpoa.annotation.OmimHPOAnnotations;
import edu.toronto.cs.cidb.hpoa.ontology.HPO;
import edu.toronto.cs.cidb.hpoa.ontology.Ontology;
import edu.toronto.cs.cidb.hpoa.ontology.clustering.BottomUpAnnClustering;

public class Main {

	public static void main(String[] args) {
		Ontology hpo = HPO.getInstance();
		OmimHPOAnnotations ann = new OmimHPOAnnotations(hpo);
		ann
				.load(getInputFileHandler(
						"http://compbio.charite.de/svn/hpo/trunk/src/annotation/phenotype_annotation.tab",
						false));

		// hpo.display(System.out);
		BottomUpAnnClustering mfp = new BottomUpAnnClustering(hpo, ann);
		mfp.buttomUpCluster().display(System.out);
		// int min = ann.getAnnotationIds().size(), max = 0;
		// double avg = 0;
		// CounterMap<Integer> h = new CounterMap<Integer>();
		// for (String d : ann.getAnnotationIds()) {
		// Set<String> related = new HashSet<String>();
		// for (String s : ann.getAnnotationNode(d).getOriginalAnnotations()) {
		// related.addAll(ann.getNeighborIds(s));
		// }
		// if (min > related.size()) {
		// min = related.size();
		// }
		// if (max < related.size()) {
		// max = related.size();
		// }
		// avg += related.size();
		// h.addTo(related.size());
		// }
		// avg /= ann.getAnnotationIds().size();
		// PrintStream out = System.out;
		// out.println(h);
		// out.println();
		// out.println("MIN: " + min);
		// out.println("MAX: " + max);
		// out.println("AVG: " + avg);
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
