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

import org.json.simple.JSONArray;
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
        
        String name = "";
        String start_date = "";
        String end_date = "";

        try {

        	JSONObject event = (JSONObject)parser.parse(reader);
        	
        	if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject)event.get("queryStringParameters");
                if (qps.get("name") != null) {
                	name = (String) qps.get("name");
                }
                if (qps.get("start_date") != null) {
                	start_date = (String) qps.get("start_date");
                }
                if (qps.get("end_date") != null) {
                	end_date = (String) qps.get("end_date");
                }
            }
        	
            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            
            if (name == "") {
	            JSONObject calendarList = getCalendarList(context);
	            responseBody.put("body", calendarList);
            }
            else {
            	JSONObject timeslots = getTimeSlots(name, start_date, end_date, context);
            	responseBody.put("body", timeslots);
            }

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

    

    private JSONObject getTimeSlots(String name, String start_date, String end_date, Context context) {
    	LambdaLogger logger = context.getLogger();
    	JSONObject rs = new JSONObject();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    // Get calendar id
    	    String calendarIdQuery = String.format("SELECT id FROM cms_db.Calendars WHERE name='%s'", name);
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);
    	    int calendarId = 0;
    	    if (resultSet.next()) {
    	    	calendarId = resultSet.getInt("id");
    	    }
    	    resultSet.close();
    	    
    	    //	Get all timeslots in the date range
    	    String timeslotQuery = String.format("SELECT date, start_time, duration, status, person, location FROM cms_db.TimeSlots WHERE "
    	    		+ "(calendarId = %d)", calendarId);
    	    if (start_date != "") {
    	    	timeslotQuery += String.format(" AND (date >= '%s')", start_date);
    	    }
    	    if (end_date != "") {
    	    	timeslotQuery += String.format(" AND (date <= '%s')", end_date);
    	    }
    	    resultSet = stmt.executeQuery(timeslotQuery);
    	    JSONArray timeslotList = new JSONArray();
    	    while (resultSet.next()) {
    	    	JSONObject timeslot = new JSONObject();
    	    	String rs_date = resultSet.getString("date");
    	    	int rs_start_time = resultSet.getInt("start_time");
    	    	int rs_duration = resultSet.getInt("duration");
    	    	int rs_status = resultSet.getInt("status");
    	    	String rs_person = resultSet.getString("person");
    	    	String rs_location = resultSet.getString("location");
    	    	timeslot.put("date", rs_date);
    	    	timeslot.put("start_time", rs_start_time);
    	    	timeslot.put("duration", rs_duration);
    	    	timeslot.put("status", rs_status);
    	    	timeslot.put("person", rs_person);
    	    	timeslot.put("location", rs_location);
    	    	timeslotList.add(timeslot);
    	    }
    	    resultSet.close();
    	    rs.put("timeslots", timeslotList);

    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    	
		return rs;
	}



	public JSONObject getCalendarList(Context context) {
    	LambdaLogger logger = context.getLogger();
    	JSONObject rs = new JSONObject();

    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    //	Get all calendars name
    	    String calendarIdQuery = "SELECT id, name FROM cms_db.Calendars";
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);

    	    JSONArray calendarList = new JSONArray();
    	    while (resultSet.next()) {
    	    	JSONObject calendar = new JSONObject();
    	    	calendar.put("id", resultSet.getInt("id"));
    	    	calendar.put("name", resultSet.getString("name"));
    	    	calendarList.add(calendar);
    	    }
    	    
    	    rs.put("calendars", calendarList);

    	    resultSet.close();

    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}

    	return rs;
    }
}

