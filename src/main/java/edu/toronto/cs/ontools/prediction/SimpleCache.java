package edu.toronto.cs.ontools.prediction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleCache<K, V> implements Cache<K, V> {
	protected Map<K, V> cache = new HashMap<K, V>();
	protected List<K> order = new LinkedList<K>();
	protected final V DEFAULT_VALUE;
	protected final int CACHE_SIZE_LIMIT;
	protected static final int DEFAULT_CACHE_SIZE_LIMIT = 50000000;

	public SimpleCache(V defaultValue) {
		this(defaultValue, DEFAULT_CACHE_SIZE_LIMIT);
	}

	public SimpleCache(V defaultValue, int limit) {
		this.DEFAULT_VALUE = defaultValue;
		this.CACHE_SIZE_LIMIT = limit > 0 ? limit : DEFAULT_CACHE_SIZE_LIMIT;
	}

	synchronized public V put(K key, V value) {
		V prevValue = this.get(key);
		if (prevValue != null) {
			this.order.remove(key);
		}
		this.cache.put(key, value);
		this.order.add(0, key);
		while (this.cache.size() > this.CACHE_SIZE_LIMIT) {
			this.cache.remove(this.order.remove(this.order.size() - 1));
		}
		return prevValue;
	}

	public V safeGet(K key) {
		V result = this.cache.get(key);
		if (result == null) {
			result = this.DEFAULT_VALUE;
		}
		return result;
	}

	public V get(K key) {
		return this.cache.get(key);
	}

	synchronized public V remove(K key) {
		this.order.remove(key);
		return this.cache.remove(key);
	}

	public int size() {
		return this.cache.size();
	}

	synchronized public void clear() {
		this.cache.clear();
		this.order.clear();
	}

	public boolean isEmpty() {
		return this.cache.isEmpty();
	}
}
