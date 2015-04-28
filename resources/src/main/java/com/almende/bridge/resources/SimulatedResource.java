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

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
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
	private int											index		= 0;
	private Duration									eta			= null;

	// other: {"lat":52.069451, "lon":4.640714}
	// work: {"lat":51.908913, "lon":4.479624}
	private double[]									geoJsonPos	= new double[] {
			4.479624, 51.908913, 0, 0								};
	private double[]									geoJsonGoal	= new double[] {
			4.479624, 51.908913, 0, 0								};
	private static final TypeUtil<ArrayList<double[]>>	ROUTETYPE	= new TypeUtil<ArrayList<double[]>>() {};

	public void onReady() {
		register();
	}

	/**
	 * Register agent at Proxy
	 */
	@Access(AccessType.PUBLIC)
	public void register() {
		try {
			call(new URI("local:proxy"), "register", null);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Error registering agent", e);
		}
	}

	/**
	 * Gets the current location of this resource
	 *
	 * @return the current location
	 */
	@Access(AccessType.PUBLIC)
	public synchronized ObjectNode getCurrentLocation() {
		final ObjectNode result = JOM.createObjectNode();
		if (route != null) {
			final long millis = new Duration(routeBase, DateTime.now())
					.getMillis();
			double[] pos = null;
			if (getEta().isBeforeNow()) {
				pos = route.get(route.size() - 1);
			} else {
				double[] last = null;
				for (int i = index; i < route.size(); i++) {
					double[] item = route.get(i);
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
					index = index > 0 ? index - 1 : 0;
				}
			}

			if (pos != null) {
				result.put("lon", pos[0]);
				result.put("lat", pos[1]);
				result.put("eta", getEtaString());
				geoJsonPos = pos;
			}
		} else {
			result.put("lon", geoJsonPos[0]);
			result.put("lat", geoJsonPos[1]);
		}
		result.put("name", getId());
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
	 * Gets the geo json description of this Resource.
	 *
	 * @param incTrack
	 *            Should the track data be included?
	 * @return the geo json
	 */
	@Access(AccessType.PUBLIC)
	public FeatureCollection getGeoJson(
			@Optional @Name("includeTrack") Boolean incTrack) {

		getCurrentLocation();
		
		final FeatureCollection fc = new FeatureCollection();
		fc.setProperty("id", getId());

		final Feature origin = new Feature();
		origin.setId(getId());
		final Point originPoint = new Point();
		originPoint.setCoordinates(new LngLatAlt(geoJsonPos[0], geoJsonPos[1]));
		origin.setGeometry(originPoint);
		origin.setProperty("type", "currentLocation");

		fc.add(origin);

		if (route != null) {
			if (incTrack != null && incTrack) {
				final Feature track = new Feature();
				track.setId(getId());
				final LineString tracksteps = new LineString();
				for (double[] step : route) {
					tracksteps.add(new LngLatAlt(step[0], step[1]));
				}
				track.setGeometry(tracksteps);
				track.setProperty("type", "route");
				fc.add(track);
			}
			final Feature goal = new Feature();
			goal.setId(getId());
			final Point goalPoint = new Point();
			goalPoint.setCoordinates(new LngLatAlt(geoJsonGoal[0],
					geoJsonGoal[1]));
			goal.setGeometry(goalPoint);
			goal.setProperty("type", "targetLocation");
			goal.setProperty("eta", getEtaString());

			fc.add(goal);
		}

		return fc;
	}

	/**
	 * Sets the location.
	 *
	 * @param lat
	 *            the lat
	 * @param lon
	 *            the lon
	 */
	@Access(AccessType.PUBLIC)
	public void setLocation(@Name("lat") double lat, @Name("lon") double lon) {
		setGeoJsonLocation(new double[] { lon, lat, 0, 0 });
	}

	/**
	 * Sets the geo json location.
	 *
	 * @param pos
	 *            the new geo json location
	 */
	@Access(AccessType.PUBLIC)
	public void setGeoJsonLocation(@Name("pos") double[] pos) {
		geoJsonPos = pos;
		if (route != null) {

		}
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
	public synchronized void setGoal(@Name("goal") ObjectNode goal)
			throws IOException {

		getCurrentLocation();
		geoJsonGoal[0] = goal.get("lon").asDouble();
		geoJsonGoal[1] = goal.get("lat").asDouble();

		final Params params = new Params();
		params.put("startLat", geoJsonPos[1]);
		params.put("startLon", geoJsonPos[0]);
		params.put("endLat", geoJsonGoal[1]);
		params.put("endLon", geoJsonGoal[0]);

		call(NAVAGENT, "getRoute", params, new AsyncCallback<ObjectNode>() {
			@Override
			public void onSuccess(ObjectNode result) {
				routeBase = DateTime.now();
				route = ROUTETYPE.inject(result.get("route"));
				index = 0;
				eta = new Duration(result.get("millis").asLong());
			}

			@Override
			public void onFailure(Exception exception) {
				LOG.log(Level.WARNING, "Couldn't get route:", exception);
				route = null;
				index = 0;
				eta = new Duration(0);
				routeBase = DateTime.now();
			}
		});
	}
}
