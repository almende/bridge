/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimulatedResource.
 */
public class SimulatedResource extends Agent {
	private static final Logger							LOG			= Logger.getLogger(SimulatedResource.class
																			.getName());
	private static final URI							NAVAGENT	= URIUtil
																			.create("http://localhost:8080/agents/navigation");

	private DateTime									routeBase	= DateTime
																			.now();
	private List<double[]>								route		= null;
	private Duration									eta			= null;
	private static final TypeUtil<ArrayList<double[]>>	ROUTETYPE	= new TypeUtil<ArrayList<double[]>>() {};

	/**
	 * Gets the current location of this resource
	 *
	 * @return the current location
	 */
	@Access(AccessType.PUBLIC)
	public ObjectNode getCurrentLocation() {
		final ObjectNode result = JOM.createObjectNode();
		if (route != null) {
			final long millis = new Duration(routeBase, DateTime.now())
					.getMillis();
			double[] pos = null;
			if (getEta().isBeforeNow()) {
				pos = route.get(route.size() - 1);
			} else {
				double[] last = null;
				for (double[] item : route) {
					if (item[3] > millis) {
						if (last != null) {
							double length = item[3] - last[3];
							double latDiff = item[1] - last[1];
							double lonDiff = item[0] - last[0];
							double part = millis - last[3];

							final double[] loc = new double[4];
							loc[0] = last[0] + lonDiff * (part / length);
							loc[1] = last[1] + latDiff * (part / length);
							loc[2] = 0;
							loc[3] = millis;
							pos = loc;
						} else {
							pos = item;
						}
						break;
					}
					last = item;
				}
			}

			if (pos != null) {
				result.put("lon", pos[0]);
				result.put("lat", pos[1]);
			}
		} else {
			result.put("lon", 4.479624);
			result.put("lat", 51.908913);
		}
		return result;
	}

	/**
	 * Gets the eta.
	 *
	 * @return the eta
	 */
	@Access(AccessType.PUBLIC)
	public DateTime getEta() {
		return routeBase.plus(eta);
	}

	/**
	 * Gets the eta.
	 *
	 * @return the eta
	 */
	@Access(AccessType.PUBLIC)
	public String getEtaString() {
		return getEta().toString();
	}

	/**
	 * Sets the goal.
	 *
	 * @param goal
	 *            the new goal
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.PUBLIC)
	public void setGoal(@Name("goal") ObjectNode goal) throws IOException {

		// From current location:
		ObjectNode location = getCurrentLocation();
		// Home: 52.069451, 4.640714
		// Work: 51.908913, 4.479624

		final Params params = new Params();
		params.set("startLat", location.get("lat"));
		params.set("startLon", location.get("lon"));
		params.set("endLat", goal.get("lat"));
		params.set("endLon", goal.get("lon"));

		call(NAVAGENT, "getRoute", params, new AsyncCallback<ObjectNode>() {
			@Override
			public void onSuccess(ObjectNode result) {
				routeBase = DateTime.now();
				route = ROUTETYPE.inject(result.get("route"));
				eta = new Duration(result.get("millis").asLong());
			}

			@Override
			public void onFailure(Exception exception) {
				LOG.log(Level.WARNING, "Couldn't get route:", exception);
				route = null;
				eta = new Duration(0);
				routeBase = DateTime.now();
			}
		});
	}

}
