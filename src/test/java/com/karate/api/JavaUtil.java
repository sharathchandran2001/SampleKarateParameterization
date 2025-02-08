package com.karate.api;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class JavaUtil {

	public static int stringToInt(String myString) {
		int myInt = Integer.parseInt(myString);
		return myInt;
	}

	public static String convString(String myString) {
		String myNewString = myString.toString();
		System.out.println("response.completed"+myString);
		return myNewString;
	}
	
	
	public static String fileChoose() throws Exception {

		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

		int returnValue = jfc.showOpenDialog(null);
		// int returnValue = jfc.showSaveDialog(null);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			System.out.println(selectedFile.getAbsolutePath());
			returnValue = 0;
			return selectedFile.getAbsolutePath();
			
		}
		return null;

	}


}

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

@Service
public class GitDownloadService {

    private final WebClient webClient;

    public GitDownloadService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16 MB buffer
                .build();
    }

    public Mono<File> downloadZip(String repoUrl, File downloadDir, String token) {
        return webClient.get()
                .uri(repoUrl)
                .header("Authorization", "token " + token) // Use "Bearer" for OAuth tokens if needed
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Java-WebClient")
                .retrieve()
                .bodyToFlux(DataBuffer.class) // Stream the file content as DataBuffer
                .reduce(DataBufferUtils.join()) // Combine the buffers into a single DataBuffer
                .flatMap(dataBuffer -> saveToFile(dataBuffer, downloadDir))
                .doOnError(WebClientResponseException.class, ex -> {
                    throw new RuntimeException("Error downloading file: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString());
                })
                .onErrorMap(ex -> new RuntimeException("Failed to download ZIP file", ex));
    }

    private Mono<File> saveToFile(DataBuffer dataBuffer, File downloadDir) {
        try {
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + downloadDir.getAbsolutePath());
            }

            File zipFile = new File(downloadDir, "repo.zip");

            try (FileChannel channel = new FileOutputStream(zipFile).getChannel()) {
                channel.write(dataBuffer.asByteBuffer());
            } finally {
                DataBufferUtils.release(dataBuffer); // Release the buffer
            }

            return Mono.just(zipFile);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error saving ZIP file: " + e.getMessage(), e));
        }
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class NestedJsonConverter {
    public static void main(String[] args) {
        // Input string
        String input = "feature:    2 | skipped:  0 | efficiency : 0.03 \n" +
                       "scenarios:    4 | passed:  0 | failed : 0";

        // Convert the string to structured JSON
        String jsonOutput = convertToNestedJSON(input);
        System.out.println("JSON Output:");
        System.out.println(jsonOutput);
    }

    public static String convertToNestedJSON(String input) {
        try {
            // Create the main JSON object
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode mainObject = objectMapper.createObjectNode();

            // Split the input string into lines
            String[] lines = input.split("\n");
            for (String line : lines) {
                if (line.startsWith("feature")) {
                    // Create the "features" JSON object
                    ObjectNode featuresNode = objectMapper.createObjectNode();
                    addKeyValuePairs(featuresNode, line);
                    mainObject.set("features", featuresNode);
                } else if (line.startsWith("scenarios")) {
                    // Create the "scenarios" JSON object
                    ObjectNode scenariosNode = objectMapper.createObjectNode();
                    addKeyValuePairs(scenariosNode, line);
                    mainObject.set("scenarios", scenariosNode);
                }
            }

            // Convert to JSON string
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mainObject);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static void addKeyValuePairs(ObjectNode node, String line) {
        String[] parts = line.split("\\|");
        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Add key-value pairs (detect numbers or keep as strings)
                try {
                    if (value.contains(".")) {
                        node.put(key, Double.parseDouble(value));
                    } else {
                        node.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    node.put(key, value);
                }
            }
        }
    }
}





