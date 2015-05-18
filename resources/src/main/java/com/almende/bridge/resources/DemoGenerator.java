/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.joda.time.DateTime;

import com.almende.eve.agent.AgentConfig;
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
import com.almende.util.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class DemoGenerator.
 */
@Access(AccessType.PUBLIC)
public class DemoGenerator extends NodeAgent {
	private static final Logger			LOG					= Logger.getLogger(DemoGenerator.class
																	.getName());
	private Map<String, List<double[]>>	placesOfInterest	= new HashMap<String, List<double[]>>();
	private Map<String, ObjectNode>		properties			= new HashMap<String, ObjectNode>();
	private EventBus					events				= null;

	private Map<String, Task>			tasks				= new HashMap<String, Task>();
	private boolean						stopEvac			= false;

	@Override
	public void onReady() {
		doScenarioSwitch("reset");
		events = new EventBus(getScheduler(), caller, getGraph(), "SFN");
		addNode2SFN();
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

	/**
	 * Send task.
	 *
	 * @param planName
	 *            the plan name
	 * @param type
	 *            the type
	 * @param poiType
	 *            the poi type
	 * @param poiNumber
	 *            the poi number
	 * @param minutes
	 *            the minutes
	 * @param taskParams
	 *            the task params
	 */
	public void sendTask(@Name("plan") String planName,
			@Name("type") String type, @Name("poiType") String poiType,
			@Name("poiNumber") int poiNumber, @Name("inMinutes") int minutes,
			@Name("taskParams") ObjectNode taskParams) {
		final Params params = new Params();
		final ObjectNode config = JOM.createObjectNode();

		Feature poi = getPoI(poiType, poiNumber);
		Point point = (Point) poi.getGeometry();
		config.put("lat", point.getCoordinates().getLatitude());
		config.put("lon", point.getCoordinates().getLongitude());
		config.put("before", DateTime.now().plusMinutes(minutes).getMillis());
		config.put("planName", planName);
		config.put("resType", type);
		config.set("taskParams", taskParams);
		config.put("id", new UUID().toString());
		tasks.put(config.get("id").asText(), new Task(config));

		params.add("task", config);
		params.add("reportTo", getUrls().get(0));

		events.sendEvent(new JSONRequest("taskRequest", params));

		schedule("handleTask", config, 10000);
		LOG.warning("Added task:" + config);
	}

	/**
	 * Volunteer.
	 *
	 * @param sender
	 *            the sender
	 * @param taskConfig
	 *            the task config
	 * @param eta
	 *            the eta
	 */
	public void volunteer(@Sender URI sender,
			@Name("task") ObjectNode taskConfig, @Name("eta") DateTime eta) {
		final Task task = tasks.get(taskConfig.get("id").asText());
		if (task != null) {
			synchronized (task) {
				task.setCandidate(eta, sender);
			}
		}
	}

	/**
	 * Acknowledge.
	 *
	 * @param sender
	 *            the sender
	 * @param id
	 *            the id
	 * @param confirm
	 *            the confirm
	 */
	public void acknowledge(@Sender URI sender, @Name("id") String id,
			@Name("confirm") boolean confirm) {
		if (confirm) {
			tasks.remove(id);
		} else {
			final Task task = tasks.get(id);
			if (task != null) {
				synchronized (task) {
					URI next = task.getNext(sender);
					LOG.warning("Getting an alternative resource:" + next);
					if (next != null) {
						try {
							final Params params = new Params();
							params.add("plan", task.getConfig().get("planName")
									.asText());
							params.add("id", id);
							params.set("params",
									task.getConfig().get("taskParams"));
							call(next, "setPlan", params);
						} catch (IOException e) {
							LOG.log(Level.WARNING, "Couldn't send plan", e);
						}
					} else {
						LOG.log(Level.WARNING,
								"still no candidates, gotta retry!");
					}
				}
			} else {
				LOG.log(Level.WARNING, "Strange, missing task? " + id);
			}
		}
	}

	/**
	 * Handle task.
	 *
	 * @param id
	 *            the id
	 */
	public void handleTask(@Name("id") String id) {
		final Task task = tasks.get(id);
		if (task != null) {
			synchronized (task) {
				URI closest = task.getClosest();
				if ( closest != null) {
					try {
						final Params params = new Params();
						params.add("plan", task.getConfig().get("planName")
								.asText());
						params.add("id", id);
						params.set("params", task.getConfig().get("taskParams"));
						call(closest, "setPlan", params);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Couldn't send plan", e);
					}
				} else {
					LOG.log(Level.WARNING,
							"No candidates available, need to retry at application level, escalate!!");
				}
			}
		} else {
			LOG.log(Level.WARNING, "Strange, missing task? " + id);
		}
	}

	/**
	 * Store places of interest.
	 *
	 * @param type
	 *            the type
	 * @param list
	 *            the list
	 */
	private synchronized void storePlacesOfInterest(@Name("type") String type,
			@Name("list") double[][] list) {

		List<double[]> arrayList;
		if (placesOfInterest.containsKey(type)) {
			arrayList = placesOfInterest.get(type);
		} else {
			arrayList = new ArrayList<double[]>();
			placesOfInterest.put(type, arrayList);
		}
		arrayList.addAll(Arrays.asList(list));
	}

	/**
	 * Clear po i.
	 */
	private void clearPoI() {
		placesOfInterest.clear();
		properties.clear();
	}

	/**
	 * Store PointOfInterest properties.
	 *
	 * @param type
	 *            the type
	 * @param props
	 *            the props
	 */
	private synchronized void storePoIproperties(@Name("type") String type,
			@Name("properties") ObjectNode props) {
		if (properties.containsKey(type)) {
			ObjectNode node = properties.get(type);
			node.setAll(props);
		} else {
			properties.put(type, props);
		}
	}

	private void createPoIproperties(String key, String label, String icon) {
		final ObjectNode node = JOM.createObjectNode();
		node.put("label", label);
		node.put("icon", icon);
		storePoIproperties(key, node);
	}

	/**
	 * Gets the po i.
	 *
	 * @param type
	 *            the type
	 * @param i
	 *            the i
	 * @return the po i
	 */
	public Feature getPoI(@Name("type") String type, @Name("count") int i) {
		final Feature feature = new Feature();
		feature.setProperty("type", type);
		final ObjectNode node = properties.get(type + "-" + i);
		if (node != null) {
			feature.setId(node.get("label").asText());
			feature.setProperty("icon", node.get("icon").asText());
		}
		final Point point = new Point();
		final double[] loc = placesOfInterest.get(type).get(i);
		point.setCoordinates(new LngLatAlt(loc[0], loc[1]));
		feature.setGeometry(point);
		return feature;
	}

	/**
	 * Generate agents.
	 *
	 * @param type
	 *            the type
	 * @param at
	 *            At what type of location?
	 * @param nofAgents
	 *            the nof agents
	 * @param icon
	 *            the icon
	 * @param tag
	 *            the tag
	 */
	public void generateAgents(@Name("type") String type,
			@Name("at") String at, @Name("nofAgents") int nofAgents,
			@Name("icon") String icon, @Name("tag") String tag) {
		// Generate X agents, at random stations
		for (int i = 0; i < nofAgents; i++) {
			SimulatedResource agent = new SimulatedResource();
			AgentConfig agentConfig = new AgentConfig();
			agentConfig.setId(URIUtil.encode(type) + "-" + i + "-"
					+ DateTime.now().getMillis());
			agentConfig.setAll((ObjectNode) getConfig().get("simAgents"));
			List<double[]> stations = placesOfInterest.get(at);
			agentConfig
					.set("initLocation",
							JOM.getInstance()
									.valueToTree(
											stations.get((int) (Math.random() * stations
													.size()))));
			agentConfig.put("resType", type);
			agentConfig.put("guid", new UUID().toString());
			agentConfig.put("tag", tag);
			agentConfig.put("icon", icon);
			agent.setConfig(agentConfig);
		}
	}

	private boolean filter(boolean operational, String type, int i) {
		if (!operational)
			return true;

		Map<String, Integer> allowedTypes = new HashMap<String, Integer>();
		allowedTypes.put("fireStation", 2);
		allowedTypes.put("policeStation", 2);
		allowedTypes.put("hospital", 2);
		allowedTypes.put("rvpFire", 1);
		allowedTypes.put("rvpAmbu", 1);
		allowedTypes.put("incident", 1);

		if (allowedTypes.containsKey(type)) {
			return allowedTypes.get(type) > i;
		}
		return false;
	}

	/**
	 * Gets the points of interest.
	 *
	 * @param asaFilter
	 *            the asa filter
	 * @return the points of interest
	 */
	public FeatureCollection getPointsOfInterest(
			@Optional @Name("asaFilter") Boolean asaFilter) {
		boolean filter = false;
		if (asaFilter != null && asaFilter) {
			filter = true;
		}
		final FeatureCollection fc = new FeatureCollection();
		fc.setProperty("id", "PointsOfInterest");

		for (Entry<String, List<double[]>> entry : placesOfInterest.entrySet()) {
			String type = entry.getKey();
			List<double[]> list = entry.getValue();
			for (int i = 0; i < list.size(); i++) {
				if (filter(filter, type, i)) {
					fc.add(getPoI(type, i));
				}
			}
		}

		return fc;
	}

	/**
	 * Do scenario switch.
	 *
	 * @param step
	 *            the step
	 */
	public void doScenarioSwitch(@Name("step") String step) {

		LOG.warning("DoScenarioSwitch called:" + step);
		switch (step) {
			case "reset":
				stopEvac = true;
				clearPoI();
				resetAgents();

				double initHospital[][] = { { 1.452668, 43.559601 },
						{ 1.400976, 43.610032 }, { 1.431940, 43.600344 },
						{ 1.452860, 43.552724 } };
				storePlacesOfInterest("hospital", initHospital);
				createPoIproperties("hospital-0", "Rangueil",
						"hospital_building.png");
				createPoIproperties("hospital-1", "Purpan",
						"hospital_building.png");
				createPoIproperties("hospital-2", "La Grave",
						"hospital_building.png");
				createPoIproperties("hospital-3", "Larrey",
						"hospital_building.png");
				
				double initDamagedHospital[][] = { { 1.420396, 43.560099 } };
				storePlacesOfInterest("damagedHospital", initDamagedHospital);
				createPoIproperties("damagedHospital-0", "Marchant",
						"hospital_building.png");

				double policeStation[][] = { { 1.431868, 43.597184 },
						{ 1.412553, 43.579855 }, { 1.456670, 43.571616 },
						{ 1.400537, 43.565397 }, { 1.462464, 43.610998 },
						{ 1.483750, 43.578798 } };
				storePlacesOfInterest("policeStation", policeStation);
				createPoIproperties("policeStation-0", "policeStation-1",
						"police_building.png");
				createPoIproperties("policeStation-1", "policeStation-2",
						"police_building.png");
				createPoIproperties("policeStation-2", "policeStation-3",
						"police_building.png");
				createPoIproperties("policeStation-3", "policeStation-4",
						"police_building.png");
				createPoIproperties("policeStation-4", "policeStation-5",
						"police_building.png");
				createPoIproperties("policeStation-5", "policeStation-6",
						"police_building.png");

				double fireStation[][] = { { 1.455619, 43.595087 },
						{ 1.355983, 43.593078 }, { 1.473571, 43.554649 },
						{ 1.432859, 43.594798 }, { 1.464916, 43.600050 }, {1.410107, 43.533935 } };
				storePlacesOfInterest("fireStation", fireStation);
				createPoIproperties("fireStation-0", "fireStation-1",
						"firedpt_building.png");
				createPoIproperties("fireStation-1", "fireStation-2",
						"firedpt_building.png");
				createPoIproperties("fireStation-2", "fireStation-3",
						"firedpt_building.png");
				createPoIproperties("fireStation-3", "fireStation-4",
						"firedpt_building.png");
				createPoIproperties("fireStation-4", "fireStation-5",
						"firedpt_building.png");
				createPoIproperties("fireStation-5", "fireStation-6",
						"firedpt_building.png");
				
				// Setup resources (most are there already through the keep
				// eve.yaml)
				// Load point of interest with "original icons"
				break;
			case "incident":
				// Add incident poi, rvpFire0 near incident, roadblock0 at
				// freeway
				double incident[][] = { { 1.427344, 43.567000 } };
				storePlacesOfInterest("incident", incident);
				createPoIproperties("incident-0", "AZF fire/explosion",
						"incidents_hazards/fire.svg");

				double rb1[][] = { { 1.424354, 43.569847 } };
				storePlacesOfInterest("roadblock", rb1);
				createPoIproperties("roadblock-0", "rb-0",
						"cordons_zones_areas/scene_access_control_point_black_and_white.svg");

				double rvf1[][] = { { 1.424404, 43.567260 } };
				storePlacesOfInterest("rvpFire", rvf1);
				createPoIproperties("rvpFire-0", "rvpf-0",
						"assets/rendevous_point_fire.svg");

				// 2x Firetruck goto rvpFire0, police goto roadblock0
				Params params = new Params();
				params.add("poiType", "rvpFire");
				params.add("poiNumber", "0");
				sendTask("FireSuppression", "fire vehicle", "rvpFire", 0, 15,
						params);
				sendTask("FireSuppression", "fire vehicle", "rvpFire", 0, 15,
						params);

				Params params2 = new Params();
				params2.add("poiType", "roadblock");
				params2.add("poiNumber", "0");
				sendTask("RoadBlock", "police vehicle", "roadblock", 0, 15,
						params2);

				break;
			case "assessment":
				// add roadblock 1, rvpFire1

				double rb2[][] = { { 1.423846, 43.559564 } };
				storePlacesOfInterest("roadblock", rb2);
				createPoIproperties("roadblock-1", "rb-1",
						"incidents_hazards/road_block.png");

				double rvf2[][] = { { 1.426443, 43.563031 } };
				storePlacesOfInterest("rvpFire", rvf2);
				createPoIproperties("rvpFire-1", "rvpf-1",
						"assets/rendevous_point_fire.svg");

				// 2x Firetruck goto rvpFire1, 1x firetruck goto rvpFire0, 2x
				// police goto roadblock1 1x police goto roadblock0.
				Params params3 = new Params();
				params3.add("poiType", "rvpFire");
				params3.add("poiNumber", "1");
				sendTask("FireSuppression", "fire vehicle", "rvpFire", 1, 15,
						params3);
				sendTask("FireSuppression", "fire vehicle", "rvpFire", 1, 15,
						params3);

				Params params4 = new Params();
				params4.add("poiType", "rvpFire");
				params4.add("poiNumber", "0");
				sendTask("FireSuppression", "fire vehicle", "rvpFire", 0, 15,
						params4);

				Params params5 = new Params();
				params5.add("poiType", "roadblock");
				params5.add("poiNumber", "1");
				sendTask("RoadBlock", "police vehicle", "roadblock", 1, 15,
						params5);
				sendTask("RoadBlock", "police vehicle", "roadblock", 1, 15,
						params5);

				Params params6 = new Params();
				params6.add("poiType", "roadblock");
				params6.add("poiNumber", "0");
				sendTask("RoadBlock", "police vehicle", "roadblock", 0, 15,
						params6);

				break;
			case "scaleUp":
				double roadblock[][] = { { 1.428539, 43.570415 },
						{ 1.435995, 43.572156 }, { 1.422178, 43.563294 },
						{ 1.424631, 43.559758 }, { 1.431684, 43.557258 },
						{ 1.410907, 43.552105 }, { 1.420585, 43.549564 } };
				storePlacesOfInterest("roadblock", roadblock);
				createPoIproperties("roadblock-2", "rb-2",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-3", "rb-3",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-4", "rb-4",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-5", "rb-5",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-6", "rb-6",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-7", "rb-7",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-8", "rb-8",
						"incidents_hazards/road_block.png");
				createPoIproperties("roadblock-9", "rb-9",
						"incidents_hazards/road_block.png");

				double hospital[][] = { { 1.452668, 43.559601 },
						{ 1.400976, 43.610032 }, { 1.431940, 43.600344 },
						{ 1.452860, 43.552724 } };
				storePlacesOfInterest("hospital", hospital);
				createPoIproperties("hospital-0", "Rangueil",
						"infrastructures/hospital_green.svg");
				createPoIproperties("hospital-1", "Purpan",
						"infrastructures/hospital_green.svg");
				createPoIproperties("hospital-2", "La Grave",
						"infrastructures/hospital_green.svg");
				createPoIproperties("hospital-3", "Larrey",
						"infrastructures/hospital_green.svg");

				double damagedHospital[][] = { { 1.420396, 43.560099 } };
				storePlacesOfInterest("damagedHospital", damagedHospital);
				createPoIproperties("damagedHospital-0", "Marchant",
						"infrastructures/hospital_red.svg");

				double rpAmbulances[][] = { { 1.424318, 43.569079 },
						{ 1.424490, 43.559548 } };
				storePlacesOfInterest("rvpAmbu", rpAmbulances);
				createPoIproperties("rvpAmbu-0", "rvpa-0",
						"assets/rendevous_point_ambulance.svg");
				createPoIproperties("rvpAmbu-1", "rvpa-1",
						"assets/rendevous_point_ambulance.svg");

				double commandPost[][] = { { 1.423707, 43.556492 },
						{ 1.423411, 43.568942 } };
				storePlacesOfInterest("commandPost", commandPost);
				createPoIproperties(
						"commandPost-0",
						"cp-1",
						"command_control_coordination_communication_sites/incident_command_post_police_fire_and_ambulance.svg");
				createPoIproperties(
						"commandPost-1",
						"cp-2",
						"command_control_coordination_communication_sites/gold_command_post_police_fire_and_ambulance.svg");

				// Per roadblock, 2 policecars, start Ambulance bridge to
				// hospitals (~10), 10 fire trucks moving in to the two rvpFire.
				final int length = placesOfInterest.get("roadblock").size();
				for (int i = 0; i < length; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "roadblock");
					paramInner.add("poiNumber", i);
					sendTask("RoadBlock", "police vehicle", "roadblock", i,
							15, paramInner);
				}
				for (int i = 0; i < 6; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "rvpFire");
					paramInner.add("poiNumber", 0);
					sendTask("FireSuppression", "fire vehicle", "rvpFire", 0,
							15, paramInner);
				}
				for (int i = 0; i < 4; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "rvpFire");
					paramInner.add("poiNumber", 1);
					sendTask("FireSuppression", "fire vehicle", "rvpFire", 1,
							15, paramInner);
				}
				stopEvac = false;
				scheduleAmbulances();

