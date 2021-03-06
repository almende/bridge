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
import com.almende.bridge.resources.plans.GotoAndStay;
import com.almende.bridge.resources.plans.Plan;
import com.almende.eve.algorithms.EventBus;
import com.almende.eve.algorithms.agents.NodeAgent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Namespace;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
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
public class SimulatedResource extends NodeAgent {

	private static final Logger	LOG			= Logger.getLogger(SimulatedResource.class
													.getName());
	private static final URI	NAVAGENT	= URIUtil
													.create("http://localhost:8881/agents/navigation");

	private enum DEPLOYMENTSTATE {
		Unassigned, Assigned, Active, Withdrawn, Post
	}

	private DEPLOYMENTSTATE								deploymentState	= DEPLOYMENTSTATE.Unassigned;

	private EventBus									events			= null;

	private String										tag				= "empty";
	private String										guid			= new UUID()
																				.toString();

	private Route										route			= null;
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

	/**
	 * Instantiates a new simulated resource.
	 *
	 * @param id
	 *            the id
	 * @param config
	 *            the config
	 */
	public SimulatedResource(String id, ObjectNode config) {
		super(id, config);
	}

	/**
	 * Instantiates a new simulated resource.
	 */
	public SimulatedResource() {
		super();
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.plan = null;
		this.route = null;
		this.deploymentState = DEPLOYMENTSTATE.Unassigned;
		if (getConfig().has("initLocation")) {
			TypeUtil<double[]> typeutil = new TypeUtil<double[]>() {};
			setGeoJsonLocation(typeutil.inject(getConfig().get("initLocation")));
		}
	}

	/**
	 * Gets the event bus.
	 *
	 * @return the event bus
	 */
	@Namespace("event")
	public EventBus getEventBus() {
		return events;
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#onReady()
	 */
	public void onReady() {
		ObjectNode config = getConfig();
		if (config.has("initLocation")) {
			TypeUtil<double[]> typeutil = new TypeUtil<double[]>() {};
			setGeoJsonLocation(typeutil.inject(config.get("initLocation")));
		}
		if (config.has("resType")) {
			setResType(config.get("resType").asText());
		}
		if (config.has("guid")) {
			this.guid = config.get("guid").asText();
		}
		if (config.has("tag")) {
			this.tag = config.get("tag").asText();
		}
		if (config.has("icon")) {
			properties.put("icon", config.get("icon").asText());
		}
		register();
		if ("master".equals(tag)) {
			events = new EventBus(getScheduler(), caller, getGraph(), "SFN");
			addNode2SFN();
		}
	}

	/**
	 * Register agent at Proxy.
	 */
	public void register() {
		try {
			final Params params = new Params("tag", tag);
			call(new URI("local:proxy"), "register", params);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Error registering agent", e);
		}
	}

	/**
	 * Task request.
	 * -Check if busy
	 * -Check if capable of that task
	 * -Check distance to start (rough guess if reachable in time) (TODO,
	 * somewhat problematic due to datum issues)
	 * -Check ETA to start
	 * If all true, report possible match plus ETA.
	 *
	 * @param task
	 *            the task
	 * @param reportTo
	 *            the report to
	 */
	public void taskRequest(final @Name("task") ObjectNode task,
			final @Name("reportTo") URI reportTo) {
		String resType = getResType();
		if (task.has("resType")) {
			if (!resType.equals(task.get("resType").asText())) {
				return;
			}
		}
		if (deploymentState.equals(DEPLOYMENTSTATE.Unassigned)) {
			boolean capable = false;
			String planName = task.get("planName").asText();
			if ("Goto".equals(planName) || "GotoAndStay".equals(planName)) {
				capable = true;
			} else if (resType.equals("medic vehicle")) {
				if (task.get("planName").asText().equals("Evac")) {
					capable = true;
				}
			}
			if (!capable) {
				return;
			}
			// distance/speed on Highway (~80km/h)

			final Params params = new Params();
			params.put("startLat", geoJsonPos[1]);
			params.put("startLon", geoJsonPos[0]);
			params.put("endLat", task.get("lat").asDouble());
			params.put("endLon", task.get("lon").asDouble());

			try {
				getRoute(params, new AsyncCallback<ObjectNode>() {
					/*
					 * (non-Javadoc)
					 * @see
					 * com.almende.util.callback.AsyncCallback#onSuccess(java.lang
					 * .Object
					 * )
					 */
					@Override
					public void onSuccess(ObjectNode result) {
						Route myRoute = new Route();
						myRoute.routeBase = DateTime.now();
						myRoute.route = ROUTETYPE.inject(result.get("route"));
						myRoute.index = 0;
						myRoute.eta = new Duration(result.get("millis")
								.asLong());

						if (myRoute.routeBase.plus(myRoute.eta).isBefore(
								task.get("before").asLong())) {
							// Potential!
							Params params = new Params();
							params.add("task", task);
							params.add("eta", myRoute.eta.plus((long) Math
									.floor(Math.random() * 5000)));
							try {
								call(reportTo, "volunteer", params);
							} catch (IOException e) {
								LOG.log(Level.WARNING,
										"Couldn't volunteer for task", e);
							}
						}
					}

					@Override
					public void onFailure(Exception exception) {
						LOG.log(Level.WARNING, "Couldn't plan route:",
								exception);
					}
				});
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Couldn't plan route:", e);
			}
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
			final long millis = new Duration(route.routeBase, DateTime.now()
					.plus((long) (Math.random() * 1000))).getMillis();
			double[] pos = null;
			if (getEta().isBeforeNow()) {
				pos = route.route.get(route.route.size() - 1);
				route = null;
			} else {
				double[] last = null;
				for (int i = route.index; i < route.route.size(); i++) {
					double[] item = route.route.get(i);
					if (item[3] > millis) {
						if (last != null) {
							double length = item[3] - last[3];
							double latDiff = item[1] - last[1];
							double lonDiff = item[0] - last[0];
							double part = millis - last[3];

							final double[] loc = new double[4];
							loc[0] = last[0] + lonDiff * (part / length)
									+ (Math.random() * 0.0001 - 0.00005);
							loc[1] = last[1] + latDiff * (part / length)
									+ (Math.random() * 0.0001 - 0.00005);
							loc[2] = 0;
							loc[3] = millis;
							pos = loc;
						} else {
							pos = item;
						}
						break;
					}
					last = item;
					route.index = route.index > 0 ? route.index - 1 : 0;
				}
			}

			if (pos != null) {
				result.put("lon", pos[0]);
				result.put("lat", pos[1]);
				if (route != null) {
					result.put("eta", getEtaString());
				}
				geoJsonPos = pos;
			}
		} else {
			result.put("lon", geoJsonPos[0]);
			result.put("lat", geoJsonPos[1]);
		}
		if (properties.has("icon")) {
			result.put("icon", properties.get("icon").asText());
		}
		result.put("name", getId());
		return result;
	}

	/**
	 * Gets the eta.
	 *
	 * @return the eta
	 */
	@JsonIgnore
	public DateTime getEta() {
		if (route != null) {
			return route.routeBase.plus(route.eta);
		} else {
			return DateTime.now();
		}
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
			if (plan.getTargetLocation() != null) {
				feature.setProperty("targetId", plan.getTargetLocation()
						.getId());
			}
		}
	}

