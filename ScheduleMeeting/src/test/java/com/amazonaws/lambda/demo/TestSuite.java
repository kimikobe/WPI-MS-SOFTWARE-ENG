package com.amazonaws.lambda.demo;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
   LambdaFunctionHandlerTest.class,
   FaultLambdaFunctionHandlerTest.class
})

public class TestSuite{
}  	
