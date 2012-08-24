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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.toronto.cs.ontools.annotation.SearchResult;
import edu.toronto.cs.ontools.annotation.TaxonomyAnnotation;
import edu.toronto.cs.ontools.utils.maps.CounterMap;
import edu.toronto.cs.ontools.utils.maps.SumMap;

public abstract class AbstractPredictor implements Predictor {
	protected TaxonomyAnnotation annotations;

	@Override
	public void setAnnotation(TaxonomyAnnotation annotations) {
		this.annotations = annotations;
	}

	@Override
	public List<SearchResult> getDifferentialTaxonomyTerms(
			Collection<String> taxonomyTermIDs) {
		List<SearchResult> result = new LinkedList<SearchResult>();
		SumMap<String> cummulativeScore = new SumMap<String>();
		CounterMap<String> matchCounter = new CounterMap<String>();
		List<SearchResult> matches = getMatches(taxonomyTermIDs);
		for (SearchResult r : matches) {
			String omimId = r.getId();
			for (String tID : this.annotations.getTaxonomyTermsWithAnnotation(
					omimId).keySet()) {
				if (taxonomyTermIDs.contains(tID)) {
					continue;
				}
				cummulativeScore.addTo(tID, r.getScore());
				matchCounter.addTo(tID);
			}
		}
		if (matchCounter.getMinValue() <= matches.size() / 2) {
			for (String tID : cummulativeScore.keySet()) {
				result.add(new SearchResult(tID, this.annotations.getTaxonomy()
						.getTerm(tID).getName(), cummulativeScore.get(tID)
						/ (matchCounter.get(tID) * matchCounter.get(tID))));
			}
			Collections.sort(result);
		}
		return result;
	}

	public int getMatchRank(Collection<String> taxonomyTermIDs,
			String annotationID) {
		int rank = 0;
		for (SearchResult r : this.getMatches(taxonomyTermIDs)) {
			++rank;
			if (r.getId().equals(annotationID)) {
				return rank;
			}
		}
		return -1;
	}

	public int getMatchRank(Collection<String> taxonomyTermIDs,
			String annotationID, int LIMIT) {
		int rank = 0;
		for (SearchResult r : this.getMatches(taxonomyTermIDs)) {
			if (++rank >= LIMIT) {
				break;
			}
			if (r.getId().equals(annotationID)) {
				return rank;
			}
		}
		return -1;
	}

	public int getRankForOwnTaxonomyTerms(String annotationID) {
		int rank = 1;
		List<String> annPhenotypes = this.annotations.getAnnotationNode(
				annotationID).getOriginalAnnotations();
		double ownScore = getMatchScore(annPhenotypes, annotationID);
		Set<String> pool = new HashSet<String>();
		for (String p : annPhenotypes) {
			pool.addAll(this.annotations.getTaxonomyNode(p).getNeighbors());
		}
		// System.out.print(resultID + "\trank out of " + pool.size() + "/"
		// + this.annotations.getAnnotationIds().size() + ":\t");
		// pool.remove(resultID);
		for (String d : pool) {
			if (getMatchScore(annPhenotypes, d) > ownScore) {
				++rank;
			}
		}
		// System.out.println(rank);
		return rank;
	}

	public double getSimilarityScore(Collection<String> query,
			Collection<String> reference) {
		return getSimilarityScore(query, reference, false);
	}
}
