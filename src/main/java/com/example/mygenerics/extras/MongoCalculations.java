package io.credable.processorservice.service.impl;

import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import io.credable.processorservice.db.entity.ColumnConfigEntity;
import io.credable.processorservice.db.entity.ProfileConfig;
import io.credable.processorservice.db.repo.ColumnConfigRepo;
import io.credable.processorservice.db.repo.ProfileConfigRepo;
import io.credable.processorservice.db.repo.ReconciliationRepo;
import io.credable.processorservice.dto.KafkaMessage;
import io.credable.processorservice.dto.SummaryDto;
import io.credable.processorservice.enums.ReconStatus;
import io.credable.processorservice.service.MatcherService;
import io.credable.processorservice.service.MessageSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class MatcherServiceImpl implements MatcherService {
    @Autowired
    private MongoTemplate mongoTemplate;
    private final ColumnConfigRepo columnConfigRepo;
    private final ReconciliationRepo reconciliationRepo;
    private final MessageSenderService messageSenderService;
    private final ProfileConfigRepo profileConfigRepo;

    @Override
    public void doMatch(KafkaMessage kafkaMessage) {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        var reconId = kafkaMessage.getReconId();
        var columnConfig = columnConfigRepo.findAllByProfileId(UUID.fromString(kafkaMessage.getProfileId()));
        String leftCollection = reconId + "_leftSideExtracted";
        String rightCollection = reconId + "_rightSideExtracted";
        SummaryDto summaryDto = new SummaryDto();



        String[] uniqueFields = columnConfig.stream().filter(ColumnConfigEntity::getIsUnique)
                .map(ColumnConfigEntity::getColumnName).toArray(String[]::new);
        Queue<Document> documentList = new LinkedBlockingQueue<>();
        executorService.submit(() -> {
            var foundInBoth = getFoundInBoth(uniqueFields, leftCollection, rightCollection);
            summaryDto.setCountOfFoundInBoth(foundInBoth.size());
            documentList.add(createDocument("foundInBoth", foundInBoth));
        });

        executorService.submit(() -> {
            List<Document> leftUnMatched = getUniqueFromComposite(uniqueFields, leftCollection, rightCollection);
            summaryDto.setCountOfUniqueOnLeft(leftUnMatched.size());
            documentList.add(createDocument("uniqueToLeft", leftUnMatched));
        });

        executorService.submit(() -> {
            List<Document> rightUnMatched = getUniqueFromComposite(uniqueFields, rightCollection, leftCollection);
            summaryDto.setCountOfUniqueOnRight(rightUnMatched.size());
            documentList.add(createDocument("uniqueToRight", rightUnMatched));
        });

        executorService.submit(() -> {
            List<Document> leftDuplicates = getDuplicates(uniqueFields, leftCollection);
            summaryDto.setCountOfDuplicatesOnLeft(leftDuplicates.size());
            documentList.add(createDocument("leftDuplicates", leftDuplicates));
        });

        executorService.submit(() -> {
            List<Document> rightDuplicates = getDuplicates(uniqueFields, rightCollection);
            summaryDto.setCountOfDuplicatesOnRight(rightDuplicates.size());
            documentList.add(createDocument("rightDuplicates", rightDuplicates));
        });


        columnConfig.forEach(c ->
                {
                    executorService.submit(() -> {
                        if (c.getOperations().contains("summation")) {
                            log.info("Column to be summed: {}", c.getColumnName());
                            var leftSum = getSumOfColumn(leftCollection, c.getColumnName());
                            Map<String, BigDecimal> mp = new HashMap<>();
                            mp.put("leftSide", leftSum);
                            Document sums = new Document();
                            sums.put("key", "summations");
                            sums.put("columnName", c.getColumnName());
                            sums.put("leftSide", leftSum);
                            log.info("leftSide: {}", leftSum);
                            var rightSum = getSumOfColumn(rightCollection, c.getColumnName());
                            log.info("rightSide: {}", rightSum);
                            mp.put("rightSide", rightSum);
                            var diff = leftSum.subtract(rightSum);
                            log.info("Diff: {}", diff);
                            mp.put("difference", diff.abs());
                            summaryDto.getSums().put(c.getColumnName(), mp);
                            sums.put("rightSide", rightSum);
                            sums.put("difference", diff.abs());
                            var max = leftSum.max(rightSum);
                            BigDecimal percentage = diff.abs().divide(max, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                            sums.put("percentDiff", percentage);
                            log.info("Sum of column: {}:{}", c.getColumnName(), sums);
                            documentList.add(sums);


                        }

                        if (c.getOperations().contains("average")) {
                            log.info("Column to be averaged: {}", c.getColumnName());
                            var leftAverage = getAverageOfColumn(leftCollection, c.getColumnName());
                            Map<String, BigDecimal> mp = new HashMap<>();
                            mp.put("leftSide", leftAverage);

                            Document avs = new Document();
                            avs.put("key", "averages");
                            avs.put("columnName", c.getColumnName());
                            avs.put("leftSide", leftAverage);

                            var rightAverage = getAverageOfColumn(rightCollection, c.getColumnName());
                            mp.put("rightSide", rightAverage);
                            mp.put("difference", leftAverage.subtract(rightAverage).abs());
                            summaryDto.getAverages().put(c.getColumnName(), mp);

                            avs.put("rightSide", rightAverage);
                            var diff = leftAverage.subtract(rightAverage).abs();
                            avs.put("difference", diff);
                            var max = leftAverage.max(rightAverage);
                            BigDecimal percentage = diff.abs().divide(max, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                            avs.put("percentDiff", percentage);
                            log.info("Average of column: {}:{}", c.getColumnName(), avs);
                            documentList.add(avs);

                        }
                        if (c.getOperations().contains("comparison")) {
                            log.info("Column to be compared: {}", c.getColumnName());
                            if (c.getDataType().equals("number")) {
                                try {
                                    log.info("Sum Comparison: Unique uniqueFields: {},Field: {},CompareWith: {}", uniqueFields, c.getColumnName(), c.getCompareWithOnRight());
                                    var leftComparison = compareSums(uniqueFields, c.getColumnName(), leftCollection, rightCollection, c.getCompareWithOnRight(), profileConfigRepo.findById(UUID.fromString(kafkaMessage.getProfileId())).get());

                                    Document compResults = new Document();
                                    compResults.put("key", "sumComparison");
                                    compResults.put("columnName", c.getColumnName());
                                    compResults.put("value", leftComparison);
                                    documentList.add(compResults);


                                } catch (Exception e) {
                                    log.error("Unable to compare column {}", c.getColumnName(), e);
                                }


                            } else if (c.getDataType().equals("text")) {
                                try {
                                    log.info("Left Text Comparison: Unique uniqueFields: {},Field: {},CompareWith: {}", uniqueFields, c.getColumnName(), c.getCompareWithOnRight());
                                    var leftComparison = compareTextField(uniqueFields, leftCollection, rightCollection, c.getColumnName(), c.getCompareWithOnRight());


                                    Document compResults = new Document();
                                    compResults.put("key", "textComparison");
                                    compResults.put("columnName", c.getColumnName());
                                    compResults.put("value", leftComparison);
                                    documentList.add(compResults);

                                } catch (Exception e) {
                                    log.error("Unable to compare column {}", c.getColumnName(), e);
                                }
                            }

                        }
                    });

                }
        );

        awaitTerminationAfterShutdown(executorService);
        // Checking if all tasks have finished executing
        boolean isFinished = executorService.isTerminated();
        log.info("Checking if all tasks have finished executing: {}", isFinished);
        if (isFinished) {
            log.info("Details Collection: {}", reconId + "_details");
            log.info("Summary Collection: {}", reconId + "_summary");
            log.info("Summary {}", summaryDto);
            mongoTemplate.insert(documentList, reconId + "_details");
            mongoTemplate.insert(summaryDto, reconId + "_summary");
            var reconciliation = reconciliationRepo.findById(UUID.fromString(reconId));
            if (reconciliation.isPresent()) {
                reconciliation.get().setReconStatus(ReconStatus.PROCESSED.getStatus());
                reconciliationRepo.save(reconciliation.get());
            }
            messageSenderService.sendMessage(kafkaMessage);

        }



    }


    // Helper method to create a Document object
    private static Document createDocument(String key, Object value) {
        Document document = new Document();
        document.put("key", key);
        document.put("value", value);
        return document;
    }

    private void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        log.info("Shutdown: {}", threadPool);
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(120, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public List<Map<String, Object>> compareSums(String[] uniqueFields, String summationField, String firstCollection, String secondCollection, List<String> compareWithFields, ProfileConfig profileConfig) {
        log.info(Arrays.toString(uniqueFields) + "}{" + summationField);

        log.info("Unique Fields: {}", Arrays.toString(uniqueFields));


        Aggregation aggregation1 = Aggregation.newAggregation(

                Aggregation.project(uniqueFields).and(ConvertOperators.ToDecimal.toDecimal("$" + summationField)).as("decimalValue"),
                Aggregation.group(uniqueFields).sum("decimalValue").as("total_sum")
        );


        List<Document> sum1 = mongoTemplate.aggregate(aggregation1, firstCollection, Document.class).getMappedResults();

        List<Document> sum2 = getMultipleFieldSums(uniqueFields, secondCollection, compareWithFields);

        Map<String, BigDecimal> sums1 = formSums(uniqueFields, sum1, profileConfig);

        Map<String, BigDecimal> sums2 = formSums(uniqueFields, sum2, profileConfig);
        List<Map<String, Object>> results = new LinkedList<>();
        for (String key : sums1.keySet()) {
            BigDecimal value1 = sums1.get(key);
            BigDecimal value2 = sums2.get(key);
            if (value2 != null && value1 != null) {
//                if (value2.compareTo(value1) != 0) {
                    Criteria[] criteriaList = new Criteria[uniqueFields.length];
                    for (int i = 0; i < uniqueFields.length; i++) {
                        criteriaList[i] = Criteria.where(uniqueFields[i]).is(key.split("\\|\\|")[i]);
                    }


                    Set<Document> doc1 = new HashSet<>(mongoTemplate.find(new Query().addCriteria(new Criteria().andOperator(criteriaList)), Document.class, firstCollection));
                    Set<Document> doc2 = new HashSet<>(mongoTemplate.find(new Query().addCriteria(new Criteria().andOperator(criteriaList)), Document.class, secondCollection));
                    if (!doc1.isEmpty() && !doc2.isEmpty()) {
                        value1 = value1.abs();
                        value2 = value2.abs();

                        var max = value1.max(value2);
                        var difference = value2.subtract(value1).abs();
                        BigDecimal percentage = difference.divide(max, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                        results.add(Map.of("leftSide", doc1, "rightSide", doc2, "difference", difference, "percentDiff", percentage));
                    }
//                }
            }
        }

        return results;
    }

    public List<Document> getMultipleFieldSums(String[] uniqueFields, String collectionName, List<String> compareWithFields) {

        GroupOperation groupOperation = Aggregation.group(uniqueFields);
        List<AggregationOperation> operations = new ArrayList<>();

        for (String compareWithField : compareWithFields) {
            operations.add(
                    Aggregation.project(uniqueFields)
                            .and(ConvertOperators.ToDecimal.toDecimal("$" + compareWithField)).as(compareWithField + "_decimalValue")
            );

            groupOperation = groupOperation.sum("%s_decimalValue".formatted(compareWithField)).as("sum_" + compareWithField);
            operations.add(groupOperation);
        }

        ProjectionOperation projectionOperation = Aggregation.project()
                .andExpression(String.join(" + ", compareWithFields.stream().map(f -> "sum_" + f).toArray(String[]::new)))
                .as("total_sum");
        operations.add(projectionOperation);


        Aggregation aggregation = Aggregation.newAggregation(
                operations
        );


        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, collectionName, Document.class
        );

        return results.getMappedResults();
    }


    private static Map<String, BigDecimal> formSums(String[] fields, List<Document> sum, ProfileConfig profileConfig) {
        Map<String, BigDecimal> sums = new HashMap<>();
        for (Document doc : sum) {
            StringBuilder sb = new StringBuilder();
            if (fields.length > 1) {
                for (String field : fields) {

                    Document d = (Document) doc.get("_id");
                    sb.append(d.get(field)).append("||");
                }
            } else {
                sb.append(doc.get("_id")).append("||");
            }

            String key = sb.substring(0, sb.length() - 2);
            BigDecimal value = getBigDecimal(doc.get("total_sum"));
            sums.put(key, value);
        }
        return sums;
    }

    public static BigDecimal getBigDecimal(Object value) {
        BigDecimal ret = null;
        if (value != null) {
            ret = switch (value) {
                case BigDecimal bigDecimal -> bigDecimal;
                case String s -> new BigDecimal(s);
                case BigInteger bigInteger -> new BigDecimal(bigInteger);
                case Integer number -> new BigDecimal(number);
                case Number number -> BigDecimal.valueOf(number.doubleValue());
                case null, default ->
                        throw new ClassCastException("Not possible to coerce [" + value + "] from class " + value.getClass() + " into a BigDecimal.");
            };
        }
        return ret;
    }


    private List<Document> getUniqueFromComposite(String[] fieldNames, String firstCollection, String secondCollection) {
        Query query = new Query();
        Criteria[] criteriaList = new Criteria[fieldNames.length];

        for (int i = 0; i < fieldNames.length; i++) {
            criteriaList[i] = Criteria.where(fieldNames[i]).nin(getDistinctFieldValues(fieldNames[i], secondCollection));

        }
        Criteria criteria = new Criteria().andOperator(criteriaList);
        query.addCriteria(criteria);

        return mongoTemplate.find(query, Document.class, firstCollection);

    }


    private List<Document> foundInBoth(String[] fieldNames, String firstCollection, String secondCollection) {
        Query query = new Query();
        Criteria[] criteriaList = new Criteria[fieldNames.length];

        for (int i = 0; i < fieldNames.length; i++) {
            criteriaList[i] = Criteria.where(fieldNames[i]).in(getDistinctFieldValues(fieldNames[i], secondCollection));

        }
        Criteria criteria = new Criteria().andOperator(criteriaList);
        query.addCriteria(criteria);

        return mongoTemplate.find(query, Document.class, firstCollection);

    }

    private Map<String, List<Document>> getFoundInBoth(String[] fieldNames, String firstCollection, String secondCollection) {
        var rs1 = foundInBoth(fieldNames, firstCollection, secondCollection);
        var rs2 = foundInBoth(fieldNames, secondCollection, firstCollection);
        rs1.addAll(rs2);

        return rs1.stream()
                .collect(Collectors.groupingBy(document -> String.valueOf(Arrays.stream(fieldNames).map(fieldName -> String.valueOf(document.get(fieldName))).toList())));
    }

    private List<Document> compareTextField(String[] fieldNames, String firstCollection, String secondCollection, String fieldForComparison, List<String> compareWithFields) {
        Query query = new Query();
        Criteria[] criteriaList = new Criteria[fieldNames.length];

        for (int i = 0; i < fieldNames.length; i++) {
            criteriaList[i] = Criteria.where(fieldNames[i]).is(secondCollection + "." + fieldNames[i]);

        }

        Criteria criteria = new Criteria().andOperator(criteriaList).and(fieldForComparison).ne(secondCollection + "." + compareWithFields.get(0));
        query.addCriteria(criteria);

        return mongoTemplate.find(query, Document.class, firstCollection);

    }

    private List<Document> getDuplicates(String[] uniqueFields, String collection) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group(uniqueFields)
                        .addToSet("$$ROOT").as("records")
                        .count().as("count"),
                Aggregation.match(Criteria.where("count").gt(1))
        );

        return mongoTemplate.aggregate(aggregation, collection, Document.class).getMappedResults();

    }


    public List<String> getDistinctFieldValues(String fieldName, String collection) {
        DistinctIterable<String> distinctIterable = mongoTemplate.getCollection(collection).distinct(fieldName, String.class);
        List<String> values;
        try (MongoCursor<String> iterator = distinctIterable.iterator()) {
            values = new ArrayList<>();
            while (iterator.hasNext()) {
                values.add(iterator.next());
            }
        }
        return values;
    }


    public BigDecimal getSumOfColumn(String collectionName, String fieldName) {
        List<String> values = mongoTemplate
                .getCollection(collectionName)
                .find()
                .projection(Projections.include(fieldName))
                .map(document -> String.valueOf(document.get(fieldName)))
                .into(new ArrayList<>());

        return !values.isEmpty() ? sum(values) : BigDecimal.ZERO;

    }

    public BigDecimal getAverageOfColumn(String collectionName, String fieldName) {
        List<String> values = mongoTemplate
                .getCollection(collectionName)
                .find()
                .projection(Projections.include(fieldName))
                .map(document -> String.valueOf(document.get(fieldName)))
                .into(new ArrayList<>());

        return !values.isEmpty() ? sum(values).divide(new BigDecimal(values.size()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

    }

    public static BigDecimal sum(List<String> amounts) {
        try {
            return amounts.stream()
                    .map(x -> new BigDecimal(x).abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }

    }
}
