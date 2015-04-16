/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import com.almende.eve.agent.Agent;

/**
 * The Class GroupGoal.
 */
public class GroupGoal extends Agent {
	//type:
		//requirements: tags (plus required numbers)
		//geolocation
		//priority
		//deadlines & durations
		//creator address
		//dependencies on other goals?
	
	//current:
		//subscribers?
		//progress
	
	//logic:
		//Publish this goal to all potential resources
		//receive escalation:
			//republish with relaxed requirements, different deadlines and/or higher priority
			//escalate to creator
			
		//When done, inform dependent goals
}
