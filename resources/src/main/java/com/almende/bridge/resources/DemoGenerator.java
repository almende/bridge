/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.util.ArrayList;
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

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class DemoGenerator.
 */
@Access(AccessType.PUBLIC)
public class DemoGenerator extends Agent {
	private static final Logger				LOG					= Logger.getLogger(DemoGenerator.class
																		.getName());
	Map<String, List<SimulatedResource>>	agents				= new HashMap<String, List<SimulatedResource>>();
	Map<String, List<double[]>>				placesOfInterest	= new HashMap<String, List<double[]>>();
	Map<String, ObjectNode>					properties			= new HashMap<String, ObjectNode>();

	/**
	 * Store places of interest.
	 *
	 * @param type
	 *            the type
	 * @param list
	 *            the list
	 */
	public void storePlacesOfInterest(@Name("type") String type,
			@Name("list") double[][] list) {
		List<double[]> arrayList = new ArrayList<double[]>();
		for (double[] pos : list) {
			arrayList.add(pos);
		}
		placesOfInterest.put(type, arrayList);
	}

	/**
	 * Store PointOfInterest properties.
	 *
	 * @param type
	 *            the type
	 * @param props
	 *            the props
	 */
	public void storePoIproperties(@Name("type") String type,
			@Name("properties") ObjectNode props) {
		properties.put(type, props);
	}

	private void createPoIproperties(String key, String label, String icon) {
		final ObjectNode node = JOM.createObjectNode();
		node.put("label", label);
		node.put("icon", icon);
		storePoIproperties(key, node);
	}

