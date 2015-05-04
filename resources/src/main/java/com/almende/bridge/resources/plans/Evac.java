/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources.plans;

import org.geojson.Feature;

import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.eve.scheduling.Scheduler;

/**
 * The Class Evac.
 */
public class Evac extends Plan {

	private enum STATE {
		init, toPickup, pickup, toDropOff, dropOff, finished
	};

	private Feature						hospital		= null;
	private Feature						pickupPoint		= null;
	// TODO: give a delay based on how busy the location is!
	private long						pickupDelay		= 160000;
	private long						dropOffDelay	= 120000;

	private static final String			TITLE			= "Evacuate wounded to hospital";
	private static final String[]		DESCRIPTIONS	= { "Task not started",
			"Proceed to rendezvous point", "Pickup patient",
			"Proceed to hospital", "Drop off patient", "Task finished" };

	private static final JSONRequest	pickupTask		= new JSONRequest(
																"plan.doStateChange",
																new Params(
																		"state",
																		"toDropOff"));
	private static final JSONRequest	dropOffTask		= new JSONRequest(
																"plan.doStateChange",
																new Params(
																		"state",
																		"finished"));

	private STATE						status			= STATE.init;

	/**
	 * Instantiates a new evac.
	 *
	 * @param scheduler
	 *            the scheduler
	 * @param hospital
	 *            the hospital
	 * @param pickupPoint
	 *            the pickup point
	 */
	public Evac(Scheduler scheduler, Feature hospital, Feature pickupPoint) {
		super(scheduler);
		this.hospital = hospital;
		this.pickupPoint = pickupPoint;
	}

	@Override
	public String getTitle() {
		return TITLE;
	}

	@Override
	public String getCurrentTitle() {
		return DESCRIPTIONS[status.ordinal()];
	}

	@Override
	public String getStatus() {
		return status.toString();
	}

	@Override
	public String[] getLocations() {
		return new String[] { hospital.getId(), pickupPoint.getId() };
	}

	@Override
	public Feature getTargetLocation() {
		if (status.equals(STATE.toPickup)) {
			return pickupPoint;
		} else if (status.equals(STATE.toDropOff)) {
			return hospital;
		}
		return null;
	}

	@Override
	public void arrival() {
		if (status.equals(STATE.toPickup)) {
			doStateChange("pickup");
		} else if (status.equals(STATE.toDropOff)) {
			doStateChange("dropOff");
		} else if (status.equals(STATE.init)) {
			doStateChange("toPickup");
		}
	}

	/**
	 * Schedule.
	 */
	public void schedule() {
		if (status.equals(STATE.pickup)) {
			scheduler.schedule(pickupTask, pickupDelay);
		} else if (status.equals(STATE.dropOff)) {
			scheduler.schedule(dropOffTask, dropOffDelay);
		}
	}

	@Override
	public void doStateChange(@Name("state") String state) {
		status = STATE.valueOf(state);
		super.doStateChange(state);
		schedule();
	}

}
