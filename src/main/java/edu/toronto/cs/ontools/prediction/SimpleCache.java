package edu.toronto.cs.ontools.prediction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleCache<K, V> {
	Map<K, V> cache = new HashMap<K, V>();
	List<K> accessOrder = new LinkedList<K>();
	private final int CACHE_SIZE_LIMIT;
	private static final int DEFAULT_CACHE_SIZE_LIMIT = 20000;

	public SimpleCache() {
		this(DEFAULT_CACHE_SIZE_LIMIT);
	}

	public SimpleCache(int limit) {
		this.CACHE_SIZE_LIMIT = limit > 0 ? limit : DEFAULT_CACHE_SIZE_LIMIT;
	}

	public V put(K key, V value) {
		V prevValue = this.get(key);
		if (prevValue != null) {
			this.accessOrder.remove(key);
		}
		this.cache.put(key, value);
		this.accessOrder.add(0, key);
		while (this.cache.size() > this.CACHE_SIZE_LIMIT) {
			this.cache.remove(this.accessOrder
					.remove(this.accessOrder.size() - 1));
		}
		return prevValue;
	}

	public V get(K key) {
		V result = this.cache.get(key);
		if (result != null && !this.accessOrder.get(0).equals(key)) {
			this.accessOrder.remove(key);
			this.accessOrder.add(0, key);
		}
		return result;
	}

	public V remove(K key) {
		this.accessOrder.remove(key);
		return this.cache.remove(key);
	}

	public int size() {
		return this.cache.size();
	}

	public void clear() {
		this.cache.clear();
		this.accessOrder.clear();
	}

	public boolean isEmpty() {
		return this.cache.isEmpty();
	}

}
