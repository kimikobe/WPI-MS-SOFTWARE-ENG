package com.amazonaws.lambda.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

public class FaultLambdaFunctionHandlerTest {

    private static InputStream inputStream;
    private static OutputStream outputStream;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
    	inputStream = null;
    	JSONObject body2 = new JSONObject();
    	
    	body2.put("date", "");
    	body2.put("name", "");

    	inputStream = new ByteArrayInputStream(body2.toString().getBytes());
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }
    
    @Test
    public void testfaultLambdaFunctionHandler() throws IOException{
    	LambdaFunctionHandler handler = new LambdaFunctionHandler();
    	Context ctx = createContext();
    	
    	JSONObject responseJson = new JSONObject();
    	System.out.println("Inside testfaultLambdaFunctionHandler()");
        outputStream = new ByteArrayOutputStream();
        
        responseJson.put("statusCode", "400");
        
		handler.handleRequest(inputStream, outputStream, ctx);
    }

}