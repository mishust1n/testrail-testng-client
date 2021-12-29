package io.mishustin.client.testrail;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestRailTestNgClient {

    private final TestRailApiClient testRailApiClient;

    public TestRailTestNgClient(String projectId, String suiteId, String login, String pass, String host) {
        this.testRailApiClient = new TestRailApiClient(projectId, suiteId, login, pass, host);
    }

    public void sendTestResults(List<ISuite> suites) {

        for (ISuite suite : suites) {
            Collection<ISuiteResult> testResults = suite.getResults().values();
            for (ISuiteResult testResult : testResults) {
                ITestContext testContext = testResult.getTestContext();

                JSONArray passedTestsArray = buildTestResultsArray(testContext.getPassedTests());
                JSONArray failedTestsArray = buildTestResultsArray(testContext.getFailedTests());

                List<Integer> testCaseIds = new ArrayList<>();

                passedTestsArray.forEach(e -> testCaseIds.add(((JSONObject) e).getInt("case_id")));
                failedTestsArray.forEach(e -> testCaseIds.add(((JSONObject) e).getInt("case_id")));

                DateTimeFormatter europeanDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

                String testRunTitle = "Test Execution - " + LocalDateTime.now().format(europeanDateFormatter);

                int testRunId = testRailApiClient.createTestRun(testRunTitle, testCaseIds);

                if (passedTestsArray.length() > 0) {
                    testRailApiClient.sendTestResults(new JSONObject().put("results", passedTestsArray), testRunId);
                }
                if (failedTestsArray.length() > 0) {
                    testRailApiClient.sendTestResults(new JSONObject().put("results", failedTestsArray), testRunId);
                }
            }
        }
    }

    private JSONArray buildTestResultsArray(IResultMap iResultMap) {
        JSONArray result = new JSONArray();
        for (ITestResult testResult : iResultMap.getAllResults()) {
            TestCaseId annotationWithTestCaseId = testResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseId.class);
            if (annotationWithTestCaseId != null) {
                int testId = annotationWithTestCaseId.id();
                int status = convertTestNgTestStatusToTestRail(testResult.getStatus());
                long duration = (testResult.getEndMillis() - testResult.getStartMillis());

                JSONObject resultJsonObject = new JSONObject();
                resultJsonObject.put("case_id", testId);
                resultJsonObject.put("status_id", status);

                if (testResult.getThrowable() != null) {
                    resultJsonObject.put("comment", testResult.getThrowable().toString());
                }

                if (duration > 0) {
                    String durationStr;
                    if (duration > 1000) {
                        durationStr = duration / 1000 + "s";
                    } else {
                        durationStr = duration + "ms";
                    }
                    resultJsonObject.put("elapsed", durationStr);
                }
                result.put(resultJsonObject);
            }
        }

        return result;
    }

    private int convertTestNgTestStatusToTestRail(int status) {
        switch (status) {
            case ITestResult.SUCCESS:
                return 1;
            case ITestResult.FAILURE:
                return 5;
            default:
                return 3;
        }
    }
}