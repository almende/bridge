/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.almende.bridge.oldDataStructs.Location;
import com.almende.bridge.resources.plans.Evac;
import com.almende.bridge.resources.plans.Goto;
import com.almende.bridge.resources.plans.Plan;
import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Namespace;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimulatedResource.
 */
@Access(AccessType.PUBLIC)
public class SimulatedResource extends Agent {
	private static final Logger	LOG			= Logger.getLogger(SimulatedResource.class
													.getName());
	private static final URI	NAVAGENT	= URIUtil
													.create("http://localhost:8881/agents/navigation");

	private enum DEPLOYMENTSTATE {
		Unassigned, Assigned, Active, Withdrawn, Post
	}

	private DEPLOYMENTSTATE								deploymentState	= DEPLOYMENTSTATE.Unassigned;

	private UUID										guid			= new UUID();

	private DateTime									routeBase		= DateTime
																				.now();
	private List<double[]>								route			= null;
	private int											index			= 0;
	private Duration									eta				= null;

	private Plan										plan			= null;

	// other: {"lat":52.069451, "lon":4.640714}
	// work: {"lat":51.908913, "lon":4.479624}
	private double[]									geoJsonPos		= new double[] {
			4.479624, 51.908913, 0, 0									};
	private double[]									geoJsonGoal		= new double[] {
			4.479624, 51.908913, 0, 0									};
	private static final TypeUtil<ArrayList<double[]>>	ROUTETYPE		= new TypeUtil<ArrayList<double[]>>() {};

	private static final JSONRequest					NEXTLEGREQ		= new JSONRequest(
																				"planNextLeg",
																				null);
	private static final JSONRequest					REPEATREQ		= new JSONRequest(
																				"repeat",
																				null);
	private static final JSONRequest					STOPREQ			= new JSONRequest(
																				"stop",
																				null);

	private static final PeriodFormatter				MINANDSECS		= new PeriodFormatterBuilder()
																				.printZeroAlways()
																				.appendMinutes()
																				.appendSeparator(
																						":")
																				.minimumPrintedDigits(
																						2)
																				.appendSeconds()
																				.toFormatter();

	private ObjectNode									properties		= JOM.createObjectNode();

	/*
	 * (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#onReady()
	 */
	public void onReady() {
		register();
	}

	/**
	 * Register agent at Proxy.
	 */
	public void register() {
		try {
			call(new URI("local:proxy"), "register", null);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Error registering agent", e);
		}
	}

	/**
	 * Gets the plan.
	 *
	 * @return the plan
	 */
	@Namespace("plan")
	@JsonIgnore
	public Plan getPlan() {
		return plan;
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public ObjectNode getProperties() {
		return properties;
	}

	/**
	 * Sets the properties.
	 *
	 * @param properties
	 *            the new properties
	 */
	public void setProperties(ObjectNode properties) {
		this.properties = properties;
	}

	/**
	 * Gets the res type.
	 *
	 * @return the res type
	 */
	public String getResType() {
		if (properties.has("resourceType")) {
			return properties.get("resourceType").asText();
		}
		return "<unknown>";
	}

	/**
	 * Sets the res type.
	 *
	 * @param type
	 *            the new res type
	 */
	public void setResType(String type) {
		properties.put("resourceType", type);
	}

	/**
	 * Gets the current location of this resource.
	 *
	 * @return the current location
	 */
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

	public DateTime getEta() {
		return routeBase.plus(eta);
	}

	/**
	 * Gets the eta.
	 *
	 * @return the eta
	 */

	public String getEtaString() {
		return getEta().toString();
	}

	private void addProperties(Feature feature) {
		Iterator<Entry<String, JsonNode>> iter = properties.fields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> field = iter.next();
			feature.setProperty(field.getKey(), field.getValue().asText());
		}
	}

	private void addTaskProperties(Feature feature) {
		if (plan != null) {
			// TODO: add taskTitle,taskAssigner,taskAssignmentDate,taskStatus
			feature.setProperty("taskTitle", plan.getCurrentTitle());
			feature.setProperty("taskStatus", plan.getStatus());
			feature.setProperty("taskLocations", plan.getLocations());
		}
	}

	/**
	 * Gets the geo json description of this Resource.
	 *
	 * @param incTrack
	 *            Should the track data be included?
	 * @return the geo json
	 */

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
		addProperties(origin);
		addTaskProperties(origin);
		// TODO: add resource icon

		fc.add(origin);

