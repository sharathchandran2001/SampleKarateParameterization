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


