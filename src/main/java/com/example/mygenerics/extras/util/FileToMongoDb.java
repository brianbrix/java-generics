package io.credable.reconapi.util;

import com.mongodb.BasicDBObject;
import io.credable.reconapi.db.entity.UploadedFileNames;
import io.credable.reconapi.db.repo.UploadedFileNamesRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Component
@RequiredArgsConstructor
public class FileToMongoDb {
    private static final int THREAD_POOL_SIZE = 10;
    private final MongoTemplate mongoTemplate;
    private final UploadedFileNamesRepo uploadedFileNamesRepo;
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public void saveReconFilesToMongoDB(List<MultipartFile> files, String collectionName, UUID reconId, int side) {
        log.info("Saving files to MongoDB...");
        files.forEach(file -> executorService.submit(() -> {
                            try {
                                File convFile = (File) saveFile(collectionName, file, null).get("file");
                                uploadedFileNamesRepo.save(UploadedFileNames.builder().reconId(reconId).fileName(file.getOriginalFilename()).side(side).build());
                                convFile.delete();
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to save file to MongoDB", e);
                            }
                        }
                )
        );


    }

    public Document saveFile(String collectionName, MultipartFile file, String id) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        BasicDBObject metadata = new BasicDBObject();
        metadata.put("filename", id != null ? id + "_" + file.getOriginalFilename() : file.getOriginalFilename());
        metadata.put("content-type", file.getContentType());
        metadata.put("size", file.getSize());

        Document doc = new Document();
        doc.put("metadata", metadata);
        doc.put("content", Files.readAllBytes(convFile.toPath()));
        log.info("Inserting.");
        log.info(collectionName);
        mongoTemplate.insert(doc, collectionName);
        log.info("Inserted.");
        log.info("DB: {}", mongoTemplate.getDb().getName());
        doc.put("file", convFile);
        return doc;
    }
}
