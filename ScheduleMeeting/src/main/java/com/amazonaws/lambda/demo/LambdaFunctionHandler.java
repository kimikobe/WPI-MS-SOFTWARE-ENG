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
        int id = 1;
        int status = 1;
        String person = "";
        String location = "";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("body") != null) {
                JSONObject qps = (JSONObject)parser.parse((String) event.get("body"));
                logger.log(qps.toJSONString());
                if ( qps.get("id") != null) {
                    id = (int)qps.get("id");
                }
                if ( qps.get("status") != null) {
                    status = (int)qps.get("status");
                }
                if ( qps.get("person") != null) {
                	person = (String)qps.get("person");
                }
                if ( qps.get("location") != null) {
                	location = (String)qps.get("location");
                }
                
            }

            scheduleMeeting(id, status, person, location, context);
            
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
    
    public void scheduleMeeting(int id, int status, String person, String location, Context context) {
    	LambdaLogger logger = context.getLogger();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    //	Create new meeting
    	    String newMeeting = String.format("UPDATE cms_db.Calendars (id, status, person, location, context) VALUES ('%s', %s, %d, %d)",
    	    		id, status, person, location);
    	    stmt.executeUpdate(newMeeting);
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}

