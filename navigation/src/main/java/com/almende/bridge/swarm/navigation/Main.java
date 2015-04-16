/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.swarm.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.almende.eve.capabilities.Config;
import com.almende.eve.config.YamlReader;
import com.almende.eve.deploy.Boot;
import com.graphhopper.GraphHopper;
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
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// Load Eve agents
		// Load navigation data (first through normal graphhopper init, later by
		// merging with Eve configuration.
		CmdArgs cargs = CmdArgs.read(args);
		hopper = new GraphHopper().init(cargs);
		hopper.importOrLoad();
		
		final Config configfile = YamlReader.load(
				new FileInputStream(new File(cargs.get("eveyaml", "eve.yaml"))));
		Boot.boot(configfile);
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
