package com.spring2025.vietchefs.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AzureBlobStorageService {
    private final BlobContainerClient blobContainerClient;
    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    public AzureBlobStorageService(@Value("${azure.storage.connection-string}") String connectionString,
                                   @Value("${azure.storage.container-name}") String containerName) {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        this.blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    public String uploadFile(MultipartFile file) throws IOException {
        try {
            // Generate a unique file name with a random UUID
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            // Create a BlobClient to interact with Azure Blob Storage
            BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

            // Create BlobHttpHeaders and set Content-Type
            BlobHttpHeaders headers = new BlobHttpHeaders();

            // Check file type and set Content-Type accordingly
            if (file.getContentType().equals("image/jpeg")) {
                headers.setContentType("image/jpeg");  // For JPEG images
            } else if (file.getContentType().equals("image/png")) {
                headers.setContentType("image/png");   // For PNG images
            } else {
                // Default to octet-stream if type is not recognized
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Image cannot access.");
            }

            // Upload the file to Azure Blob Storage with the correct Content-Type
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            blobClient.setHttpHeaders(headers);

            // Return the URL of the uploaded file
            return blobClient.getBlobUrl();
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw new IOException("Error uploading file to Azure Blob Storage", e);
        }
    }
}
