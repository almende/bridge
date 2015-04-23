/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger LOG = Logger.getLogger(DemoGenerator.class
			.getName());
	List<double[]> stations = new ArrayList<double[]>();
	List<SimulatedResource> agents = new ArrayList<SimulatedResource>();
		
	/**
	 * Adds the station.
	 *
	 * @param pos
	 *            the pos
	 */
	public void addStation(@Name("pos") double[] pos){
		stations.add(pos);
	}
	
	/**
	 * Generate agents.
	 *
	 * @param nofAgents
	 *            the nof agents
	 */
	public void generateAgents(@Name("nofAgents") int nofAgents){
		//Generate X agents, at random stations
		for (int i=0; i<nofAgents; i++){
			SimulatedResource agent = new SimulatedResource();
			AgentConfig agentConfig = new AgentConfig();
			agentConfig.setAll((ObjectNode)getConfig().get("simAgents"));
			agent.setConfig(agentConfig);
			agent.setGeoJsonLocation(stations.get((int) (Math.random()*stations.size())));
			agents.add(agent);
		}
	}
	
	//Feed agents goals, e.g. other stations, see them migrate between them.
	/**
	 * Generate goals.
	 */
	public void generateGoals(){
		for (SimulatedResource agent: agents){
			final ObjectNode goal = JOM.createObjectNode();
			double[] loc = stations.get((int) (Math.random()*stations.size()));
			goal.put("lat", loc[1]);
			goal.put("lon", loc[0]);
			try {
				agent.setGoal(goal);
			} catch (IOException e) {
				LOG.log(Level.WARNING,"Couldn't set goal",e);
			}
		}
	}
}
