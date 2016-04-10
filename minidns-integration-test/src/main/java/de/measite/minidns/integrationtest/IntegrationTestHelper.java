/*
 * Copyright 2015 the original author or authors
 *
 * This software is licensed under the Apache License, Version 2.0,
 * the GNU Lesser General Public License version 2 or later ("LGPL")
 * and the WTFPL.
 * You may choose either license to govern your use of this software only
 * upon the condition that you accept all of the terms of either
 * the Apache License 2.0, the LGPL 2.1+ or the WTFPL.
 */
package de.measite.minidns.integrationtest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationTestHelper {
    private static Set<Class<?>> testClasses = new HashSet<>();
    private static Logger LOGGER = Logger.getLogger(IntegrationTestHelper.class.getName());

    enum TestResult {
        Success,
        Failure,
        ;
    }

    static {
        testClasses.add(CoreTest.class);
        testClasses.add(DNSSECTest.class);
        testClasses.add(DaneTest.class);
        testClasses.add(HlaTest.class);
    }

    public static void main(String[] args) {
        int testsRun = 0;
        List<Method> successfulTests = new ArrayList<>();
        List<Method> failedTests = new ArrayList<>();
        for (final Class<?> aClass : testClasses) {
            for (final Method method : aClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(IntegrationTest.class)) {
                    TestResult result = invokeTest(method, aClass);
                    testsRun++;
                    switch (result) {
                    case Success:
                        successfulTests.add(method);
                        break;
                    case Failure:
                        failedTests.add(method);
                        break;
                    }
                }
            }
        }
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("MiniDNS Integration Test Result: [").append(successfulTests.size()).append('/').append(testsRun).append("] ");
        int exitStatus = 0;
        if (failedTests.isEmpty()) {
            resultMessage.append("SUCCESS \\o/");
        } else {
            resultMessage.append("FAILURE :(");
            exitStatus = 2;
        }
        LOGGER.info(resultMessage.toString());
        System.exit(exitStatus);
    }

    public static TestResult invokeTest(Method method, Class<?> aClass) {
        Class<?> expected = method.getAnnotation(IntegrationTest.class).expected();
        if (!Exception.class.isAssignableFrom(expected)) expected = null;

        try {
            method.invoke(null);

            if (expected != null) {
                LOGGER.logp(Level.WARNING, aClass.getName(), method.getName(), "Test failed: expected exception " + expected + " was not thrown!");
                return TestResult.Failure;
            } else {
                LOGGER.logp(Level.INFO, aClass.getName(), method.getName(), "Test suceeded.");
                return TestResult.Success;
            }
        } catch (InvocationTargetException e) {
            if (expected != null && expected.isAssignableFrom(e.getTargetException().getClass())) {
                LOGGER.logp(Level.INFO, aClass.getName(), method.getName(), "Test suceeded.");
                return TestResult.Success;
            } else {
                LOGGER.logp(Level.WARNING, aClass.getName(), method.getName(), "Test failed: unexpected exception was thrown: ", e.getTargetException());
                return TestResult.Failure;
            }
        } catch (IllegalAccessException | NullPointerException e) {
            LOGGER.logp(Level.SEVERE, aClass.getName(), method.getName(), "Test failed: could not invoke test, is it public static?");
            return TestResult.Failure;
        }
    }
}
