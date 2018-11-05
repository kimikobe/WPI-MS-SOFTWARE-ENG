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

        try {

        	JSONObject event = (JSONObject)parser.parse(reader);
            JSONObject responseBody = new JSONObject();
            
            JSONObject calendarList = loadCalendar(context);
            
            responseBody.put("input", event.toJSONString());
            responseBody.put("body", calendarList);

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

    

    public JSONObject loadCalendar(Context context) {
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

