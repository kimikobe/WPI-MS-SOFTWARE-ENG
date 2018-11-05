package com.amazonaws.lambda.demo;

import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStream;

import java.io.InputStreamReader;

import java.io.OutputStream;

import java.io.OutputStreamWriter;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.ResultSet;

import java.sql.Statement;

import java.time.DayOfWeek;

import java.time.LocalDate;

import java.time.format.DateTimeFormatter;


import org.json.simple.JSONObject;

import org.json.simple.parser.JSONParser;


import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;




public class LambdaFunctionHandler implements RequestStreamHandler {

	JSONParser parser = new JSONParser();

	

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

    	LambdaLogger logger = context.getLogger();

        logger.log("Loading Java Lambda handler of ProxyWithStream");

        

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        JSONObject responseJson = new JSONObject();

        String responseCode = "200";

        

        String name = "personal";

        String start_date = "2018-09-24";
        
        String end_date = "2018-10-24";
        
        int start_time = 480;
        
        String date = "2018-09-26";
        
        int duration = 20;
        
        int status = 1;
        
        String person = "Jimmy";
        
        String location = "fuller";

        try {

        	JSONObject event = (JSONObject)parser.parse(reader);

            if (event.get("queryStringParameters") != null) {

                JSONObject qps = (JSONObject)event.get("queryStringParameters");

                if ( qps.get("name") != null) {

                	name = (String) qps.get("name");

                }
                
                if ( qps.get("start_date") != null) {
                	
                	start_date = (String)qps.get("start_date");
                	
                }
                
                if ( qps.get("end_date") != null) {
                	
                	end_date = (String)qps.get("end_date");
                	
                }

                if ( qps.get("start_time") != null) {
                	
                	start_time = Integer.parseInt((String)qps.get("start_time"));
                	
                }
                
                if ( qps.get("date") != null) {
                	
                	date = (String)qps.get("date");
                	
                }
                
                if ( qps.get("duration") != null) {
                	
                	duration = Integer.parseInt((String)qps.get("duration"));
                	
                }

                if ( qps.get("status") != null) {
                	
                	status = Integer.parseInt((String)qps.get("status"));
                	
                }
                
                if ( qps.get("person") != null) {
                	
                	person = (String)qps.get("person");
                	
                }
                
                if ( qps.get("location") != null) {
                	
                	location = (String)qps.get("location");
                	
                }
                
            }




            loadCalendar(name, start_time, duration, status, date, person, location, context);

            

            JSONObject responseBody = new JSONObject();

            responseBody.put("input", event.toJSONString());

            responseBody.put("body", "Success");




            responseJson.put("isBase64Encoded", false);

            responseJson.put("statusCode", responseCode);

            responseJson.put("body", responseBody.toString());  




        } catch(Exception pex) {

            responseJson.put("statusCode", "400");

            responseJson.put("exception", pex);

        }




        logger.log(responseJson.toJSONString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

        writer.write(responseJson.toJSONString());  

        writer.close();

    }

    

    public void loadCalendar(String name, int start_time, int duration, int status, String person, String date, String location, Context context) {

    	LambdaLogger logger = context.getLogger();

    	

    	try {

    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";

    	    String username = "cmsAdmin";

    	    String password = "cms:pass";




    	    Connection conn = DriverManager.getConnection(url, username, password);

    	    Statement stmt = conn.createStatement();

    	    

    	    //	Get calendar id

    	    String calendarIdQuery = String.format("SELECT id FROM cms_db.Calendars WHERE name='%s'", name);

    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);

    	    int calendarId = 0;

    	    if (resultSet.next()) {

    	    	calendarId = resultSet.getInt("id");

    	    }

    	    resultSet.close();


    	    String loadCalendar = String.format("GET a timeslot FROM cms_db.TimeSlots (calendarId, start_time, duration, status, date, person, location)" + 
    	    		"VALUES ('%s', %d, %d, %d, '%s', '%s', %d)", date, start_time, duration, 0, "", "", calendarId);

    	    stmt.executeUpdate(loadCalendar);

    	    

    	    stmt.close();

    	    conn.close();




    	} catch (Exception e) {

    	    e.printStackTrace();

    	    logger.log("Caught exception: " + e.getMessage());

    	}

    }




}

