package io.mishustin.client.testrail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class TestRailApiClient {

    public static final Logger LOG = LogManager.getLogger(TestRailApiClient.class);

    private final String suiteId;
    private final String authLine;
    private final HttpClient httpClient;
    private final String addTestRunUrl;
    private final String addTestResultsUrl;

    public TestRailApiClient(String projectId, String suiteId, String login, String pass, String host) {
        this.suiteId = suiteId;
        this.authLine = "Basic " + Base64.getEncoder().encodeToString((login + ":" + pass).getBytes(StandardCharsets.UTF_8));

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.addTestRunUrl = host + "/index.php?/api/v2/add_run/" + projectId;
        this.addTestResultsUrl = host + "/index.php?/api/v2/add_results_for_cases/";
    }

    public HttpResponse<String> makePostCall(String url, JSONObject body) {
        try {

            URI uri = new URI(url);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .header("Content-Type", "application/json")
                    .uri(uri)
                    .setHeader("Authorization", authLine)
                    .build();

            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            LOG.debug("Response: {} - {}", send.statusCode(), send.body());
            return send;
        } catch (IOException | InterruptedException | URISyntaxException ex) {
            LOG.error(ex);
            throw new RuntimeException(ex);
        }
    }

    public int createTestRun(String name, List<Integer> testCaseIds) {
        JSONObject newRunRequestBody = new JSONObject();
        newRunRequestBody.put("suite_id", suiteId);
        newRunRequestBody.put("name", name);
        newRunRequestBody.put("include_all", false);
        newRunRequestBody.put("case_ids", testCaseIds);

        HttpResponse<String> stringHttpResponse = makePostCall(addTestRunUrl, newRunRequestBody);
        return new JSONObject(stringHttpResponse.body()).getInt("id");
    }

    public void sendTestResults(JSONObject results, int testRunId) {
        makePostCall(addTestResultsUrl + testRunId, results);
    }
}