package com.amazonaws.lambda.demo;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
   LambdaFunctionHandlerTest.class,
   FaultLambdaFunctionHandlerTest.class,
   FaultCalendarIdHandlerTest.class
})

public class TestSuite{
}  	