		if (route != null) {
			if (incTrack != null && incTrack) {
				final Feature track = new Feature();
				track.setId(getId());
				final LineString tracksteps = new LineString();
				tracksteps.add(new LngLatAlt(geoJsonPos[0], geoJsonPos[1]));
				final long millis = new Duration(routeBase, DateTime.now())
						.getMillis();

				for (double[] step : route) {
					if (step[3] > millis) {
						tracksteps.add(new LngLatAlt(step[0], step[1]));
					}
				}
				track.setGeometry(tracksteps);
				track.setProperty("type", "route");
				addProperties(track);
				addTaskProperties(track);
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
			if (getEta().isAfterNow()) {
				Period period = new Duration(DateTime.now(), getEta())
						.toPeriod();
				goal.setProperty("timeRemaining", period.toString(MINANDSECS));
				goal.setProperty("etaShort", getEta().toString("kk:mm:ss"));
			} else {
				goal.setProperty("minutesRemaining", 0);
				goal.setProperty("etaShort", "00:00:00");
			}
			goal.setProperty("targetId", plan.getTargetLocation().getId());
			addProperties(goal);
			addTaskProperties(goal);

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

	public void setLocation(@Name("lat") double lat, @Name("lon") double lon) {
		setGeoJsonLocation(new double[] { lon, lat, 0, 0 });
	}

	/**
	 * Sets the geo json location.
	 *
	 * @param pos
	 *            the new geo json location
	 */

	public void setGeoJsonLocation(@Name("pos") double[] pos) {
		geoJsonPos = pos;
	}

	/**
	 * Sets the plan.
	 *
	 * @param planName
	 *            the plan name
	 * @param params
	 *            the params
	 * @param repeat
	 *            the repeat
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void setPlan(@Name("plan") String planName,
			@Name("params") ObjectNode params,
			@Optional @Name("repeat") Boolean repeat) throws IOException {
		if ("Evac".equals(planName)) {
			final Params parms = new Params();
			parms.add("type", "hospital");
			parms.add("count", params.get("hospital").asInt());
			final Feature hospital = callSync(URIUtil.create("local:demo"),
					"getPoI", parms, Feature.class);

			final Params parms2 = new Params();
			parms2.add("type", "rvpAmbu");
			parms2.add("count", params.get("rvpAmbu").asInt());
			final Feature pickup = callSync(URIUtil.create("local:demo"),
					"getPoI", parms2, Feature.class);

			plan = new Evac(getScheduler(), hospital, pickup);

			plan.onStateChange("toPickup", NEXTLEGREQ);
			plan.onStateChange("toDropOff", NEXTLEGREQ);
		} else if ("Goto".equals(planName)) {
			final Params parms = new Params();
			parms.add("type", params.get("type").asText());
			parms.add("count", params.get("index").asInt());
			final Feature feature = callSync(URIUtil.create("local:demo"),
					"getPoI", parms, Feature.class);

			plan = new Goto(getScheduler(), feature);
			plan.onStateChange("travel", NEXTLEGREQ);
		}
		if (plan != null) {
			if (repeat != null && repeat) {
				plan.onStateChange("finished", REPEATREQ);
			} else {
				plan.onStateChange("finished", STOPREQ);
			}
			deploymentState = DEPLOYMENTSTATE.Active;
			plan.arrival();
		}
	}

	/**
	 * Repeat.
	 */
	public void repeat() {
		if (plan != null) {
			plan.doStateChange("init");
			plan.arrival();
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		deploymentState = DEPLOYMENTSTATE.Unassigned;
		plan = null;
	}

	/**
	 * Plan next leg.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void planNextLeg() throws IOException {
		if (plan != null) {
			Feature goal = plan.getTargetLocation();
			if (goal != null) {
				Point loc = (Point) goal.getGeometry();

				getCurrentLocation();
				geoJsonGoal[0] = loc.getCoordinates().getLongitude();
				geoJsonGoal[1] = loc.getCoordinates().getLatitude();
				planRoute();
			}
		}
	}

	/**
	 * Check arrival.
	 */
	public void checkArrival() {
		if (plan != null && getEta().isBeforeNow()) {
			plan.arrival();
		} else {
			schedule("checkArrival", null, getEta());
		}
	}

	private void planRoute() throws IOException {
		final Params params = new Params();
		params.put("startLat", geoJsonPos[1]);
		params.put("startLon", geoJsonPos[0]);
		params.put("endLat", geoJsonGoal[1]);
		params.put("endLon", geoJsonGoal[0]);

		call(NAVAGENT, "getRoute", params, new AsyncCallback<ObjectNode>() {

			/*
			 * (non-Javadoc)
			 * @see
			 * com.almende.util.callback.AsyncCallback#onSuccess(java.lang.Object
			 * )
			 */
			@Override
			public void onSuccess(ObjectNode result) {
				routeBase = DateTime.now();
				route = ROUTETYPE.inject(result.get("route"));
				index = 0;
				eta = new Duration(result.get("millis").asLong());

				checkArrival();
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

	/**
	 * Sets the goal.
	 *
	 * @param goal
	 *            the new goal
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */

	public synchronized void setGoal(@Name("goal") ObjectNode goal)
			throws IOException {

		getCurrentLocation();
		geoJsonGoal[0] = goal.get("lon").asDouble();
		geoJsonGoal[1] = goal.get("lat").asDouble();
		planRoute();
	}

	/**
	 * Request status.
	 *
	 * @return the object node
	 */
	public ObjectNode requestStatus() {
		ObjectNode status = JOM.createObjectNode();
		status.put("name", getId());
		status.put("id", guid.toString()); // Some global uid, for .NET id
											// separation.
		status.put("type", getResType());
		status.put("deploymentStatus", deploymentState.toString());

		getCurrentLocation();
		Location location = new Location(new Double(geoJsonPos[1]).toString(),
				new Double(geoJsonPos[0]).toString(), new Long(DateTime.now()
						.getMillis()).toString());
		if (location != null) {
			status.set("current", JOM.getInstance().valueToTree(location));
		}

		if (route != null) {
			Location goal = new Location(new Double(geoJsonGoal[1]).toString(),
					new Double(geoJsonGoal[0]).toString(), new Long(getEta()
							.getMillis()).toString());
			if (goal != null) {
				status.set("goal", JOM.getInstance().valueToTree(goal));
			}
		}

		if (plan != null) {
			String taskDescription = plan.getTitle() + " ("
					+ plan.getCurrentTitle() + ")";
			if (taskDescription != null) {
				status.put("task", taskDescription);
			}
		}
		return status;
	}
}
