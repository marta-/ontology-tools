package edu.toronto.cs.ontools.prediction;

public interface Cache<K, V> {
	public V put(K key, V value);

	public V get(K key);

	public V safeGet(K key);

	public V remove(K key);

	public int size();

	public void clear();

	public boolean isEmpty();
}
