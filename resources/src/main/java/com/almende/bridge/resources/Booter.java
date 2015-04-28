/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.almende.eve.deploy.Boot;

/**
 * The Class Booter.
 */
public class Booter {

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Boot.boot(new FileInputStream(new File(
				args[0])));

		// Init the wink REST interface.
		RESTApplication.init();
	}

}