	private void addRouteProperties(Feature feature) {
		if (route != null) {
			feature.setProperty("eta", getEtaString());
			if (getEta().isAfterNow()) {
				Period period = new Duration(DateTime.now(), getEta())
						.toPeriod();
				feature.setProperty("minutesRemaining",
						period.toString(MINANDSECS));
				feature.setProperty("etaShort", getEta().toString("kk:mm:ss"));
			} else {
				feature.setProperty("minutesRemaining", 0);
				feature.setProperty("etaShort", "00:00:00");
			}
		}
	}

	/**
	 * Gets the geo json description of this Resource.
	 *
	 * @param incTrack
	 *            Should the track data be included?
	 * @param incTarget
	 *            the inc target
	 * @return the geo json
	 */

	public FeatureCollection getGeoJson(
			@Optional @Name("includeTrack") Boolean incTrack,
			@Optional @Name("includeTarget") Boolean incTarget) {
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
				final long millis = new Duration(route.routeBase,
						DateTime.now()).getMillis();

				for (double[] step : route.route) {
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
			if (incTarget != null && incTarget) {
				final Feature goal = new Feature();
				goal.setId(getId());
				final Point goalPoint = new Point();
				goalPoint.setCoordinates(new LngLatAlt(geoJsonGoal[0],
						geoJsonGoal[1]));
				goal.setGeometry(goalPoint);
				goal.setProperty("type", "targetLocation");
				addRouteProperties(goal);

				addProperties(goal);
				addTaskProperties(goal);

				fc.add(goal);
			}
			addRouteProperties(origin);
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
	 * @param id
	 *            the id
	 * @param repeat
	 *            the repeat
	 * @param sender
	 *            the sender
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void setPlan(@Name("plan") String planName,
			@Name("params") ObjectNode params, @Optional @Name("id") String id,
			@Optional @Name("repeat") Boolean repeat, @Sender URI sender)
			throws IOException {

		if (id != null) {
			// Check if Plan is still allowed.
			boolean confirm = true;
			if (plan != null && plan.getStatus() != "finished") {
				confirm = false;
			}
			// TODO: check if ETA still reachable? If not, probably nobody can.
			// Can we inform sender of this situation?
			final Params parms = new Params();
			parms.add("id", id);
			parms.add("confirm", confirm);
			call(sender, "acknowledge", parms);
			if (!confirm) {
				LOG.warning(getId()
						+ ": Not confirming plan, as I'm already doing something else.");
				return;
			}
		}
		if ("Evac".equals(planName)) {

			final ObjectNode config = JOM.createObjectNode();
			String title = "Goto location";
			if (params.has("title")) {
				title = params.get("title").asText();
			}
			if (params.has("task")) {
				config.set("task", params.get("task"));
			} else {
				final Params parms = new Params();
				parms.add("type", "hospital");
				parms.add("count", params.get("hospital").asInt());
				final Feature hospital = callSync(URIUtil.create("local:demo"),
						"getPoI", parms, Feature.class);
				config.set("hospital", JOM.getInstance().valueToTree(hospital));

				final Params parms2 = new Params();
				parms2.add("type", "rvpAmbu");
				parms2.add("count", params.get("rvpAmbu").asInt());
				final Feature pickup = callSync(URIUtil.create("local:demo"),
						"getPoI", parms2, Feature.class);
				config.set("pickupPoint", JOM.getInstance().valueToTree(pickup));
			}
			plan = new Evac(getScheduler(), config, title);

			plan.onStateChange("toPickup", NEXTLEGREQ);
			plan.onStateChange("toDropOff", NEXTLEGREQ);

		} else if ("GotoAndStay".equals(planName) || "Goto".equals(planName)) {
			final ObjectNode config = JOM.createObjectNode();
			String title = "Goto location";
			if (params.has("title")) {
				title = params.get("title").asText();
			}
			if (params.has("task")) {
				config.set("task", params.get("task"));
			} else {
				final Params parms = new Params();
				parms.add("type", params.get("poiType").asText());
				parms.add("count", params.get("poiNumber").asInt());
				final Feature feature = callSync(URIUtil.create("local:demo"),
						"getPoI", parms, Feature.class);

				config.set("goal", JOM.getInstance().valueToTree(feature));
			}
			plan = new GotoAndStay(getScheduler(), config, title,
					!"Goto".equals(planName));

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
		route = null;
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

	private void getRoute(final ObjectNode params,
			final AsyncCallback<ObjectNode> callback) throws IOException {
		call(NAVAGENT, "getRoute", params, callback);
	}

	private void planRoute() throws IOException {
		final Params params = new Params();
		params.put("startLat", geoJsonPos[1]);
		params.put("startLon", geoJsonPos[0]);
		params.put("endLat", geoJsonGoal[1]);
		params.put("endLon", geoJsonGoal[0]);
		getRoute(params, new AsyncCallback<ObjectNode>() {

			/*
			 * (non-Javadoc)
			 * @see
			 * com.almende.util.callback.AsyncCallback#onSuccess(java.lang.Object
			 * )
			 */
			@Override
			public void onSuccess(ObjectNode result) {
				if (route == null) {
					route = new Route();
				}
				route.routeBase = DateTime.now().plus(
						(long) (Math.random() * 10000));
				route.route = ROUTETYPE.inject(result.get("route"));
				route.index = 0;
				route.eta = new Duration(result.get("millis").asLong());
				checkArrival();
			}

			@Override
			public void onFailure(Exception exception) {
				LOG.log(Level.WARNING, "Couldn't get route:", exception);
				route = null;
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
		status.put("id", guid); // Some global uid, for .NET id
								// separation.
		status.put("type", getResType());
		status.put("deploymentStatus", deploymentState.toString());

		getCurrentLocation();
		Location location = new Location(new Double(geoJsonPos[1]).toString(),
				new Double(geoJsonPos[0]).toString(), DateTime.now().toString());
		if (location != null) {
			status.set("current", JOM.getInstance().valueToTree(location));
		}

		if (route != null) {
			Location goal = new Location(new Double(geoJsonGoal[1]).toString(),
					new Double(geoJsonGoal[0]).toString(), getEtaString());
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

	class Route {
		DateTime		routeBase	= DateTime.now();
		List<double[]>	route		= null;
		int				index		= 0;
		Duration		eta			= null;
	}
}
