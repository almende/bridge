/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources.plans;

import com.almende.eve.scheduling.Scheduler;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class RoadBlock.
 */
public class RoadBlock extends Goto {

	private static final String		TITLE			= "Go towards goal location.";
	private static final String[]	DESCRIPTIONS	= { "Task not started",
			"Traveling towards location", "Keep location", "Task finished" };

	/**
	 * Instantiates a new road block.
	 *
	 * @param scheduler
	 *            the scheduler
	 * @param config
	 *            the config
	 */
	public RoadBlock(Scheduler scheduler, ObjectNode config) {
		super(scheduler, config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.bridge.resources.plans.Plan#arrival()
	 */
	@Override
	public void arrival() {
		if (status == STATE.travel) {
			doStateChange(STATE.stay.name());
		} else {
			doStateChange(STATE.travel.name());
		}
	}

	@Override
	public String getCurrentTitle() {
		return DESCRIPTIONS[status.ordinal()];
	}

	@Override
	public String getTitle() {
		return TITLE;
	}
}
