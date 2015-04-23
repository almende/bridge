/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.swarm.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.DefaultServlet;

import com.almende.eve.capabilities.Config;
import com.almende.eve.config.YamlReader;
import com.almende.eve.deploy.Boot;
import com.almende.eve.transport.http.ServletLauncher;
import com.almende.eve.transport.http.embed.JettyLauncher;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.google.inject.Guice;
import com.google.inject.servlet.GuiceFilter;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.DefaultModule;
import com.graphhopper.http.GHServletModule;
import com.graphhopper.util.CmdArgs;

/**
 * The Class Main.
 */
public class Main {
	private static GraphHopper	hopper	= null;

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws ServletException
	 *             the servlet exception
	 */
	public static void main(String[] args) throws FileNotFoundException,
			ServletException {
		// Load Eve agents
		// Load navigation data (first through normal graphhopper init, later by
		// merging with Eve configuration.

		final CmdArgs cargs = CmdArgs.read(args);
		final Config configfile = YamlReader.load(new FileInputStream(new File(
				cargs.get("eveyaml", "eve.yaml"))));
		Boot.boot(configfile);
		final ServletLauncher launcher = new JettyLauncher();

		final DefaultModule module = new DefaultModule(cargs);
		Guice.createInjector(module, new GHServletModule(cargs));
		launcher.addFilter(GuiceFilter.class.getName(), "/*");
		launcher.add(new DefaultServlet(),
				URIUtil.create("http://localhost:8080/"),
				JOM.createObjectNode());

		hopper = module.getGraphHopper();
	}

	/**
	 * Gets the hopper.
	 *
	 * @return the hopper
	 */
	public static GraphHopper getHopper() {
		return hopper;
	}

}