				break;
			case "handling":
				final int length2 = placesOfInterest.get("roadblock").size();
				for (int i = 0; i < length2; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "roadblock");
					paramInner.add("poiNumber", i);
					sendTask("RoadBlock", "police vehicle", "roadblock", i,
							15, paramInner);
				}
				for (int i = 0; i < 4; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "rvpFire");
					paramInner.add("poiNumber", 0);
					sendTask("FireSuppression", "fire vehicle", "rvpFire", 0,
							15, paramInner);
				}
				for (int i = 0; i < 6; i++) {
					final Params paramInner = new Params();
					paramInner.add("poiType", "rvpFire");
					paramInner.add("poiNumber", 1);
					sendTask("FireSuppression", "fire vehicle", "rvpFire", 1,
							15, paramInner);
				}
				break;
			default:
				LOG.warning("Unknown step given:" + step);
		}

	}

	/**
	 * Load po i.
	 *
	 * @param data
	 *            the data
	 */
	public void loadPoI(@Name("data") String data) {
		int cntH = 0;
		int cntP = 0;
		int cntF = 0;

		for (String line : data.split(" ")) {
			String[] fields = line.split(",");
			String type = fields[3];
			double[][] loc = new double[1][2];
			loc[0][0] = Double.valueOf(fields[1]);
			loc[0][1] = Double.valueOf(fields[0]);

			storePlacesOfInterest(type, loc);
			switch (type) {
				case "rem_hospital":
					createPoIproperties(type + "-" + cntH, "",
							"hospital_building.png");
					cntH++;
					break;
				case "rem_policeStation":
					createPoIproperties(type + "-" + cntP, "",
							"police_building.png");
					cntP++;
					break;
				case "rem_fireStation":
					createPoIproperties(type + "-" + cntF, "",
							"firedpt_building.png");
					cntF++;
					break;
				default:
					LOG.warning("Unknown type: '" + type + "'");
			}
		}
	}

	/**
	 * Schedule ambulances.
	 */
	public void scheduleAmbulances() {
		// TODO: Send 1 ambulance every half minute to an rvpAmbu, transport to
		// randomHospital.
		if (!stopEvac) {
			doAmbulance();
			schedule("scheduleAmbulances", null, 30000);
		}
	}

	private void doAmbulance() {
		int rvp = (int) (Math.random() * 2);
		int hospital = (int) (Math.random() * 4);

		Params params = new Params();
		params.add("hospital", hospital);
		params.add("rvpAmbu", rvp);
		sendTask("Evac", "medic vehicle", "rvpAmbu", rvp, 15, params);
	}
	
	private static final TypeUtil<List<URI>>	URILIST		= new TypeUtil<List<URI>>() {};
	private void resetAgents() {
		List<URI> allResources;
		try {
			allResources = callSync(URI.create("local:proxy"),
					"getAllResources", new Params(), URILIST);
		} catch (IOException e) {
			allResources = new ArrayList<URI>(0);
			LOG.log(Level.WARNING, "Couldn't obtain resourceList", e);
		}
		
		for (URI agent: allResources){
			try {
				call(agent,"reset",new Params());
			} catch (IOException e) {
				LOG.log(Level.WARNING, "failed to send reset", e);
			}
		}
	}

}
