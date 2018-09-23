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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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


public class GeneTagDatasourceTest
{
	protected GeneTagDatasource datasource;
	protected boolean verbose = false;

	public GeneTagDatasourceTest() {}

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
		String key = "P00027739T0000";
		Data dataIn = new Data(Discriminators.Uri.GET, key);
		String result = datasource.execute(dataIn.asJson());
		if (verbose)
			System.out.println(result);

		JSONObject json = parseJson(result);
		JSONObject payload = (JSONObject) json.get("payload");
		JSONObject text = (JSONObject) payload.get("text");
		String value = (String) text.get("@value");
		assertEquals("Wrong result for P00027739T0000", "gamma", value.substring(6, 11));

		// I would really like to do the test using the serializer to parse the json string:
		//Data dataOut = Serializer.parse(result, Data.class);
		//Object pl = dataOut.getPayload();
		// The problem is that I do not know how to deal with that Data class. It
		// responds to the getPayLoad() message but I cannot find the code for that
		// one anywhere. In this case it returns a java.util.LinkedHashMap, but I
		// don't know whether I can rely on that.
		//System.out.println(pl);
		//System.out.println(pl.getClass());
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

	//@Test
	public void testReadLines() throws IOException
	{
		BufferedReader reader = open("/sentences.txt");
		String line = reader.readLine();
		System.out.println(line);
		while (line != null) {
			System.out.println(line);
			line = reader.readLine();
		}

	}

	//@Test
	public void testCollectStream() throws IOException
	{
		BufferedReader reader = open("/sentences.txt");
		String content = reader.lines().collect(Collectors.joining("\n"));
		System.out.println(content);
	}

	//@Test
	public void testForEachLine() throws IOException
	{
		BufferedReader reader = open("/sentences.txt");
		reader.lines().forEach(System.out::println);
	}

	private BufferedReader open(String name) {
		InputStream stream = this.getClass().getResourceAsStream("/sentences.txt");
		assertNotNull(stream);
		return new BufferedReader(new InputStreamReader(stream));
	}

	/**
	 * Utility method for simple JSON parsing.
	 * @param jsonString
	 * @return A JSONObject for the input string
	 */
	private JSONObject parseJson(String jsonString) {
		JSONObject result;
		try {
			JSONParser parser = new JSONParser();
			result = (JSONObject) parser.parse(jsonString);
		} catch (ParseException ex) {
			Logger.getLogger(GeneTagDatasourceTest.class.getName()).log(Level.SEVERE, null, ex);
			result = null;
		}
		return result;
	}

}
