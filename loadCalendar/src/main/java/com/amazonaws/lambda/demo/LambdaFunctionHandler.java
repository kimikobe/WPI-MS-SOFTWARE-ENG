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

            if (event.get("name") != null) {
            	name = (String) event.get("name");
            }
            if (event.get("start_date") != null) {
            	start_date = (String) event.get("start_date");
            }
            if (event.get("end_date") != null) {
            	end_date = (String) event.get("end_date");
            }

            if (name.equals("")) {
	            JSONArray calendarList = getCalendarList(context);
	            responseJson.put("calendarList", calendarList);
            }
            else {
            	JSONArray timeslots = getTimeSlots(name, start_date, end_date, context);
            	responseJson.put("timeslots", timeslots);
            }

            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode); 


        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
    }

    private JSONArray getTimeSlots(String name, String start_date, String end_date, Context context) throws Exception {
    	LambdaLogger logger = context.getLogger();
    	JSONArray timeslotList = new JSONArray();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    // Get calendar id
    	    String calendarIdQuery = String.format("SELECT id FROM cms_db.Calendars WHERE name='%s'", name);
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);
    	    int calendarId = -1;
    	    if (resultSet.next()) {
    	    	calendarId = resultSet.getInt("id");
    	    }
    	    resultSet.close();
    	    
    	    if (calendarId == -1) throw new Exception("Calendar does not exist!");
    	    
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

    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}
    	
		return timeslotList;
	}

	public JSONArray getCalendarList(Context context) throws Exception {
    	LambdaLogger logger = context.getLogger();
    	JSONArray calendarList = new JSONArray();

    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    //	Get all calendars name
    	    String calendarIdQuery = "SELECT id, name, duration, start_time, end_time FROM cms_db.Calendars";
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);

    	    while (resultSet.next()) {
    	    	JSONObject calendar = new JSONObject();
    	    	calendar.put("id", resultSet.getInt("id"));
    	    	calendar.put("name", resultSet.getString("name"));
    	    	calendar.put("duration", resultSet.getInt("duration"));
    	    	calendar.put("start_time", resultSet.getInt("start_time"));
    	    	calendar.put("end_time", resultSet.getInt("end_time"));
    	    	calendarList.add(calendar);
    	    }
    	    
    	    resultSet.close();

    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}

    	return calendarList;
    }
}