import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogParser {
    public static Map<String, Map<String, String>> parseLogFile(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(filePath))) {
            // Read all lines and collect into a list
            List<String> allLines = lines
                    .map(String::trim) // Trim leading and trailing spaces
                    .filter(line -> !line.isEmpty()) // Exclude empty lines
                    .collect(Collectors.toList());

            // Process lines and group into submissions
            Map<String, Map<String, String>> submissions = new LinkedHashMap<>();
            Map<String, String> currentSubmission = null;
            String currentKey = null;

            for (String line : allLines) {
                if (line.endsWith("submission completed")) {
                    // New submission block starts
                    currentKey = line;
                    currentSubmission = new LinkedHashMap<>();
                    submissions.put(currentKey, currentSubmission);
                } else if (currentSubmission != null) {
                    String[] parts = line.split(":", 2); // Split into key-value pairs
                    if (parts.length == 2) {
                        currentSubmission.put(parts[0].trim(), parts[1].trim());
                    } else {
                        currentSubmission.put("Result", line); // Handle the "Result Received" line
                    }
                }
            }
            return submissions;
        }
    }

    public static void main(String[] args) throws IOException {
        String filePath = "output.log";
        Map<String, Map<String, String>> data = parseLogFile(filePath);

        // Print parsed data
        data.forEach((key, value) -> {
            System.out.println(key + " -> " + value);
        });
    }
}


import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LogValidationSteps {
    private Map<String, Map<String, String>> parsedLogData;

    @Given("I parse the log file {string}")
    public void parseLogFile(String filePath) throws Exception {
        parsedLogData = LogParser.parseLogFile(filePath);
    }

    @Then("the log data should match the following")
    public void validateLogData(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String testType = row.get("test_type");
            String workflowIdExists = row.get("Workflow_id_exits");
            String submissionIdExists = row.get("submission_id_exists");
            String durationExists = row.get("Duration_exists");
            String outputStatus = row.get("output status");

            // Get the parsed data for this test type
            Map<String, String> logEntry = parsedLogData.get(testType);

            if ("Yes".equalsIgnoreCase(workflowIdExists)) {
                assertEquals("Workflow ID exists validation failed for " + testType,
                        true, logEntry.containsKey("Workflow ID"));
            }
            if ("Yes".equalsIgnoreCase(submissionIdExists)) {
                assertEquals("Submission ID exists validation failed for " + testType,
                        true, logEntry.containsKey("Submission ID"));
            }
            if ("Yes".equalsIgnoreCase(durationExists)) {
                assertEquals("Duration exists validation failed for " + testType,
                        true, logEntry.containsKey("Duration"));
            }
            assertEquals("Output status validation failed for " + testType,
                    outputStatus, logEntry.get("Result"));
        }
    }
}




public String executeGradleCommand(String command) {
    return Mono.create(sink -> {
        try {
            // Start the Gradle process
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            Process process = processBuilder.start();

            // Read process output
            StringBuilder outputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append(System.lineSeparator()); // Collect output
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Complete Mono with output or error
            if (exitCode == 0) {
                sink.success(outputBuilder.toString()); // Pass the complete output
            } else {
                sink.error(new RuntimeException("Gradle process failed with exit code: " + exitCode));
            }
        } catch (Exception e) {
            sink.error(e); // Handle exceptions
        }
    }).block(); // Block the Mono to get the final string result
}


//=====


            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean captureLines = false; // Flag to start capturing lines
                int linesCaptured = 0;       // Counter for lines captured

                while ((line = reader.readLine()) != null) {
                    // Check for "| threads:" and start capturing the next 2 lines
                    if (line.contains("| threads:")) {
                        captureLines = true; // Enable capturing
                        outputBuilder.append(line).append(System.lineSeparator()); // Include the "| threads:" line
                    } else if (captureLines && linesCaptured < 2) {
                        outputBuilder.append(line).append(System.lineSeparator());
                        linesCaptured++;
                        // Stop capturing after 2 lines
                        if (linesCaptured == 2) {
                            captureLines = false;
                        }
                    }
                }
            }





//