	/**
	 * Creates the places of interest.
	 */
	public void createPlacesOfInterest() {
		double incident[][] = { { 1.427344, 43.567000 } };
		storePlacesOfInterest("incident", incident);
		createPoIproperties("incident-0", "AZF fire/explosion",
				"incidents_hazards/fire.svg");

		double roadblock[][] = { { 1.428539, 43.570415 },
				{ 1.424354, 43.569847 }, { 1.435995, 43.572156 },
				{ 1.422178, 43.563294 }, { 1.424631, 43.559758 },
				{ 1.431684, 43.557258 }, { 1.410907, 43.554105 },
				{ 1.420585, 43.549564 }, { 1.423846, 43.559564 } };
		storePlacesOfInterest("roadblock", roadblock);
		createPoIproperties("roadblock-0", "rb-1",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-1", "rb-2",
				"cordons_zones_areas/scene_access_control_point_black_and_white.svg");
		createPoIproperties("roadblock-2", "rb-3",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-3", "rb-4",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-4", "rb-5",
				"cordons_zones_areas/scene_access_control_point_black_and_white.svg");
		createPoIproperties("roadblock-5", "rb-6",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-6", "rb-7",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-7", "rb-8",
				"incidents_hazards/road_block.png");
		createPoIproperties("roadblock-8", "rb-9",
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

		double rpAmbulances[][] = { { 1.424318, 43.569079 },
				{ 1.424490, 43.559548 } };
		storePlacesOfInterest("rvpAmbu", rpAmbulances);
		createPoIproperties("rvpAmbu-0", "rvpa-0",
				"assets/rendevous_point_ambulance.svg");
		createPoIproperties("rvpAmbu-1", "rvpa-1",
				"assets/rendevous_point_ambulance.svg");

		double rpFirebrigade[][] = { { 1.424404, 43.567260 },
				{ 1.426443, 43.563031 }, };
		storePlacesOfInterest("rvpFire", rpFirebrigade);
		createPoIproperties("rvpFire-0", "rvpf-0",
				"assets/rendevous_point_fire.svg");
		createPoIproperties("rvpFire-1", "rvpf-1",
				"assets/rendevous_point_fire.svg");

		double policeStation[][] = { { 1.431868, 43.597184 },
				{ 1.412553, 43.579855 }, { 1.456670, 43.571616 },
				{ 1.400537, 43.565397 }, { 1.462464, 43.610998 },
				{ 1.483750, 43.578798 } };
		storePlacesOfInterest("policeStation", policeStation);
		createPoIproperties("policeStation-0", "policeStation-1", null);
		createPoIproperties("policeStation-1", "policeStation-2", null);
		createPoIproperties("policeStation-2", "policeStation-3", null);
		createPoIproperties("policeStation-3", "policeStation-4", null);
		createPoIproperties("policeStation-4", "policeStation-5", null);
		createPoIproperties("policeStation-5", "policeStation-6", null);

		double fireStation[][] = { { 1.455619, 43.595087 },
				{ 1.355983, 43.593078 }, { 1.473571, 43.554649 },
				{ 1.432859, 43.594798 }, { 1.464916, 43.600050 } };
		storePlacesOfInterest("fireStation", fireStation);
		createPoIproperties("fireStation-0", "fireStation-1", null);
		createPoIproperties("fireStation-1", "fireStation-2", null);
		createPoIproperties("fireStation-2", "fireStation-3", null);
		createPoIproperties("fireStation-3", "fireStation-4", null);
		createPoIproperties("fireStation-4", "fireStation-5", null);

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

		double damagedHospital[][] = { { 1.420396, 43.560099 } };
		storePlacesOfInterest("damagedHospital", damagedHospital);
		createPoIproperties("damagedHospital-0", "Marchant",
				"infrastructures/hospital_red.svg");

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
	 */
	public void generateAgents(@Name("type") String type,
			@Name("at") String at, @Name("nofAgents") int nofAgents) {
		// Generate X agents, at random stations
		final List<SimulatedResource> agentList = new ArrayList<SimulatedResource>(
				nofAgents);
		for (int i = 0; i < nofAgents; i++) {
			SimulatedResource agent = new SimulatedResource();
			AgentConfig agentConfig = new AgentConfig();
			agentConfig.setAll((ObjectNode) getConfig().get("simAgents"));
			agent.setConfig(agentConfig);
			List<double[]> stations = placesOfInterest.get(at);
			agent.setGeoJsonLocation(stations.get((int) (Math.random() * stations
					.size())));
			agent.setResType(type);
			agentList.add(agent);
		}
		agents.put(type, agentList);
	}

	/**
	 * Generate goals.
	 *
	 * @param at
	 *            To what location type should this resource go?
	 * @param type
	 *            the type of resources
	 * @param description
	 *            the description
	 */
	public void generateGoals(@Name("at") String at, @Name("type") String type,
			@Name("description") String description) {

		// TODO: this should be the other way around: resources subscribing to
		// goals.
		if (!placesOfInterest.containsKey(at)){
			throw new IllegalArgumentException("Unknown location given:"+at);
		}

		final List<SimulatedResource> agentList = agents.get(type);
		final List<double[]> stations = new ArrayList<double[]>();
		for (SimulatedResource agent : agentList) {
			if (stations.size() == 0) {
				stations.addAll(placesOfInterest.get(at));
			}
			final ObjectNode goal = JOM.createObjectNode();
			double[] loc = stations.remove((int) (Math.random() * stations
					.size()));
			goal.put("taskTitle", description);
			goal.put("lat", loc[1]);
			goal.put("lon", loc[0]);
			try {
				agent.setGoal(goal);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Couldn't set goal", e);
			}
		}
	}

	/**
	 * Gets the points of interest.
	 *
	 * @return the points of interest
	 */
	public FeatureCollection getPointsOfInterest() {
		final FeatureCollection fc = new FeatureCollection();
		fc.setProperty("id", "PointsOfInterest");

		for (Entry<String, List<double[]>> entry : placesOfInterest.entrySet()) {
			String type = entry.getKey();
			List<double[]> list = entry.getValue();
			for (int i = 0; i < list.size(); i++) {
				final Feature feature = new Feature();
				feature.setProperty("type", type);
				ObjectNode node = properties.get(type + "-" + i);
				if (node != null) {
					feature.setId(node.get("label").asText());
					feature.setProperty("icon", node.get("icon").asText());
				}
				Point point = new Point();
				point.setCoordinates(new LngLatAlt(list.get(i)[0],
						list.get(i)[1]));
				feature.setGeometry(point);
				fc.add(feature);
			}
		}

		return fc;
	}
}
