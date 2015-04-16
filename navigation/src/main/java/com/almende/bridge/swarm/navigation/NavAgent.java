/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */

package com.almende.bridge.swarm.navigation;

import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.shapes.GHPoint;

/**
 * The Class NavAgent.
 */
public class NavAgent extends Agent {
	
	/**
	 * Instantiates a new nav agent.
	 *
	 * @param config
	 *            the config
	 */
	public NavAgent(ObjectNode config) {
		super(config);
	}

	/**
	 * Instantiates a new nav agent.
	 */
	public NavAgent() {
		super();
	}
	
	/**
	 * Gets the route.
	 *
	 * @param startLat
	 *            the start lat
	 * @param startLon
	 *            the start lon
	 * @param endLat
	 *            the end lat
	 * @param endLon
	 *            the end lon
	 * @return the route
	 */
	@Access(AccessType.PUBLIC)
	public ObjectNode getRoute(@Name("startLat") Double startLat, @Name("startLon") Double startLon,
			@Name("endLat") Double endLat, @Name("endLon") Double endLon) {
		ObjectNode result = JOM.createObjectNode();

		GHRequest req = new GHRequest(new GHPoint(startLat, startLon), new GHPoint(
				endLat, endLon));

		GHResponse res = Main.getHopper().route(req);
		InstructionList list = res.getInstructions();
		final List<GPXEntry> gpx = list.createGPXList();
		final ArrayNode parts = JOM.createArrayNode();
		for (GPXEntry entry : gpx){
			final ArrayNode item = JOM.createArrayNode();
			item.add(entry.getLon());
			item.add(entry.getLat());
			item.add(Double.isNaN(entry.getEle())?0:entry.getEle());
			item.add(entry.getMillis());
			parts.add(item);
		}
		result.set("route",parts);
		result.put("distance", res.getDistance());
		result.put("millis", res.getMillis());

		return result;
	}

}
