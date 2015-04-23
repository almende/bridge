/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ResourceProxy.
 */
@Access(AccessType.PUBLIC)
public class ResourceProxy extends Agent {
	List<URI>	neighbors	= new ArrayList<URI>();

	/**
	 * Register.
	 *
	 * @param sender
	 *            the sender
	 */
	public void register(@Sender URI sender) {
		synchronized (neighbors) {
			neighbors.add(sender);
		}
	}

	
	/**
	 * Gets the all geo json.
	 *
	 * @param incTrack
	 *             Should the track data be included? Defaults to no.
	 * @return the all geo json
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public ArrayNode getAllGeoJson(@Optional @Name("includeTrack") Boolean incTrack) throws IOException {
		final Params params = new Params();
		params.add("includeTrack", incTrack != null && incTrack?true:false);
		
		final ArrayNode result = JOM.createArrayNode();
		synchronized(neighbors){
			for (URI uri: neighbors){
				call(uri,"getGeoJson",params,new AsyncCallback<ObjectNode>(){

					@Override
					public void onSuccess(ObjectNode res) {
						synchronized(result){
							result.add(res);
						}
					}

					/* (non-Javadoc)
					 * @see com.almende.util.callback.AsyncCallback#onFailure(java.lang.Exception)
					 */
					@Override
					public void onFailure(Exception exception) {
						synchronized(result){
							result.add(JOM.createObjectNode());
						}
					}
					
				});
			}
		}
		int size=Integer.MAX_VALUE;
		synchronized(result){
			size=result.size();
		}
		while (size<neighbors.size()){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			synchronized(result){
				size=result.size();
			}
		}
		return result;
	}
	
	/**
	 * Gets the all locations.
	 *
	 * @return the all locations
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public ArrayNode getAllLocations() throws IOException {
		final ArrayNode result = JOM.createArrayNode();
		synchronized(neighbors){
			for (URI uri: neighbors){
				call(uri,"getCurrentLocation",null,new AsyncCallback<ObjectNode>(){

					@Override
					public void onSuccess(ObjectNode res) {
						synchronized(result){
							result.add(res);
						}
					}

					/* (non-Javadoc)
					 * @see com.almende.util.callback.AsyncCallback#onFailure(java.lang.Exception)
					 */
					@Override
					public void onFailure(Exception exception) {
						synchronized(result){
							result.add(JOM.createObjectNode());
						}
					}
					
				});
			}
		}
		int size=Integer.MAX_VALUE;
		synchronized(result){
			size=result.size();
		}
		while (size<neighbors.size()){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			synchronized(result){
				size=result.size();
			}
		}
		return result;
	}
}
