package edu.toronto.cs.ontools.prediction;

public class AccessOrderCache<K, V> extends SimpleCache<K, V> {

	public AccessOrderCache(V defaultValue) {
		super(defaultValue);
	}

	public AccessOrderCache(V defaultValue, int limit) {
		super(defaultValue, limit);
	}

	@Override
	public V get(K key) {
		V result = this.cache.get(key);
		if (result != null) {
			if (!this.order.get(0).equals(key)) {
				this.order.remove(key);
				this.order.add(0, key);
			}
		} else {
			result = this.DEFAULT_VALUE;
		}
		return result;
	}
}
