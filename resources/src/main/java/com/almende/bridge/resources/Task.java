/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.net.URI;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Task.
 */
public class Task {
	private ObjectNode								config		= null;
	private ConcurrentSkipListMap<DateTime, URI>	candidates	= new ConcurrentSkipListMap<DateTime, URI>();

	/**
	 * Instantiates a new task.
	 */
	public Task() {}

	/**
	 * Instantiates a new task.
	 *
	 * @param config
	 *            the config
	 */
	public Task(ObjectNode config) {
		this.config = config;
	}

	/**
	 * Gets the config.
	 *
	 * @return the config
	 */
	public ObjectNode getConfig() {
		return config;
	}

	/**
	 * Sets the config.
	 *
	 * @param config
	 *            the new config
	 */
	public void setConfig(ObjectNode config) {
		this.config = config;
	}

	/**
	 * Sets the candidate.
	 *
	 * @param eta
	 *            the eta
	 * @param uri
	 *            the uri
	 */
	public void setCandidate(DateTime eta, URI uri) {
		candidates.put(eta, uri);
	}

	/**
	 * Gets the closest eta.
	 *
	 * @return the closest eta
	 */
	public DateTime getClosestEta() {
		return candidates.firstKey();
	}

	/**
	 * Gets the closest.
	 *
	 * @return the closest
	 */
	public URI getClosest() {
		if (candidates.size() > 0) {
			return candidates.firstEntry().getValue();
		} else {
			return null;
		}
	}

	/**
	 * Gets the next.
	 *
	 * @param lastTry
	 *            the last try
	 * @return the next
	 */
	public URI getNext(URI lastTry) {
		DateTime oldeta = null;
		for (Entry<DateTime, URI> entry : candidates.entrySet()) {
			if (entry.getValue().equals(lastTry)) {
				oldeta = entry.getKey();
				break;
			}
		}
		if (oldeta != null) {
			candidates.remove(oldeta);
			final Entry<DateTime, URI> next = candidates.ceilingEntry(oldeta);
			if (next != null) {
				return next.getValue();
			}
		}
		return null;
	}

}
