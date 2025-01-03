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

volumes:
- name: destination-volume
  persistentVolumeClaim:
    claimName: your-pvc-name
volumeMounts:
- name: destination-volume
  mountPath: /mnt/destination



import java.io.IOException;
import java.nio.file.*;

public class FileMover {
    public static void main(String[] args) {
        // Source and destination directories
        Path sourceDir = Paths.get("/tmp/testresults");
        Path destinationDir = Paths.get("/mnt/destination");

        try {
            // Ensure the destination directory exists
            if (!Files.exists(destinationDir)) {
                Files.createDirectories(destinationDir);
            }

            // Copy files and directories recursively
            Files.walk(sourceDir).forEach(sourcePath -> {
                try {
                    Path destinationPath = destinationDir.resolve(sourceDir.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(destinationPath)) {
                            Files.createDirectories(destinationPath);
                        }
                    } else {
                        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    System.err.println("Error copying file: " + sourcePath + " -> " + e.getMessage());
                }
            });

            System.out.println("Files copied successfully.");
        } catch (IOException e) {
            System.err.println("Error setting up file copy: " + e.getMessage());
        }
    }
}





