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
package edu.toronto.cs.ontools.prediction;

import java.util.Collection;
import java.util.List;

import edu.toronto.cs.ontools.annotation.SearchResult;

public class BNPredictor extends AbstractPredictor {
	@Override
	public List<SearchResult> getMatches(Collection<String> taxonomyTermIDs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMatchScore(Collection<String> taxonomyTermIDs,
			String annotationID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSpecificity(String taxonomyTermID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSimilarityScore(Collection<String> query,
			Collection<String> reference, boolean symmetric) {
		// TODO Auto-generated method stub
		return 0;
	}
}
