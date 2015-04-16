/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import com.almende.eve.agent.Agent;

/**
 * The Class GenResource.
 */
public class GenResource extends Agent {

	//Type:
		//capabilities (tags)
		//own movement type/speed
		//weight/volume/transportability
		//transport capacity
	
	//State:
		//current location
		//current energy/fuel/consumables
		//current inventory (passengers/stuff in transport)
		//Current plan(s):
			//Selector on plans
	
	//Plans:
		//Goal owner (tag or agentId/address) (=adhoc team)
		//Goal location
		//Deadlines/durations
		//priority
		//requirements (tags and agentId/addresses)
		//cost estimate
	
	//Keep track of plans of other resources? e.g. within local teams?
	
	//Logic:
		//Check for new plans/goals
		//Reassess current plan(s) (which logic? Scoring?)
		//commit to plan
		//Report on progress on plans
		//escalate for plans that can't be achieved
		//drop/schedule lower: unrealistic plans
		
}
