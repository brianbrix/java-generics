package io.credable.reconapi.util;

import io.credable.reconapi.dto.SftpConfigRequest;
import io.credable.reconapi.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @project: recon-ui-api
 * @author: brianmokandu
 * @date 16/06/2023
 **/
@Log4j2
@RequiredArgsConstructor
public class SftpToFile {
    private static String getFileExtension(String fileName) {
        Path path = Path.of(fileName);
        String extension = "";

        if (path.getFileName() != null) {
            String fileNameStr = path.getFileName().toString();
            int dotIndex = fileNameStr.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileNameStr.length() - 1) {
                extension = fileNameStr.substring(dotIndex + 1);
            }
        }

        return extension;
    }

    public MultipartFile[] getFilesFromFtp(MongoTemplate mongoTemplate, SftpConfigRequest sftpConfig, String[] fileNames) throws IOException {
        log.info("SFTP CONFIG: {}", sftpConfig);
        String host = sftpConfig.getHost();
        String username = sftpConfig.getUsername();
        String privateKey = sftpConfig.getPrivateKey();

        String passphrase = sftpConfig.getPassphrase();
        int port = sftpConfig.getPort() != 0 ? sftpConfig.getPort() : 22;
        List<String> downloadedFilePaths = new ArrayList<>();
        Path keyFile = null;
        try (SSHClient ssh = new SSHClient()) {
            try {
                // Use a PromiscuousVerifier to trust any host key
                HostKeyVerifier promiscuousVerifier = new PromiscuousVerifier();

                // Configure the SSH client with the PromiscuousVerifier
                ssh.addHostKeyVerifier(promiscuousVerifier);

                // Connect to the remote server
                ssh.connect(host, port);
                if (privateKey != null) {

                    var doc = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("metadata.filename").is(sftpConfig.getId() + "_" + privateKey)), Document.class, "myKeyFiles");
                    if (doc == null)
                        throw new CustomException(HttpStatus.EXPECTATION_FAILED, "Unable to find key file");
                    var binary = (org.bson.types.Binary) doc.get("content");
                    var fileBytes = binary.getData();
                    String ext = getFileExtension(privateKey);
                    try {
                        if (fileBytes != null) {
                            keyFile = Files.createTempFile("temp-", ".%s".formatted(ext)); // Creates a temporary file with a random name and the specified extension
                            Files.write(keyFile, fileBytes); // Write the byte array to the temporary file
                            log.info("Key file recreated: " + keyFile);
                        }
                    } catch (IOException e) {
                        log.info("Error recreating key file: " + e.getMessage());
                        throw new CustomException(HttpStatus.EXPECTATION_FAILED, e.getMessage());

                    }
                    String path = Objects.requireNonNull(keyFile).toAbsolutePath().toString();
                    log.info("Path " + path + " created");
                    // Specify the private key for authentication
                    KeyProvider keyProvider = ssh.loadKeys(path);
                    if (passphrase != null)
                        keyProvider = ssh.loadKeys(path, passphrase);

                    // Authenticate using the private key
                    ssh.authPublickey(username, keyProvider);
                } else {
                    ssh.authPassword(username, passphrase);
                }

                // Create an SFTP client

                try (SFTPClient sftp = ssh.newSFTPClient()) {
                    // Specify the remote folder path
                    String remoteFolderPath = sftpConfig.getLocation().endsWith("/") ? sftpConfig.getLocation() : sftpConfig.getLocation() + "/";

                    // List the files in the remote folder
                    List<RemoteResourceInfo> files = sftp.ls(remoteFolderPath);

                    // Iterate over the files and download each one
                    log.info("FileNames: {}", Arrays.asList(fileNames));
                    for (RemoteResourceInfo file : files) {
                        if (file.isRegularFile() && Arrays.asList(fileNames).contains(file.getName())) {
                            String remoteFilePath = remoteFolderPath + file.getName();
                            Path localFilePath = Files.createTempFile("temp-", file.getName());
                            sftp.get(remoteFilePath, localFilePath.toString());
                            downloadedFilePaths.add(localFilePath.toString());
                        }
                    }
                }
                // Close the SFTP client
            } catch (Exception e) {
                e.printStackTrace();
                throw new CustomException(HttpStatus.EXPECTATION_FAILED, e.getMessage());

            } finally {
                // Disconnect from the remote server
                ssh.disconnect();
                assert keyFile != null;
                log.info("Deleting key file: {}", Files.deleteIfExists(keyFile));
            }
        }

        // Convert the downloaded file paths to MultipartFile objects
        MultipartFile[] downloadedFiles = new MultipartFile[downloadedFilePaths.size()];
        for (int i = 0; i < downloadedFilePaths.size(); i++) {
            String filePath = downloadedFilePaths.get(i);
            Path downloadPath = Paths.get(filePath);
            String fileName = downloadPath.getFileName().toString();
            String contentType = Files.probeContentType(downloadPath);
            byte[] fileBytes2 = Files.readAllBytes(downloadPath);
            downloadedFiles[i] = new MockMultipartFile(fileName, fileName, contentType, fileBytes2);
            log.info("Deleting downloaded file after saving as multipart: {}", Files.deleteIfExists(downloadPath));

        }


        return downloadedFiles;
    }

}
