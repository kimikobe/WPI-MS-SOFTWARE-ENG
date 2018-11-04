package com.amazonaws.lambda.demo;

import java.io.*;
import java.sql.*;

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
        String start_time = "0800";
        String end_time = "1700";
        String start_date = "20180924";
        String end_date = "20181004";
        
        try {
        	JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject)event.get("queryStringParameters");
                if ( qps.get("name") != null) {
                    name = (String)qps.get("name");
                }
                if ( qps.get("duration") != null) {
                	duration = Integer.parseInt((String) qps.get("duration"));
                }
                if ( qps.get("start_time") != null) {
                	start_time = (String)qps.get("start_time");
                }
                if ( qps.get("end_time") != null) {
                	end_time = (String)qps.get("end_time");
                }
                if ( qps.get("start_date") != null) {
                	start_date = (String)qps.get("start_date");
                }
                if ( qps.get("end_date") != null) {
                	end_date = (String)qps.get("end_date");
                }
            }

            String greeting = "name: " + name + ", duration: " + duration + ", start time: " + start_time + ", end time: " + end_time;


            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            responseBody.put("message", greeting);

            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode);
            responseJson.put("body", responseBody.toString());  

        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }
        
        createCalendar(name, duration, start_time, end_time, start_date, end_date, context);

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
    }
    
    public void createCalendar(String name, int duration, String start_time, String end_time, String start_date, String end_date, Context context) {
    	LambdaLogger logger = context.getLogger();
    	
    	try {
    		String url = "jdbc:mysql://cmsdb.clnm8zsvchg3.us-east-2.rds.amazonaws.com:3306";
    	    String username = "cmsAdmin";
    	    String password = "cms:pass";

    	    Connection conn = DriverManager.getConnection(url, username, password);
    	    Statement stmt = conn.createStatement();
//    	    ResultSet resultSet = stmt.executeQuery("SELECT NOW()");
    	    
    	    
    	    
    	    stmt.close();
    	    conn.close();

    	} catch (Exception e) {
    	    e.printStackTrace();
    	    logger.log("Caught exception: " + e.getMessage());
    	}
    }

}
