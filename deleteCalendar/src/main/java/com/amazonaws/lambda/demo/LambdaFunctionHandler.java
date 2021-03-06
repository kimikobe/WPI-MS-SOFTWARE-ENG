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
        
        String name = "";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
        	logger.log(event.toJSONString());
            if ( event.get("name") != null) {
            	name = (String) event.get("name");
            }
            
            if (name.equals("")) {
            	throw new Exception("Calendar name should not be empty!");
            }

            deleteCalendar(name, context);
            
            JSONObject responseBody = new JSONObject();

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
    
    public void deleteCalendar(String name, Context context) throws Exception {
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
    	    int calendarId = -1;
    	    if (resultSet.next()) {
    	    	calendarId = resultSet.getInt("id");
    	    }
    	    resultSet.close();
    	    
    	    if (calendarId == -1) {
    	    	throw new Exception("Calendar name does not exist!");
    	    }
    	    
    	    String deleteCalendar = String.format("DELETE FROM cms_db.Calendars WHERE id=%d", calendarId);
    	    stmt.executeUpdate(deleteCalendar);
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    logger.log("Caught exception: " + e.getMessage());
    	    throw e;
    	}
    }

}
