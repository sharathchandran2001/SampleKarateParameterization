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

while ((line = reader.readLine()) != null) {
                if (foundThreads) {
                    // Capture the next two lines after "| threads:"
                    extractedOutput.append(line).append(System.lineSeparator());
                    if (--foundThreads == 0) break; // Stop after capturing 2 lines
                }

                if (line.contains("| threads:")) {
                    foundThreads = 2; // Found the marker, start capturing next 2 lines
                }
            }

            return extractedOutput.length() > 0 ? extractedOutput.toString() : "not found results";
