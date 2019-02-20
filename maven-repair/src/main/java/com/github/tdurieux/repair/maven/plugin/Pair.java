package com.github.tdurieux.repair.maven.plugin;

/**
 * Created by thomas on 10/07/17.
 */
public class Pair<K,V> {
	private K key;
	private V value;

	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}
}
