package com.amazonaws.lambda.demo;

import java.io.*;
import java.sql.*;
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
        
        //	default value
        String name = "default";
        int duration = 20;
        int start_time = 480;
        int end_time = 17 * 60;
        String start_date = "2018-09-24";
        String end_date = "2018-10-04";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("body") != null) {
                JSONObject qps = (JSONObject)parser.parse((String) event.get("body"));
                logger.log(qps.toJSONString());
                if ( qps.get("name") != null) {
                    name = (String)qps.get("name");
                }
                if ( qps.get("duration") != null) {
                	duration = Integer.parseInt((String) qps.get("duration"));
                }
                if ( qps.get("start_time") != null) {
                	start_time = Integer.parseInt((String)qps.get("start_time"));
                }
                if ( qps.get("end_time") != null) {
                	end_time = Integer.parseInt((String)qps.get("end_time"));
                }
                if ( qps.get("start_date") != null) {
                	start_date = (String)qps.get("start_date");
                }
                if ( qps.get("end_date") != null) {
                	end_date = (String)qps.get("end_date");
                }
            }

            createCalendar(name, duration, start_time, end_time, start_date, end_date, context);
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            responseBody.put("body", "Sucess");

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
    
    public void createCalendar(String name, int duration, int start_time, int end_time, String start_date, String end_date, Context context) {
    	LambdaLogger logger = context.getLogger();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    //	Create new calendar
    	    String newCalendar = String.format("INSERT INTO cms_db.Calendars (name, duration, start_time, end_time) VALUES ('%s', %d, %d, %d)",
    	    		name, duration, start_time, end_time);
    	    stmt.executeUpdate(newCalendar);
    	    
    	    //	Get calendar id
    	    String calendarIdQuery = String.format("SELECT id FROM cms_db.Calendars WHERE name='%s'", name);
    	    ResultSet resultSet = stmt.executeQuery(calendarIdQuery);
    	    int calendarId = 0;
    	    if (resultSet.next()) {
    	    	calendarId = resultSet.getInt("id");
    	    }
    	    resultSet.close();
    	    
    	    //	Create new time slots
    	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
    	    LocalDate startDate = LocalDate.parse(start_date, formatter);
			LocalDate endDate = LocalDate.parse(end_date, formatter);
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
					for (int time = start_time; time < end_time; time += duration) {
						String newTimeslot = String.format("INSERT INTO cms_db.TimeSlots (date, start_time, duration, status, person, location, calendarId) "
								+ "VALUES ('%s', %d, %d, %d, '%s', '%s', %d)", date, time, duration, 0, "", "", calendarId);
						stmt.executeUpdate(newTimeslot);
		    	    }
				}
			}
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}
