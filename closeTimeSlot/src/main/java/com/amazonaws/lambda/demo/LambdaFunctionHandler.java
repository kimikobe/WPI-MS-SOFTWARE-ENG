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
        String time = "";
        String dayofweek = "";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("body") != null) {
                JSONObject qps = (JSONObject)parser.parse((String) event.get("body"));
                logger.log(qps.toJSONString());
                if ( qps.get("date") != null) {
                    date = (String)qps.get("date");
                }
                if ( qps.get("time") != null) {
                    time = (String)qps.get("time");
                }
                if ( qps.get("dayofweek") != null) {
                    dayofweek = (String)qps.get("dayofweek");
                }
                
            }

            closeTimeSlot(date, time, dayofweek, context);
            
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
    
    public void closeTimeSlot(String date, String time, String dayofweek, Context context) {
    	LambdaLogger logger = context.getLogger();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
    	    
    	    //	Close a selected timeslot
    	    String closedTimeSlot = String.format("UPDATE cms_db.TimeSlots SET date = '%s', time = '%s', dayofweek = '%s'",
    	    		date, time, dayofweek);
    	    stmt.executeUpdate(closedTimeSlot);
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}

