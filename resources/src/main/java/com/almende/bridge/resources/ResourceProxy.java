/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.geojson.FeatureCollection;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.URIUtil;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ResourceProxy.
 */
@Path("/")
@Access(AccessType.PUBLIC)
public class ResourceProxy extends Agent {
	static ResourceProxy			SINGLETON	= null;

	static Map<String, List<URI>>	agents		= new HashMap<String, List<URI>>();

	/**
	 * Gets the all resources.
	 *
	 * @param tag
	 *            the tag
	 * @return the all resources
	 */
	public List<URI> getAllResources(@Name("tag") String tag) {

		if (agents.containsKey(tag)) {
			return agents.get(tag);
		}
		return new ArrayList<URI>(0);
	}

	/**
	 * Instantiates a new resource proxy.
	 */
	public ResourceProxy() {
		if (SINGLETON == null) {
			SINGLETON = this;
		} else {
			setConfig(SINGLETON.getConfig());
		}
	}

	/**
	 * Register.
	 *
	 * @param sender
	 *            the sender
	 * @param tag
	 *            the tag
	 */
	public void register(@Sender URI sender, @Name("tag") String tag) {
		synchronized (agents) {
			List<URI> list = agents.get(tag);
			if (list == null) {
				list = new ArrayList<URI>();
				agents.put(tag, list);
			}
			list.add(sender);
		}
	}

	/**
	 * Gets the all geo json.
	 *
	 * @param filter
	 *            the filter
	 * @param track
	 *            the track
	 * @param tag
	 *            the tag
	 * @return the all geo json
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Path("geojson")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJson(@QueryParam("filter") String filter,
			@QueryParam("includeTrack") String track,
			@QueryParam("tag") String tag) throws IOException {
		final Params params = new Params();
		params.add("includeTrack", track != null ? Boolean.valueOf(track)
				: false);

		if (filter != null ? Boolean.valueOf(filter) : false) {
			return Response.ok(getAllGeoJson(params, "asa")).build();
		} else {
			return Response.ok(getAllGeoJson(params, tag)).build();
		}
	}

	/**
	 * Gets the points of interest.
	 *
	 * @param filter
	 *            the filter
	 * @return the points of interest
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Path("poi")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoI(@QueryParam("filter") String filter)
			throws IOException {
		final Params params = new Params();
		params.add("asaFilter", filter != null ? Boolean.valueOf(filter)
				: false);
		final FeatureCollection fc = callSync(URIUtil.create("local:demo"),
				"getPointsOfInterest", params, FeatureCollection.class);
		return Response.ok(fc).build();
	}

	private List<URI> getNeighbors(String tag) {
		final List<URI> neighbors = new ArrayList<URI>();
		synchronized (agents) {
			if (tag == null) {
				for (List<URI> list : agents.values()) {
					neighbors.addAll(list);
				}
			} else if (agents.containsKey(tag)) {
				neighbors.addAll(agents.get(tag));
			}
		}
		return neighbors;
	}

	/**
	 * Gets the all geo json.
	 *
	 * @param params
	 *            the params
	 * @param tag
	 *            the tag
	 * @return the all geo json
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public FeatureCollection getAllGeoJson(
			@Optional @Name("params") ObjectNode params,
			@Optional @Name("tag") String tag) throws IOException {
		final ArrayList<FeatureCollection> result = new ArrayList<FeatureCollection>();
		final List<URI> neighbors = getNeighbors(tag);
		for (URI uri : neighbors) {
			call(uri, "getGeoJson", params,
					new AsyncCallback<FeatureCollection>() {

						@Override
						public void onSuccess(FeatureCollection res) {
							synchronized (result) {
								result.add(res);
							}
						}

						/*
						 * (non-Javadoc)
						 * @see
						 * com.almende.util.callback.AsyncCallback#onFailure
						 * (java.lang.Exception)
						 */
						@Override
						public void onFailure(Exception exception) {
							synchronized (result) {
								result.add(new FeatureCollection());
							}
						}
					});
		}
		int size = Integer.MAX_VALUE;
		synchronized (result) {
			size = result.size();
		}
		while (size < neighbors.size()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			synchronized (result) {
				size = result.size();
			}
		}

		final FeatureCollection fc = new FeatureCollection();
		for (FeatureCollection collection : result) {
			fc.addAll(collection.getFeatures());
		}
		return fc;
	}

	/**
	 * Gets the all locations.
	 *
	 * @param tag
	 *            the tag
	 * @return the all locations
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public ArrayNode getAllLocations(@Optional @Name("tag") String tag)
			throws IOException {
		final ArrayNode result = JOM.createArrayNode();
		final List<URI> neighbors = getNeighbors(tag);

		for (URI uri : neighbors) {
			call(uri, "getCurrentLocation", null,
					new AsyncCallback<ObjectNode>() {

						@Override
						public void onSuccess(ObjectNode res) {
							synchronized (result) {
								result.add(res);
							}
						}

						@Override
						public void onFailure(Exception exception) {
							synchronized (result) {
								result.add(JOM.createObjectNode());
							}
						}

					});
		}
		int size = Integer.MAX_VALUE;
		synchronized (result) {
			size = result.size();
		}
		while (size < neighbors.size()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			synchronized (result) {
				size = result.size();
			}
		}
		return result;
	}
}
