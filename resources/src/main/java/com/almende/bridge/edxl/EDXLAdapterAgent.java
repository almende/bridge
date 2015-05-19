/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.edxl;

import static com.almende.bridge.edxl.EDXLGenerator.setElementWithPath;
import static com.almende.bridge.edxl.EDXLParser.getElementsByType;
import static com.almende.bridge.edxl.EDXLParser.getStringByPath;
import static com.almende.bridge.edxl.EDXLParser.parseXML;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;

import com.almende.bridge.edxl.EDXLParser.EDXLRet;
import com.almende.bridge.oldDataStructs.Task;
import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class EDXLAdapterAgent.
 */
@Access(AccessType.PUBLIC)
public class EDXLAdapterAgent extends Agent {
	private static final Logger					LOG			= Logger.getLogger(EDXLAdapterAgent.class
																	.getName());
	static String								lastTaskId	= "";
	private static final TypeUtil<List<URI>>	URILIST		= new TypeUtil<List<URI>>() {};

	private static final Map<String, String>	taskNames	= new TreeMap<String, String>(
																	String.CASE_INSENSITIVE_ORDER);

	static {
		// Medic vehicles:
		taskNames.put("Triage victims", "GotoAndStay");
		taskNames.put("Trauma treatment", "GotoAndStay");
		taskNames.put("Treat victim", "GotoAndStay");
		taskNames.put("Transport victim to hospital", "Evac");
		taskNames.put("Transport victim to emergency clinic", "Evac");
		taskNames.put("Transport victim to assembly area", "Evac");
		taskNames.put("Transport victim to other location", "Evac");
		taskNames.put("transport hospital", "Evac");
		taskNames.put("transport emergency clinic", "Evac");
		taskNames.put("transport assembly area", "Evac");
		taskNames.put("transport other location", "Evac");
		taskNames.put("Take leader role at incident scene", "GotoAndStay");
		taskNames.put("Take leader role at assembly area", "GotoAndStay");
		taskNames.put("Take leader role at other location", "GotoAndStay");
		taskNames.put("takerole incident scene", "GotoAndStay");
		taskNames.put("takerole assembly area", "GotoAndStay");
		taskNames.put("takerole other location", "GotoAndStay");

		// Fire vehicles:
		taskNames.put("fightfire putoutfire", "GotoAndStay");
		taskNames.put("fightfire smokedive", "GotoAndStay");
		taskNames.put("Put out fire", "GotoAndStay");
		taskNames.put("Smoke dive", "GotoAndStay");
		taskNames.put("Fast release", "GotoAndStay");
		taskNames.put("Search building", "GotoAndStay");
		taskNames.put("search other location", "GotoAndStay");

		taskNames.put("Secure hazardous material", "GotoAndStay");
		taskNames.put("After quench", "GotoAndStay");
		taskNames.put("Assist police", "GotoAndStay");
		taskNames.put("Assist medical personnel", "GotoAndStay");
		taskNames.put("Diving task", "GotoAndStay");
		taskNames.put("Rescue task", "Evac");

		// Police vehicles:
		taskNames.put("evacuate", "Evac");
		taskNames.put("Evacuate area", "Evac");
		taskNames.put("setup evacuation point", "GotoAndStay");
		taskNames.put("goto observe report", "GotoAndStay");
		taskNames.put("goto guard object", "GotoAndStay");
		taskNames.put("setup road block", "GotoAndStay");
		taskNames.put("setup cordon", "GotoAndStay");
		taskNames.put("Observe and report", "GotoAndStay");
		taskNames.put("Guard object", "GotoAndStay");
		taskNames.put("Set up", "GotoAndStay");
		taskNames.put("Set up road block", "GotoAndStay");
		taskNames.put("Set up cordon", "GotoAndStay");

		taskNames.put("takerole field commander", "GotoAndStay");
		taskNames.put("takerole radio leader", "GotoAndStay");
		taskNames.put("takerole logger", "GotoAndStay");
		taskNames.put("takerole incident commander", "GotoAndStay");
		taskNames.put("takerole second commander", "GotoAndStay");
		taskNames.put("takerole team leader", "GotoAndStay");
		taskNames.put("Take role as incident commander", "GotoAndStay");
		taskNames.put("Take role as second commander", "GotoAndStay");
		taskNames.put("Take role as team leader", "GotoAndStay");

		taskNames.put("investigate incident cause", "GotoAndStay");
		taskNames.put("investigate risk", "GotoAndStay");
		taskNames.put("Investigate cause of incident", "GotoAndStay");
	}

