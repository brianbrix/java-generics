package io.credable.reconapi.util;

import com.jcraft.jsch.*;
import io.credable.reconapi.dto.SftpConfigRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Log4j2
public class FTPDownload {

    public static void download(SftpConfigRequest sftpConfigRequest, String[] filePaths) {
        log.info("Downloading");
        JSch jSch = new JSch();

        try {
            // Load the private key
            if (sftpConfigRequest.getPrivateKey() != null)
                jSch.addIdentity(sftpConfigRequest.getPrivateKey());
            if (sftpConfigRequest.getPrivateKey() != null && sftpConfigRequest.getPassphrase() != null)
                jSch.addIdentity(sftpConfigRequest.getPrivateKey(), sftpConfigRequest.getPassphrase());


            // Create a session
            Session session = jSch.getSession(sftpConfigRequest.getUsername(), sftpConfigRequest.getHost(), sftpConfigRequest.getPort());
            session.setConfig("PreferredAuthentications", "publickey");
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(60000);
            session.connect();

            // Create a channel
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(60000);
            log.info("Opened channel " + channel.getHome());

            // Download the files
            for (String filePath : filePaths) {
                InputStream inputStream = channel.get(sftpConfigRequest.getLocation() + "/" + filePath);
                File localFile = new File(filePath);
                FileOutputStream outputStream = new FileOutputStream(localFile);
                byte[] buffer = new byte[1048576];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
                MultipartFile multipartFile = CustomMultipartFile.fromFile(localFile, "application/octet-stream");

                log.info("File downloaded successfully.");

            }

            // Disconnect
            channel.disconnect();
            session.disconnect();

        } catch (JSchException | SftpException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        String ftpHost = "ftp.example.com";
//        int ftpPort = 22;
//        String ftpUsername = "username";
//        String privateKeyPath = "/path/to/private/key";
//        String passphrase = "passphrase";
//        String remoteFilePath = "/path/to/remote/file.txt";
//        String localFilePath = "/path/to/local/file.txt";
//
//        JSch jSch = new JSch();
//
//        try {
//            // Load the private key
//            jSch.addIdentity(privateKeyPath, passphrase);
//
//            // Create a session
//            Session session = jSch.getSession(ftpUsername, ftpHost, ftpPort);
//            session.setConfig("PreferredAuthentications", "publickey");
//            session.setConfig("StrictHostKeyChecking", "no");
//            session.connect();
//
//            // Create a channel
//            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
//            channel.connect();
//
//            // Download the file
//            InputStream inputStream = channel.get(remoteFilePath);
//            FileOutputStream outputStream = new FileOutputStream(localFilePath);
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//            inputStream.close();
//            outputStream.close();
//
//            // Disconnect
//            channel.disconnect();
//            session.disconnect();
//
//            log.info("File downloaded successfully.");
//        } catch (JSchException | SftpException | java.io.IOException e) {
//            e.printStackTrace();
//        }
//    }
}

