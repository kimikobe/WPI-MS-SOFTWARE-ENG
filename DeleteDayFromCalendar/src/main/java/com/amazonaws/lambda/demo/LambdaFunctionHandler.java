package com.amazonaws.lambda.demo;

import java.io.*;
import java.sql.*;
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
            logger.log(event.toString());
        	if (event.get("date") != null) {
        		date = (String) event.get("date");
            }
        	 
            if (event.get("name") != null) {
            	name = (String) event.get("name");
            }
            
            if (date.equals("") || name.equals("")) {
            	throw new Exception("Input parameters error");
            }

            JSONObject temp = checkCalendar(name, context);
	        if (temp.get("calendar") == null) {
	        	throw new Exception("Calendar not exist!");
	        }
	        temp = (JSONObject) temp.get("calendar");
            int id = (int) temp.get("id");
        	int start_time = (int) temp.get("start_time");
        	int end_time = (int) temp.get("end_time");
        	int duration = (int) temp.get("duration");
        	
			if (checkDateInTimeslot(id, date, context)){
        		throw new Exception("Date already exist in calendar!");
        	}
	        
	        RemoveDayFromCalendar(date, id, context);
            
            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode); 

        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
    }
    
    
    public JSONObject checkCalendar(String name, Context context) throws Exception {
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
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}

    	return rs;
    }
    
    public boolean checkDateInTimeslot(int id, String date, Context context) throws Exception {
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
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}
    	
		return true;
    	
    }
    
    public void RemoveDayFromCalendar(String date, int id, Context context) throws Exception {
    	LambdaLogger logger = context.getLogger();  	
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    
		    String newDate = String.format("DELETE FROM cms_db.TimeSlots WHERE date = '%s' AND calendarId = %d", date, id);
		    stmt.executeUpdate(newDate);
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}
    }

}