	private void _notify(String data, JsonNode meta) throws Exception {
		LOG.info("Received notify:" + data + " : " + meta.toString());
		RequestResource(data);
	}

	/**
	 * Notify.
	 *
	 * @param items
	 *            the items
	 * @param data
	 *            the data
	 * @param meta
	 *            the meta
	 * @throws Exception
	 *             the exception
	 */
	public void notify(@Optional @Name("items") ArrayNode items,
			@Optional @Name("data") String data,
			@Optional @Name("meta") JsonNode meta) throws Exception {
		if (items != null) {
			for (JsonNode item : items) {
				_notify(item.get("data").textValue(), item.get("meta"));
			}
		}
		if (data != null) {
			_notify(data, meta);
		}
	}

	private List<URI> getResourceList() {
		final Params params = new Params("tag", "master");
		List<URI> allResources;
		try {
			allResources = callSync(URI.create("local:proxy"),
					"getAllResources", params, URILIST);
		} catch (IOException e) {
			allResources = new ArrayList<URI>(0);
			LOG.log(Level.WARNING, "Couldn't obtain resourceList", e);
		}
		return allResources;
	}

	/**
	 * Gets the resources.
	 *
	 * @return the resources
	 */
	public ArrayNode getResources() {
		final ArrayNode result = JOM.createArrayNode();
		final List<URI> allResources = getResourceList();
		try {
			Iterator<URI> iter = allResources.iterator();
			while (iter.hasNext()) {
				List<URI> subList = new ArrayList<URI>();
				subList.add(iter.next());
				String replyDoc = createReportResourceDeploymentStatus(subList);
				result.add(replyDoc);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING,
					"Ran into trouble creating and posting EDXL-RM for S2D2S.",
					e);
		}
		return result;
	}

