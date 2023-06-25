package io.credable.reconapi.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class CustomMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String filename;
    private final String contentType;

    public CustomMultipartFile(byte[] content, String filename, String contentType) {
        this.content = content;
        this.filename = filename;
        this.contentType = contentType;
    }

    public static MultipartFile fromFile(File file, String contentType) throws IOException {
        byte[] fileContent;
        try (InputStream inputStream = new FileInputStream(file)) {
            fileContent = inputStream.readAllBytes();
        }
        return new CustomMultipartFile(fileContent, file.getName(), contentType);
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            outputStream.write(content);
        }
    }
}
