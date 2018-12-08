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
        String calendar_name = "";
        String date = "";
        int time = -1;
        String dayofweek = "";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("body") != null) {
                JSONObject qps = (JSONObject)parser.parse((String) event.get("body"));
                logger.log(qps.toJSONString());
                if ( qps.get("name") != null) {
                    calendar_name = (String)qps.get("name");
                }
                if ( qps.get("date") != null) {
                    date = (String)qps.get("date");
                }
                if ( qps.get("time") != null) {
                    time = Integer.parseInt((String)qps.get("time"));
                }
                if ( qps.get("dayofweek") != null) {
                    dayofweek = (String)qps.get("dayofweek");
                }
                
            }
            int calendarId = validateCalendar(calendar_name, context);
            if (calendarId == -1) {
            	responseJson.put("isBase64Encoded", false);
                responseJson.put("statusCode", 400);
                responseJson.put("error", "Calendar does not exist!");
            }
            else {
            	closeTimeSlot(calendarId, date, time, dayofweek, context);
                
                JSONObject responseBody = new JSONObject();
                responseBody.put("input", event.toJSONString());
                responseBody.put("body", "Sucess");

                responseJson.put("isBase64Encoded", false);
                responseJson.put("statusCode", responseCode);
                responseJson.put("body", responseBody.toString());
            }  

        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
    }
    
    private int validateCalendar(String calendar_name, Context context) {
    	LambdaLogger logger = context.getLogger();
    	int calendarId = -1;
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();

    	    // Get calendar id
    	    String calendarIdQuery = String.format("SELECT id FROM cms_db.Calendars WHERE name='%s'", calendar_name);
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);
    	    if (resultSet.next()) {
    	    	calendarId = resultSet.getInt("id");
    	    }
    	    resultSet.close();
    	    
    	    stmt.close();
    	    conn.close();
    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    	return calendarId;
	}

	public void closeTimeSlot(int calendarId, String date, int time, String dayofweek, Context context) {
    	LambdaLogger logger = context.getLogger();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    //	Close a selected timeslot
    	    String closeTimeSlot = "";
    	    if (date != "" && time != -1 && dayofweek == "") {
    	    	closeTimeSlot = String.format("UPDATE cms_db.TimeSlots SET status = -1 "
    	    			+ "WHERE calendarId = %d AND date = '%s' AND time = %d",
    	    			calendarId, date, time);
    	    }
    	    else if (date == "" && time != -1 && dayofweek != "") {
    	    	closeTimeSlot = String.format("UPDATE cms_db.TimeSlots SET status = -1 "
    	    			+ "WHERE calendarId = %d AND time = %d AND DATENAME(dw, date) = '%s'",
    	    			calendarId, time, dayofweek);
    	    }
    	    else if (date != "" && time == -1 && dayofweek == "") {
    	    	closeTimeSlot = String.format("UPDATE cms_db.TimeSlots SET status = -1 "
    	    			+ "WHERE calendarId = %d AND date = '%s'",
    	    			calendarId, date);
    	    }
    	    else if (date == "" && time != -1 && dayofweek != "") {
    	    	closeTimeSlot = String.format("UPDATE cms_db.TimeSlots SET status = -1 "
    	    			+ "WHERE calendarId = %d AND time = %d",
    	    			calendarId, time);
    	    }
    	    stmt.executeUpdate(closeTimeSlot);
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}