	/**
	 * Send report resource deployment status.
	 *
	 * @param interval
	 *            the interval
	 * @param permessage
	 *            the permessage
	 */
	public void sendReportResourceDeploymentStatus(
			@Optional @Name("interval") Integer interval,
			@Optional @Name("permessage") Integer permessage) {

		final List<URI> allResources = getResourceList();
		try {
			Iterator<URI> iter = allResources.iterator();
			List<URI> subList = new ArrayList<URI>();
			int count = 1;
			while (iter.hasNext()) {
				subList.add(iter.next());
				if (permessage == null || subList.size() >= permessage) {
					String replyDoc = createReportResourceDeploymentStatus(subList);

					ObjectNode params = JOM.createObjectNode();
					params.put("topic", "App.Global.ResourceStatus");
					params.put("contentType", "application/xml");
					params.put("metadata", "");
					params.put("itemId", count++);
					params.put("persist", "true");
					params.put("payLoad", replyDoc);

					call(URI.create("http://bridge.d-cis.nl:8008/Name/S2D2S/jsonrpc"),
							"publish", params);

					subList = new ArrayList<URI>();
				}
			}
		} catch (Exception e) {
			System.err
					.println("Ran into trouble creating and posting EDXL-RM for S2D2S.");
			e.printStackTrace();
		}
		if (interval != null) {
			stop();
			Params params = new Params();
			params.add("interval", interval);
			params.add("permessage", permessage != null ? permessage : 1);
			lastTaskId = getScheduler().schedule(
					new JSONRequest("sendReportResourceDeploymentStatus",
							params), interval * 1000);
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		getScheduler().cancel(lastTaskId);
	}

	/**
	 * (inbound)<br>
	 * 3.4 RequestResource Message The “RequestResource” message is used as an
	 * announcement to a broad audience of potential suppliers as well as
	 * potential suppliers in the local geographic area of interest. It is
	 * intended to be used by Emergency Managers, Incident Commanders and other
	 * First Responders to request information on availability of needed
	 * resources.<br>
	 * <br>
	 * (outbound)<br>
	 * 3.5 ResponseToRequestResource Message The “ResponseToRequestResource”
	 * message is used by potential resource suppliers (e.g. mutual aid
	 * partners, equipment suppliers, etc.) to respond to RequestResource
	 * messages from Emergency Managers, Incident Commanders and First
	 * Responders or others with logistics responsibilities. The response may
	 * identify availability, limitations and other pertinent information
	 * related to resources in the original request.
	 *
	 * @param message
	 *            RequestResourceMessage (EDXL-RM 3.4)
	 * @return ResponseToRequestResourceMessage (EDXL-RM 3.5)
	 * @throws Exception
	 *             the exception
	 */
	public synchronized String RequestResource(
			@Name("RequestResourceMessage") String message) throws Exception {

		Task task = new Task();
		task.setStatus(Task.NOTCONFIRMED);

		ArrayNode resources = JOM.createArrayNode();

		EDXLRet inDoc = parseXML(message);
		if (inDoc == null)
			throw new Exception("Failed to parse XML message.");
		if (!"RequestResource".equalsIgnoreCase(inDoc.getMsgType()))
			throw new Exception("Incorrect XML message type!");

		String messageID = getStringByPath(inDoc.getRoot(), "MessageID");
		task.setMessageId(messageID);
		task.setIncidentDescription(getStringByPath(inDoc.getRoot(),
				new String[] { "IncidentInformation", "IncidentDescription" }));

		List<Element> resList = getElementsByType(inDoc.getRoot(),
				"ResourceInformation");
		Boolean first_resource = true;
		for (Element res : resList) {
			ObjectNode node = JOM.createObjectNode();
			node.put(
					"resourceType",
					getStringByPath(res, new String[] { "Resource",
							"TypeStructure", "Value" }));
			node.put(
					"resourceID",
					getStringByPath(res, new String[] { "Resource",
							"ResourceID" }));
			node.put(
					"amountString",
					getStringByPath(res, new String[] {
							"AssignmentInformation", "Quantity",
							"MeasuredQuantity", "Amount" }));
			resources.add(node);

			if (first_resource) {
				task.setTitle(getStringByPath(res, new String[] {
						"AssignmentInformation", "AnticipatedFunction" }));
				task.setText(getStringByPath(res, new String[] {
						"AssignmentInformation", "AssignmentInstructions" }));
				task.setAssignmentDate(getStringByPath(res, new String[] {
						"ScheduleInformation", "DateTime" }));
				// TODO: handle other notations for GeoData, depending on
				// namespace
				// definition? Currently only GML is supported.
				String location = getStringByPath(res, new String[] {
						"ScheduleInformation", "location", "TargetArea",
						"Point", "pos" });
				if (location != null) {
					String[] loc = location.split(" ");
					if (loc.length == 1){
						loc = location.split(",");
					}
					task.setLat(loc[0]);
					task.setLon(loc[1]);
				}
			}
			first_resource = false;
		}
		
		LOG.warning("Sending tasks:"+resources);
		
		// TODO: how to get Assigner from EDXL-RM?
		for (JsonNode resource : resources) {

			// If resourceID is given, get specific agent and setPlan
			// else, sendTask through demoGenerator to the agents.
			String planName = taskNames.get(task.getTitle());
			if (resource.get("resourceID") != null && !resource.get("resourceID").asText().isEmpty()) {
				String resID = resource.get("resourceID").asText();
				final Params params = new Params();
				params.add("plan", planName);

				final Params parms = new Params();
				parms.set("task", JOM.getInstance().valueToTree(task));
				parms.add("title", task.getTitle());
				params.set("params", parms);

				call(URIUtil.parse("local:" + resID), "setPlan", params);
			} else {
				for (int i = 0; i < resource.get("amountString").asInt(); i++) {
					final Params params = new Params();
					params.add("plan", planName);
					params.add("type", resource.get("resourceType").asText());
					params.add("inMinutes", 15);

					params.add("lat", task.getLat());
					params.add("lon", task.getLon());
					
					final Params parms = new Params();
					parms.set("task", JOM.getInstance().valueToTree(task));
					parms.add("title", task.getTitle());
					
					params.set("taskParams", parms);

					call(URIUtil.create("local:demo"), "sendTask", params);
				}
			}
		}
		// Prepare response message:
		Document replyDoc = EDXLGenerator.genDoc("ResponseToRequestResource");
		Element root = replyDoc.getRootElement();
		setElementWithPath(root, new String[] { "OriginatingMessageID" },
				messageID);
		setElementWithPath(root, new String[] { "PrecedingMessageID" },
				messageID);
		// // Report task agent ID in messageDescription:
		// setElementWithPath(root, new String[] { "MessageDescription" },
		// agent.getId());
		int count = 1;
		for (Element res : resList) {
			Element sub = new Element("ResourceInformation");
			setElementWithPath(sub, new String[] { "ResourceInfoElementID" },
					new Integer(count++).toString());
			setElementWithPath(
					sub,
					new String[] { "ReponseInformation",
							"PrecedingResourceInfoElementID" },
					getStringByPath(res,
							new String[] { "ResourceInfoElementID" }));
			setElementWithPath(sub, new String[] { "ReponseInformation",
					"ResponseType" }, "Provisional");
			String resourceID = getStringByPath(res, new String[] { "Resource",
					"ResourceID" });
			if (!resourceID.equals("")) {
				setElementWithPath(sub,
						new String[] { "Resource", "ResourceID" }, resourceID);
			}

			root.addContent(sub);
		}
		return EDXLGenerator.printDoc(replyDoc);
	}

	/**
	 * Creates the report resource deployment status.
	 *
	 * @param members
	 *            the members
	 * @return the string
	 */
	public String createReportResourceDeploymentStatus(
			@Name("members") List<URI> members) {
		Document replyDoc = EDXLGenerator
				.genDoc("ReportResourceDeploymentStatus");
		Element root = replyDoc.getRootElement();
		boolean sendTasks = true;

		if (members != null) {
			int count = 1;
			for (URI res : members) {
				try {
					ObjectNode status = callSync(res, "requestStatus", null,
							ObjectNode.class);
					if (status == null) {
						throw new Exception("Status null!"
								+ res.toASCIIString());
					}
					Element sub = new Element("ResourceInformation");
					setElementWithPath(sub,
							new String[] { "ResourceInfoElementID" },
							new Integer(count++).toString());
					setElementWithPath(sub, new String[] { "Resource",
							"ResourceID" }, status.get("id").textValue());
					setElementWithPath(sub,
							new String[] { "Resource", "Name" },
							status.get("name").textValue());
					setElementWithPath(sub, new String[] { "Resource",
							"TypeStructure", "rm:Value" }, status.get("type")
							.textValue());
					setElementWithPath(sub, new String[] { "Resource",
							"TypeStructure", "rm:ValueListURN" },
							"urn:x-hazard:vocab:resourceTypes");
					if (status.has("deploymentStatus")) {
						setElementWithPath(sub, new String[] { "Resource",
								"ResourceStatus", "DeploymentStatus",
								"rm:Value" }, status.get("deploymentStatus")
								.asText());
						setElementWithPath(sub, new String[] { "Resource",
								"ResourceStatus", "DeploymentStatus",
								"rm:ValueListURN" },
								"urn:x-hazard:vocab:deploymentStatusTypes");
					}
					if (status.has("current")) {
						Element schedule = new Element("ScheduleInformation");
						ObjectNode loc = (ObjectNode) status.get("current");
						setElementWithPath(schedule,
								new String[] { "ScheduleType" }, "Current");
						setElementWithPath(schedule, new String[] { "Location",
								"rm:TargetArea", "gml:Point", "gml:pos" },
								loc.get("latitude").textValue() + " "
										+ loc.get("longitude").textValue());
						if (loc.has("time")) {
							String time = loc.get("time").textValue();
							if (time != null && !time.isEmpty()) {
								setElementWithPath(schedule,
										new String[] { "DateTime" },
										loc.get("time").textValue());
							}
						}
						sub.addContent(schedule);
					}
					if (sendTasks && status.has("goal")) {
						Element schedule = new Element("ScheduleInformation");
						ObjectNode loc = (ObjectNode) status.get("goal");
						setElementWithPath(schedule,
								new String[] { "ScheduleType" },
								"RequestedArrival");
						setElementWithPath(schedule, new String[] { "Location",
								"rm:TargetArea", "gml:Point", "gml:pos" },
								loc.get("latitude").textValue() + " "
										+ loc.get("longitude").textValue());
						if (loc.has("time")) {
							String time = loc.get("time").textValue();

							if (time != null && !time.isEmpty()) {
								setElementWithPath(schedule,
										new String[] { "DateTime" },
										loc.get("time").textValue());
							}
						}
						sub.addContent(schedule);
					}
					if (sendTasks && status.has("task")) {
						setElementWithPath(sub,
								new String[] { "AssignmentInformation",
										"AnticipatedFunction" },
								status.get("task").textValue());
					}
					root.addContent(sub);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return EDXLGenerator.printDoc(replyDoc);
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return "EDXL-RM adapter for communication with MasterTable through S2D2S.";
	}

	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return "1.0";
	}

}