public String executeGradleCommand(String command) {
    return Flux.create(sink -> {
        try {
            // Start the Gradle process
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            Process process = processBuilder.start();

            // Read process output reactively
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean captureLines = false; // Flag to start capturing lines
                int linesCaptured = 0;       // Counter for lines captured

                while ((line = reader.readLine()) != null) {
                    // Emit lines to the sink
                    sink.next(line);

                    // Check for "| threads:" and start capturing the next 2 lines
                    if (line.contains("| threads:")) {
                        captureLines = true; // Enable capturing
                        sink.next(line);     // Emit the "| threads:" line
                    } else if (captureLines && linesCaptured < 2) {
                        sink.next(line);     // Emit captured lines
                        linesCaptured++;
                        if (linesCaptured == 2) {
                            captureLines = false; // Stop capturing after 2 lines
                        }
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Complete the sink or emit an error
            if (exitCode == 0) {
                sink.complete(); // Indicate the Flux is complete
            } else {
                sink.error(new RuntimeException("Gradle process failed with exit code: " + exitCode));
            }
        } catch (Exception e) {
            sink.error(e); // Handle exceptions
        }
    })
    .filter(line -> line.contains("| threads:") || line.trim().length() > 0) // Filter lines with content
    .collectList() // Collect all emitted lines into a List
    .map(lines -> String.join(System.lineSeparator(), lines)) // Join lines into a single String
    .block(); // Block to return the final string
}


//////
public String executeGradleCommand(String command) {
    return Flux.create(sink -> {
        try {
            // Start the Gradle process
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            Process process = processBuilder.start();

            // Read process output reactively
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean captureLines = false; // Flag to start capturing lines
                int linesCaptured = 0;       // Counter for lines captured

                while ((line = reader.readLine()) != null) {
                    // Check for "| threads:" and start capturing the next 2 lines
                    if (line.contains("| threads:")) {
                        captureLines = true; // Enable capturing
                        linesCaptured = 0;   // Reset line counter for new block
                        continue;            // Skip the "| threads:" line itself
                    }

                    // Capture the next two lines after "| threads:"
                    if (captureLines && linesCaptured < 2) {
                        sink.next(line);     // Emit the line to the Flux
                        linesCaptured++;
                        if (linesCaptured == 2) {
                            captureLines = false; // Stop capturing after 2 lines
                        }
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Complete the sink or emit an error
            if (exitCode == 0) {
                sink.complete(); // Indicate the Flux is complete
            } else {
                sink.error(new RuntimeException("Gradle process failed with exit code: " + exitCode));
            }
        } catch (Exception e) {
            sink.error(e); // Handle exceptions
        }
    })
    // Collect and join the relevant lines
    .collectList() // Collect all emitted lines into a List
    .map(lines -> String.join(System.lineSeparator(), lines)) // Join lines into a single String
    .block(); // Block to return the final string
}


///


// Read process output reactively
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean captureLines = false; // Flag to start capturing lines
                int linesCaptured = 0;       // Counter for lines captured

                while ((line = reader.readLine()) != null) {
                    // Check for "| threads:" and start capturing the next 2 lines
                    if (line.contains("| threads:")) {
                        captureLines = true; // Enable capturing
                        linesCaptured = 0;   // Reset line counter for new block
                        continue;            // Skip the "| threads:" line itself
                    }

                    // Capture the next two lines after "| threads:"
                    if (captureLines && linesCaptured < 2) {
                        sink.next(line);     // Emit the line to the Flux
                        linesCaptured++;
                        if (linesCaptured == 2) {
                            captureLines = false; // Stop capturing after 2 lines
                        }
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Complete the sink or emit an error
            if (exitCode == 0) {
                sink.complete(); // Indicate the Flux is complete
            } else {
                sink.error(new RuntimeException("Gradle process failed with exit code: " + exitCode));
            }
        } catch (Exception e) {
            sink.error(e); // Handle exceptions
        }
    })
    // Collect and join the relevant lines
    .collectList() // Collect all emitted lines into a List
    .map(lines -> String.join(System.lineSeparator(), lines)) // Join lines into a single String
    .block(); // Block to return the final string

// list string to json

private String convert2JSONString(List<String> lines) {
    // Regex patterns for features and scenarios
    Pattern featuresPattern = Pattern.compile("features:\\s+(\\d+)\\s+\\|\\s+skipped:\\s+(\\d+)\\s+\\|\\s+efficiency\\s+:\\s+([\\d.]+)");
    Pattern scenariosPattern = Pattern.compile("scenarios:\\s+(\\d+)\\s+\\|\\s+passed:\\s+(\\d+)\\s+\\|\\s+failed\\s+:\\s+(\\d+)");

    // Initialize the JSON map
    Map<String, Map<String, Object>> jsonMap = lines.stream()
        .flatMap(line -> {
            // Create a stream of optional key-value pairs for matched patterns
            Matcher featuresMatcher = featuresPattern.matcher(line);
            Matcher scenariosMatcher = scenariosPattern.matcher(line);

            Map<String, Map<String, Object>> result = new HashMap<>();

            if (featuresMatcher.find()) {
                Map<String, Object> featuresData = new HashMap<>();
                featuresData.put("count", Integer.parseInt(featuresMatcher.group(1)));
                featuresData.put("skipped", Integer.parseInt(featuresMatcher.group(2)));
                featuresData.put("efficiency", Double.parseDouble(featuresMatcher.group(3)));
                result.put("features", featuresData);
            }

            if (scenariosMatcher.find()) {
                Map<String, Object> scenariosData = new HashMap<>();
                scenariosData.put("count", Integer.parseInt(scenariosMatcher.group(1)));
                scenariosData.put("passed", Integer.parseInt(scenariosMatcher.group(2)));
                scenariosData.put("failed", Integer.parseInt(scenariosMatcher.group(3)));
                result.put("scenarios", scenariosData);
            }

            return result.entrySet().stream();
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Convert the map to a JSON string
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(jsonMap);
    } catch (Exception e) {
        throw new RuntimeException("Error converting to JSON", e);
    }
}



import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MapLookup {
    private final Map<String, String> dataMap;

    public MapLookup() {
        this.dataMap = new HashMap<>(); // Efficient, no duplicate keys
        this.dataMap.put("key1", "value1");
        this.dataMap.put("key2", "value2");
        this.dataMap.put("key3", "value3");
    }

    public Optional<String> getValue(String key) {
        return dataMap.entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .map(Map.Entry::getValue)
                .findFirst(); // Returns an Optional to handle missing keys
    }

    public static void main(String[] args) {
        MapLookup lookup = new MapLookup();
        System.out.println(lookup.getValue("key2").orElse("Key not found"));
    }
}



curl -X POST \
     http://my-selenium-service.namespace1.svc.cluster.local:4444/your-endpoint \
     -H "Content-Type: application/json" \
     -H "Connection: close" \
     -d '{"repo": "repo1", "project": "project1"}'





	import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GradleTestRunner {

    /**
     * Runs the Gradle test task, waits for its completion, then parses the Karate report file
     * "overview-features.html" found in "target/cucumber-html-reports" to extract scenario counts,
     * passed, and failed values. Returns a JSON string with these values.
     *
     * Expected report text format (example):
     *   "Scenarios: 10 Passed: 8 Failed: 2"
     *
     * @param gradleCommand The Gradle command to run (e.g., "./gradlew clean test")
     * @return A JSON string like {"scenarios":10, "passed":8, "failed":2} or an error JSON.
     */
    public String runGradleTestAndParseKarateReport(String gradleCommand) {
        try {
            // Start the Gradle process
            ProcessBuilder processBuilder = new ProcessBuilder(gradleCommand.split(" "));
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = processBuilder.start();

            // Optionally, log the Gradle output to the console
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Wait for the Gradle process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Gradle test failed with exit code: " + exitCode);
            }

            // Wait for the report directory to be created, with a timeout
            Path reportDir = Paths.get("target/cucumber-html-reports");
            int attempts = 0;
            while (!Files.exists(reportDir) && attempts < 10) {
                Thread.sleep(2000); // Wait 2 seconds between attempts
                attempts++;
            }
            if (!Files.exists(reportDir)) {
                throw new RuntimeException("Report directory not found after waiting");
            }

            // Define the expected report file
            Path reportFile = reportDir.resolve("overview-features.html");
            if (!Files.exists(reportFile)) {
                throw new RuntimeException("Report file 'overview-features.html' not found in " + reportDir.toString());
            }

            // Read the HTML content of the report file
            String reportHtml = new String(Files.readAllBytes(reportFile), StandardCharsets.UTF_8);

            // Parse the HTML using jsoup
            Document doc = Jsoup.parse(reportHtml);
            // Extract the full text of the document; adjust if you can narrow it down further.
            String text = doc.text();

            // Use regex to extract scenario count, passed, and failed values.
            // This regex expects text like: "Scenarios: 10 Passed: 8 Failed: 2"
            Pattern pattern = Pattern.compile("Scenarios:\\s*(\\d+).*Passed:\\s*(\\d+).*Failed:\\s*(\\d+)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) {
                throw new RuntimeException("Could not parse scenario, passed, and failed counts from the report.");
            }
            int scenarios = Integer.parseInt(matcher.group(1));
            int passed = Integer.parseInt(matcher.group(2));
            int failed = Integer.parseInt(matcher.group(3));

            // Build the JSON response using a map
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("scenarios", scenarios);
            resultMap.put("passed", passed);
            resultMap.put("failed", failed);

            // Convert the map to a JSON string using Jackson ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(resultMap);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"An error occurred while processing the Karate report.\"}";
        }
    }

    public static void main(String[] args) {
        GradleTestRunner runner = new GradleTestRunner();
        String gradleCommand = "./gradlew clean test";  // Adjust the command as needed
        String resultJson = runner.runGradleTestAndParseKarateReport(gradleCommand);
        System.out.println("Result JSON: " + resultJson);
    }
}




import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class HtmlTableParser {

    public static void main(String[] args) throws IOException {
        // **REPLACE THIS WITH YOUR ACTUAL HTML SOURCE**
        String html = """
                <table>
                ... (Your HTML table content here) ...
                </table>"""; //  Make sure to put your real HTML here.

        Document doc = Jsoup.parse(html);

        Element table = doc.selectFirst("table");

        if (table != null) {
            int totalPassedScenarios = 0;
            int totalFailedScenarios = 0;


            // Select all rows *except* the header row (if there is one)
            Elements rows = table.select("tr:not(:first-child)"); // :not(:first-child) excludes the first row

            for (Element row : rows) {
                Elements cells = row.select("td");

                // Assuming the order: Passed, Failed, Skipped, Pending, Undefined, Total, Passed (Scenarios), Failed (Scenarios), Total (Scenarios), Duration, Status
                if (cells.size() >= 9) { // Ensure at least 9 cells to avoid IndexOutOfBoundsException
                    try {
                        int passedScenarios = Integer.parseInt(cells.get(6).text().trim()); // Index 6 is "Passed" Scenarios
                        int failedScenarios = Integer.parseInt(cells.get(7).text().trim()); // Index 7 is "Failed" Scenarios

                        totalPassedScenarios += passedScenarios;
                        totalFailedScenarios += failedScenarios;

                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing scenario counts in a row: " + row.text());
                         // Handle the error appropriately, e.g., set to 0 or skip the row
                    }
                } else {
                    System.err.println("Row has fewer than 9 cells: " + row.text());
                }
            }

            int totalScenarios = totalPassedScenarios + totalFailedScenarios;

            System.out.println("Total Passed Scenarios: " + totalPassedScenarios);
            System.out.println("Total Failed Scenarios: " + totalFailedScenarios);
            System.out.println("Total Scenarios: " + totalScenarios);

        } else {
            System.out.println("Table not found.");
        }
    }
}


//≈======== process async wait 

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;

public String executeGradleCommand(String command) {
    try {
        // Start the process
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true); // Merge stderr with stdout
        Process process = processBuilder.start();

        // Create a Flux to read the process output line by line
        Flux<String> outputFlux = Flux.create(sink -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sink.next(line);
                }
                sink.complete();
            } catch (Exception ex) {
                sink.error(ex);
            }
        });

        // Create a Mono that waits for the process to complete.
        // You can set a timeout here if needed.
        Mono<Integer> exitCodeMono = Mono.fromCallable(() -> process.waitFor())
                                         .timeout(Duration.ofSeconds(300)); // adjust timeout as needed

        // Combine waiting for process completion and reading the output.
        // If the exit code is not 0, signal an error.
        String result = exitCodeMono.flatMap(exitCode -> {
            if (exitCode != 0) {
                // If the process failed, propagate an error with your desired message
                return Mono.error(new RuntimeException("{\"error\": \"Gradle process failed with exit code: " + exitCode + "\"}"));
            }
            // Otherwise, collect the output lines into a single String
            return outputFlux.collectList()
                             .map(lines -> String.join(System.lineSeparator(), lines));
        }).onErrorResume(e -> {
            // Optionally, catch errors here and return your predefined JSON error message
            return Mono.just("{\"error\": \"Something went wrong during process execution: " + e.getMessage() + "\"}");
        }).block();  // Block to get the final result

        return result;
    } catch (Exception e) {
        e.printStackTrace();
        return "{\"error\": \"An unexpected error occurred.\"}";
    }
}



