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
package edu.toronto.cs.cidb.hpoa.utils.maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

interface NumericValueMap<K, N extends Number>
{

    public N addTo(K key, N value);

    public N reset(K key);

    public N get(K key);

    public N safeGet(K key);

    public List<K> sort();

    public List<K> sort(final boolean descending);

    public K getMax();

    public K getMin();

    public N getMaxValue();

    public N getMinValue();

    public void clear();

    public boolean containsKey(K key);

    public boolean isEmpty();

    public Set<K> keySet();

    public N put(K key, N value);

    public void putAll(Map< ? extends K, ? extends N> m);

    public N remove(K key);

    public int size();
}
