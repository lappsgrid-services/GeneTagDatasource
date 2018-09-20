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
		datasource = new GeneTagDatasource();
	}

	@After
	public void teardown()
	{
		datasource = null;
	}

	@Test
	public void testSize()
	{
		// Testing whether we get the right kind of discriminator and the right number of
		// entries in the index ("http://vocab.lappsgrid.org/ns/ok" and 14996 respectively)
		Data data = new Data(Discriminators.Uri.SIZE);
		String json = datasource.execute(data.asJson());
		data = Serializer.parse(json, Data.class);
		assertEquals("Invalid discriminator", Discriminators.Uri.OK, data.getDiscriminator());
		assertEquals("Wrong number of entries", 14996, Integer.parseInt(data.getPayload().toString()));
	}

	@Test
	public void testGet()
	{
		// The key and the query are in both the sample data set and the full training
		// data, so this should worked also when debugging with a small set.
		String key = "P00055040A0000";
		Data data = new Data(Discriminators.Uri.GET, key);
		//System.out.println(data.asJson());
		String result = datasource.execute(data.asJson());
		JSONObject json = parseJson(result);
		Data dataOut = Serializer.parse(result, Data.class);
		//System.out.println(result);
		String payload = (String) dataOut.getPayload();
		assertEquals("Wrong result for identifier", "pustulosis", payload.substring(22, 32));
		//if (verbose)
		//	System.out.println("\n" + json.substring(0, 300) + " ...\n");
	}

	@Test
	public void testList()
	{
		Data data = new Data(Discriminators.Uri.LIST);
		String json = datasource.execute(data.asPrettyJson());
		JSONObject result = parseJson(json);
		ArrayList list = (ArrayList) result.get("payload");
		assertEquals("Wrong size of list", 14996, list.size());
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
