/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import com.almende.eve.transport.http.embed.JettyLauncher;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * The Class RESTApplication.
 */
public class RESTApplication extends Application {
	private static final Logger	LOG	= Logger.getLogger(RESTApplication.class
											.getName());

	/*
	 * (non-Javadoc)
	 * @see javax.ws.rs.core.Application#getClasses()
	 */
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(ResourceProxy.class);
		return classes;
	}

	 @Override
	  public Set<Object> getSingletons() {
	    Set<Object> s = new HashSet<Object>();

	    // See http://wiki.fasterxml.com/JacksonJAXBAnnotations for more information
	    ObjectMapper mapper = JOM.getInstance();
	    AnnotationIntrospector primary = new JaxbAnnotationIntrospector( TypeFactory.defaultInstance() );
	    AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();
	    AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
	    mapper.getDeserializationConfig().with(pair);
	    mapper.getSerializationConfig().with(pair);

	    // Set up the provider
	    JacksonJaxbJsonProvider jaxbProvider = new JacksonJaxbJsonProvider();
	    jaxbProvider.setMapper(mapper);

	    s.add(jaxbProvider);
	    return s;
	  }
	
	/**
	 * Inits the.
	 */
	public static void init() {
		Servlet servlet = new org.apache.wink.server.internal.servlet.RestServlet();
		ObjectNode params = JOM.createObjectNode();
		ArrayNode initParams = JOM.createArrayNode();
		ObjectNode param = JOM.createObjectNode();
		param.put("key", "javax.ws.rs.Application");
		param.put("value", RESTApplication.class.getName());
		initParams.add(param);
		params.set("initParams", initParams);

		JettyLauncher launcher = new JettyLauncher();
		try {
			launcher.add(servlet, new URI("/rs/"), params);
		} catch (URISyntaxException e) {
			LOG.log(Level.WARNING, " Failed to init REST", e);
		} catch (ServletException e) {
			LOG.log(Level.WARNING, " Failed to init REST", e);
		}
	}

}
