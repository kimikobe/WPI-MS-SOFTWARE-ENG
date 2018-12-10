package com.amazonaws.lambda.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class LambdaFunctionHandlerTest {

	private static InputStream inputStream;
    private static OutputStream outputStream;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
    	inputStream = null;
    	JSONObject input = new JSONObject();
    	input.put("id", "325");
    	input.put("status", "1");
    	input.put("person", "sdsd");
    	input.put("location", "fuller");
    	inputStream = new ByteArrayInputStream(input.toString().getBytes());
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
    public void testLambdaFunctionHandler() throws IOException {
        LambdaFunctionHandler handler = new LambdaFunctionHandler();
        Context ctx = createContext();
                
        outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, ctx);

        // TODO: validate output here if needed.
        JSONObject responseJson = new JSONObject();
        String responseCode = "200";

        responseJson.put("isBase64Encoded", false);
        responseJson.put("statusCode", responseCode);

        Assert.assertEquals(responseJson.toString(), outputStream.toString());
    }
}
