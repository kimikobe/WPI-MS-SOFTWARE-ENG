package com.amazonaws.lambda.demo;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class LambdaFunctionHandler implements RequestStreamHandler {
	JSONParser parser = new JSONParser();

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    	LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ProxyWithStream");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        String responseCode = "200";
        
        //	default value
        String date = "";
        String name = "";
                
        try {
        	JSONObject event = (JSONObject)parser.parse(reader); 
	       
        	if (event.get("date") != null) {
        		date = (String) event.get("date");
            }
        	 
            if (event.get("name") != null) {
            	name = (String) event.get("name");
            }
            
            if (date.equals("") || name.equals("")) {
            	throw new Exception("Input parameters error");
            }
                
            // read calendar detail
            JSONObject temp = checkCalendar(name, context);
	        if (temp.get("calendar") == null) {
	        	throw new Exception("Calendar not exist!");
	        }
	        temp = (JSONObject) temp.get("calendar");
            int id = (int) temp.get("id");
        	int start_time = (int) temp.get("start_time");
        	int end_time = (int) temp.get("end_time");
        	int duration = (int) temp.get("duration");
        	
			if (!checkDateInTimeslot(id, date, context)){
        		throw new Exception("Date already exist in calendar!");
        	}
	        
	        AddDayToCalendar(date, id, start_time, end_time, duration, context);
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            
            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode);
            responseJson.put("body", responseBody.toString());  

        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
    }
    
    
    public JSONObject checkCalendar(String name, Context context) {
    	LambdaLogger logger = context.getLogger();
    	JSONObject rs = new JSONObject();

    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    //	Get calendars name
    	    String calendarIdQuery = String.format("SELECT id, start_time, end_time, duration FROM cms_db.Calendars WHERE name='%s'", name);
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);

    	    while (resultSet.next()) {
    	    	JSONObject calendar = new JSONObject();
    	    	
    	    	calendar.put("id", resultSet.getInt("id"));   	    
    	    	calendar.put("start_time", resultSet.getInt("start_time"));
    	    	calendar.put("end_time", resultSet.getInt("end_time"));
    	    	calendar.put("duration", resultSet.getInt("duration"));
    	    	rs.put("calendar", calendar);
    	    }
    	    
    	    resultSet.close();

    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}

    	return rs;
    }
    
    public boolean checkDateInTimeslot(int id, String date, Context context) {
    	LambdaLogger logger = context.getLogger();

    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    String newDate = String.format("SELECT id FROM cms_db.TimeSlots WHERE "
    	    		+ "calendarId = %d AND date = '%s'", id, date);
    	    ResultSet resultSet = stmt.executeQuery(newDate);
    	    
    	    while (resultSet.next()) {
    	    	return false;
    	    }
    	    
    	    resultSet.close();
    	    
    	    stmt.close();
    	    conn.close();
    	    
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    	
		return true;
    	
    }
    
    public void AddDayToCalendar(String date, int id, int start_time, int end_time, int duration, Context context) {
    	LambdaLogger logger = context.getLogger();
    	logger.log(String.format("date: %s, id: %d, start time: %d, end time: %d, duration: %d", date, id, start_time, end_time, duration));
    	
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    for (int time = start_time; time < end_time; time += duration) {
		    	String newDate = String.format("INSERT INTO cms_db.TimeSlots (date, start_time, duration, status, person, location, calendarId) "
		    			+ "VALUES ('%s', %d, %d, %d, '%s', '%s', %d)", date, time, duration, 0, "", "", id);
		    	logger.log(newDate);
		    	stmt.executeUpdate(newDate);
    	    }
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}