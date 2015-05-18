/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources.plans;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import com.almende.bridge.oldDataStructs.Task;
import com.almende.eve.scheduling.Scheduler;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Goto.
 */
public class Goto extends Plan {

	enum STATE {
		init, travel, stay, finished
	};

	private static final String			TITLE			= "Go towards goal location.";
	private static final String[]		DESCRIPTIONS	= { "Task not started",
			"Traveling towards goal", "Staying at goal", "Task finished"	};
	protected STATE						status			= STATE.init;
	private Feature						goal			= null;

	private static final TypeUtil<Task>	TASK			= new TypeUtil<Task>() {};

	/**
	 * Instantiates a new goto.
	 *
	 * @param scheduler
	 *            the scheduler
	 * @param config
	 *            the config
	 */
	public Goto(Scheduler scheduler, ObjectNode config) {
		super(scheduler, config);
		if (config.has("goal")) {
			this.goal = FEATURE.inject(config.get("goal"));
		} else if (config.has("task")) {
			final Task task = TASK.inject(config.get("task"));
			final Feature feature = new Feature();
			final Point point = new Point();
			point.setCoordinates(new LngLatAlt(Double.valueOf(task.getLon()),
					Double.valueOf(task.getLat())));
			feature.setGeometry(point);
			feature.setId("tempLocation");
			//TODO: feature.setProperty("icon", node.get("icon").asText());
			this.goal = feature;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.bridge.resources.plans.Plan#getCurrentTitle()
	 */
	@Override
	public String getCurrentTitle() {
		return DESCRIPTIONS[status.ordinal()];
	}

	@Override
	public String[] getLocations() {
		if (goal != null) {
			return new String[] { goal.getId() };
		}
		return new String[0];
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.bridge.resources.plans.Plan#getTargetLocation()
	 */
	@Override
	public Feature getTargetLocation() {
		return goal;
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.bridge.resources.plans.Plan#arrival()
	 */
	@Override
	public void arrival() {
		if (status == STATE.travel) {
			doStateChange(STATE.finished.name());
		} else {
			doStateChange(STATE.travel.name());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.almende.bridge.resources.plans.Plan#doStateChange(java.lang.String)
	 */
	@Override
	public void doStateChange(String stateName) {
		status = STATE.valueOf(stateName);
		super.doStateChange(stateName);
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.bridge.resources.plans.Plan#getStatus()
	 */
	@Override
	public String getStatus() {
		return status.name();
	}

	@Override
	public String getTitle() {
		return TITLE;
	}
}
