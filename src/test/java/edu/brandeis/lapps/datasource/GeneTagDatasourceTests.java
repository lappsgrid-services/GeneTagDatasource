/*
 * Copyright (c) 2018 Brandeis University 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.brandeis.lapps.datasource;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;


public class GeneTagDatasourceTests
{
	protected GeneTagDatasource datasource;
	protected boolean verbose = false;

	public GeneTagDatasourceTests() {}

	@Before
	public void setup()
	{
		// Note that this  this will be executed every time a test method runs, moving
		// this code to BioAsqDatasourceTests() made no difference. With more tests
		// we will need another way to do set up.
		System.setProperty(GeneTagDatasource.PROPERTY_NAME, "src/main/resources/index-identifiers.txt");
		datasource = new GeneTagDatasource();
	}
	
	@After
	public void teardown()
	{
		datasource = null;
	}

	@Test
	public void testIndex()
	{
		// Testing whether we get the right kind of discriminator and the right number of
		// entries in the index ("http://vocab.lappsgrid.org/ns/ok" and 8 respectively)
		Data data = new Data(Discriminators.Uri.SIZE);
		String json = datasource.execute(data.asJson());
		data = Serializer.parse(json, Data.class);
		assertEquals("Invalid discriminator", Discriminators.Uri.OK, data.getDiscriminator());
		assertEquals("Wrong number of entries", 2251, Integer.parseInt(data.getPayload().toString()));
	}

	@Test
	public void testDump()
	{
		if (verbose) {
			System.out.println();
			datasource.dump();
			System.out.println(); }
	}

	@Test
	public void testGet()
	{
		// The key and the query are in both the sample data set and the full training
		// data, so this should worked also when debugging with a small set.
		String key = "589a246c78275d0c4a000032";
		String query = "Which 2 medications are included in the Qsymia pill?";
		Data data = new Data(Discriminators.Uri.GET, key);
		//System.out.println(data.asJson());
		String json = datasource.execute(data.asJson());
		JSONObject result = parseJson(json);
		assertEquals("Wrong query for identifier", query, result.get("body"));
		if (verbose)
			System.out.println("\n" + json.substring(0, 300) + " ...\n");
	}

	@Test
	public void testList()
	{
		Data data = new Data(Discriminators.Uri.LIST);
		String json = datasource.execute(data.asPrettyJson());
		JSONObject result = parseJson(json);
		ArrayList list = (ArrayList) result.get("payload");
		assertEquals("Wrong size of list", 2251, list.size());
		if (verbose)
			System.out.println("\n" + list + "\n");
	}

	/**
	 * Utility method for simple JSON parsing.
	 * @param jsonString
	 * @return 
	 */
	private JSONObject parseJson(String jsonString) {
		JSONObject result;
		try {
			JSONParser parser = new JSONParser();
			result = (JSONObject) parser.parse(jsonString);
		} catch (ParseException ex) {
			Logger.getLogger(GeneTagDatasourceTests.class.getName()).log(Level.SEVERE, null, ex);
			result = null;
		}
		return result;
	}

}
