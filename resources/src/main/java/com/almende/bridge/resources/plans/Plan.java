/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources.plans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geojson.Feature;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.scheduling.Scheduler;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Plan.
 */
@Access(AccessType.PUBLIC)
public abstract class Plan {

	protected ObjectNode						config		= null;

	protected Scheduler							scheduler;

	protected Map<String, List<JSONRequest>>	triggers	= new HashMap<String, List<JSONRequest>>();
	protected static final TypeUtil<Feature>	FEATURE		= new TypeUtil<Feature>() {};

	/**
	 * Instantiates a new plan.
	 *
	 * @param scheduler
	 *            the scheduler
	 * @param config
	 *            the config
	 */
	public Plan(Scheduler scheduler, ObjectNode config) {
		this.scheduler = scheduler;
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
	 * Gets the current title.
	 *
	 * @return the current title
	 */
	public abstract String getCurrentTitle();

	/**
	 * Gets the target location.
	 *
	 * @return the target location
	 */
	public abstract Feature getTargetLocation();

	/**
	 * Gets the locations.
	 *
	 * @return the locations
	 */
	public abstract String[] getLocations();

	/**
	 * Arrival.
	 */
	public abstract void arrival();

	/**
	 * Do state change.
	 *
	 * @param state
	 *            the state
	 */
	public void doStateChange(@Name("state") String state) {
		List<JSONRequest> requests = triggers.get(state);
		if (requests != null) {
			for (JSONRequest request : requests) {
				scheduler.schedule(request, 0);
			}
		}
	}

	/**
	 * On state change.
	 *
	 * @param state
	 *            the state
	 * @param request
	 *            the request
	 */
	public void onStateChange(@Name("state") String state,
			@Name("request") JSONRequest request) {
		if (!triggers.containsKey(state)) {
			triggers.put(state, new ArrayList<JSONRequest>());
		}
		triggers.get(state).add(request);
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public abstract String getStatus();

	/**
	 * Gets the global task title.
	 *
	 * @return the title
	 */
	public abstract String getTitle();

}
