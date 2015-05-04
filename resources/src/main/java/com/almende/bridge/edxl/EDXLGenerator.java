/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.edxl;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * The Class EDXLGenerator.
 */
public class EDXLGenerator {
	static  SAXBuilder builder = new SAXBuilder();
	final static String pattern = "yyyy-MM-dd'T'hh:mm:ssZ";
	final static SimpleDateFormat sdf = new SimpleDateFormat(pattern);
	
	final static HashMap<String,Namespace> namespaces = new HashMap<String,Namespace>();
	final static HashMap<String,String> statuses = new HashMap<String,String>();
	final static boolean init = EDXLGeneratorInit();
	
	
	private static boolean EDXLGeneratorInit(){
		namespaces.put("xsi", Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
		namespaces.put("rm", Namespace.getNamespace("rm", "urn:oasis:names:tc:emergency:EDXL:RM:1.0"));
		namespaces.put("xpil", Namespace.getNamespace("xpil","urn:oasis:names:tc:ciq:xpil:3"));
		namespaces.put("xnl", Namespace.getNamespace("xnl","urn:oasis:names:tc:ciq:xnl:3"));
		namespaces.put("xal",Namespace.getNamespace("xal","urn:oasis:names:tc:ciq:xal:3"));
		namespaces.put("geo-oasis", Namespace.getNamespace("geo-oasis","urn:oasis:names:tc:emergency:EDXL:HAVE:1.0:geo-oasis"));
		namespaces.put("gml", Namespace.getNamespace("gml","http://www.opengis.net/gml"));
		
		statuses.put("offered", "Pending");
		statuses.put("accepted", "Accept");
		statuses.put("rejected", "Reject");
		return true;
	}

	/**
	 * Sets the element with path.
	 *
	 * @param from
	 *            the from
	 * @param path
	 *            the path
	 * @param value
	 *            the value
	 * @return the element
	 */
	public static Element setElementWithPath(Element from, String[] path, String value){
		try {
			Element elem = from;
			Element parent = elem;
			for (String tag : path){
				String[] parts = tag.split(":");
				Namespace ns=null;
				if (parts.length>1){
					tag = parts[1];
					ns = namespaces.get(parts[0]);
				}
				elem = (Element) elem.getChild(tag,ns);
				if (elem != null){
					parent=elem;
				} else {
					elem = new Element(tag,ns);
					parent.addContent(elem);
					parent=elem;
				}
			}
			if (value != null) elem.setText(value);
			return elem;
		} catch (Exception e){}
		return null;
	}
	
	
	private static Document newDocument(String type){
		Document document = null;
		try {
		    document = new Document();
		    Element root = new Element(type);
		    document.setRootElement(root);
		    root.setNamespace(Namespace.getNamespace("urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg"));
		    root.setAttribute(
		    	    new Attribute("schemaLocation",
		    	    		"urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg EDXL-RM"+type+".xsd",
		    	    	namespaces.get("xsi")));
		    for (Entry<String,Namespace> ns : namespaces.entrySet()){
		    	root.addNamespaceDeclaration(ns.getValue());
		    }
		    setElementWithPath(root, new String[]{"MessageContentType"},type);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return document;
	}
	
	/**
	 * Prints the doc.
	 *
	 * @param doc
	 *            the doc
	 * @return the string
	 */
	public static String printDoc(Document doc){
		XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(doc);
	}
	
	/**
	 * Gen doc.
	 *
	 * @param type
	 *            the type
	 * @return the document
	 */
	public static Document genDoc(String type){
		Document doc = newDocument(type);
		Element root = doc.getRootElement();
		
		setElementWithPath(root, new String[]{"MessageID"}, UUID.randomUUID().toString());
		setElementWithPath(root, new String[]{"SentDateTime"}, sdf.format(new Date(System.currentTimeMillis())));
		setElementWithPath(root, new String[]{"OriginatingMessageID"},"001");
		setElementWithPath(root, new String[]{"ContactInformation","ContactRole"},"Sender");
		
		return doc;
	}
	/*
	public String getDoc(){
		Document doc = newDocument("ReportResourceDeploymentStatus");
		Element root = doc.getRootElement();
		setElementWithPath(root, new String[]{"MessageID"}, "001");
		setElementWithPath(root, new String[]{"SendDateTime"}, sdf.format(new Date(System.currentTimeMillis())));
		setElementWithPath(root, new String[]{"OriginatingMessageID"},"001");
		setElementWithPath(root, new String[]{"ContactInformation","ContactRole"},"Sender");
		
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("Memo-appservices demo");
		MemoNode tasks = baseNode.getChildByStringValue("tasks");
		int count=0;
		for (MemoNode task : tasks.getChildren()){
			System.out.println("task:"+task.getId()+":"+task.getPropertyValue("description"));
			MemoNode resources = task.getChildByStringValue("resources");
			for (MemoNode res : resources.getChildByStringValue("car").getChildren()){
				System.out.println("Car:"+res.getId()+":"+res.getPropertyValue("resType"));
				
				Element elem = new Element("ResourceInformation");
				setElementWithPath(elem, new String[]{"ResourceInfoElementID"},new Integer(count).toString());
				Element respInfo = setElementWithPath(elem, new String[]{"ResponseInformation"},null);
				setElementWithPath(respInfo,new String[]{"rm:PrecedingResourceInfoElementID"},new Integer(count).toString());
				setElementWithPath(respInfo, new String[]{"rm:ResponseType"},"Accept");

				Element resource = setElementWithPath(elem, new String[]{"Resource"},null);
				
				Element typeStructure = setElementWithPath(resource, new String[]{"TypeStructure"},null);
				setElementWithPath(typeStructure,new String[]{"rm:ValueListURN"},"urn:x-hazard:vocab:resourceTypes");
				setElementWithPath(typeStructure, new String[]{"rm:Value"},res.getPropertyValue("resType"));
				
				setElementWithPath(resource,new String[]{"ResourceID"},res.getId().toString());
				setElementWithPath(resource,new String[]{"Name"},res.getPropertyValue("name"));
				
				root.addContent(elem);
				count++;
			}
			for (MemoNode status : resources.getChildByStringValue("human").getChildren()){
				for (MemoNode human: status.getChildren()){
					Element elem = new Element("ResourceInformation");
					setElementWithPath(elem, new String[]{"ResourceInfoElementID"},new Integer(count).toString());
					Element respInfo = setElementWithPath(elem, new String[]{"ResponseInformation"},null);
					setElementWithPath(respInfo,new String[]{"rm:PrecedingResourceInfoElementID"},new Integer(count).toString());
					setElementWithPath(respInfo, new String[]{"rm:ResponseType"},statuses.get(status.getStringValue()));

					Element resource = setElementWithPath(elem, new String[]{"Resource"},null);
						
					Element typeStructure = setElementWithPath(resource, new String[]{"TypeStructure"},null);
					setElementWithPath(typeStructure,new String[]{"rm:ValueListURN"},"urn:x-hazard:vocab:resourceTypes");
					setElementWithPath(typeStructure, new String[]{"rm:Value"},human.getPropertyValue("resType"));
					
					setElementWithPath(resource,new String[]{"ResourceID"},human.getId().toString());
					setElementWithPath(resource,new String[]{"Name"},human.getPropertyValue("name"));
						
					root.addContent(elem);
					count++;						
				}
			}
		}
				
		XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(doc);
	}
	*/
}
