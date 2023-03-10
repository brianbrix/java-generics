package com.example.mygenerics.extras;



@Service
public class EntityManagerRepo {
    static List<Map<String, Object>> queryList = new LinkedList<>();
    private final EntityManagerFactory entityManagerFactory;
    private final BatchUpdater batchUpdater;
    private final String CPG_TRANSACTION_ID = "cpgTransactionID";
    private final String FILE_UPLOAD_ID = "fileUploadID";
    final String extractIDName ="extractID";

    private final String SONAR_NUISANCE_PART = "AND reconFileUploadID = :fileUploadID";
    private final String ANOTHER_SONAR_NUISANCE_PART = " AND (matchStatus = "+ExtractStatus.NOT_PROCESSED +" OR matchStatus = NULL)";


    String EXTRACT_DTO = "SELECT reconFileExtractID, reconFileUploadID, changeLogID, serviceID," +
            "currencyID, reconActionID, cpgTransactionID, receiptNumber, payerTransactionID, mobileNumber, " +
            "accountNumber, amount,  customerStatus,matchStatus,  overallStatus, overallStatusHistory, description, " +
            "active, dateCreated, dateModified, createdBy, updatedBy FROM reconFileExtracts WHERE ";

    public EntityManagerRepo(EntityManagerFactory entityManagerFactory, BatchUpdater batchUpdater) {
        this.entityManagerFactory = entityManagerFactory;
        this.batchUpdater = batchUpdater;
    }

    public int updateReconFileExtractStatus(Long extractID, Integer status, Integer overallStatus,
                                            String statusHistory, Integer updatedBy) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("status", status);
        attributes.put("statusHistory",statusHistory);
        attributes.put("updatedBy", updatedBy);
        attributes.put("overallStatus", overallStatus);
        attributes.put(extractIDName, extractID);
        Logger.info("Updating QueryList with attributes: " + attributes);
        updateList(attributes);
        Logger.info("List size: " + queryList.size());
        if (queryList.size() >= 100 ) {
            List<Map<String, Object>> list = new LinkedList<>(queryList);
            Logger.info("Running update for batch: "+ list);
            queryList.removeAll(list);
            batchUpdater.write(list,false);
        }

        return 1;
    }
    public void updateList(Map<String, Object> attributes){
        if(queryList.stream().noneMatch(map -> map.get(extractIDName).equals(attributes.get(extractIDName))))
        {
            queryList.add(attributes);
        }
    }

    public Integer updateReconFileExtractsSetDescriptionError(String description, Integer updatedBy, Integer reconFileUploadID) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("description", description);
        attributes.put("updatedBy", updatedBy);
        attributes.put("reconFileUploadID", reconFileUploadID);
        Logger.info("Running final Update with fileUploadID: " + reconFileUploadID + "with attributes: " + attributes);
        batchUpdater.write(List.of(attributes),true);
        return 1;
    }

    public void runUpdateQueryList() {
        Logger.info("Running final extract update query"+ queryList);
        if (!queryList.isEmpty())
            batchUpdater.write(queryList, false);

    }


    public List<ReconFileExtracts> findByReconFileUploadID(Integer reconFileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery("SELECT * FROM reconFileExtracts WHERE reconFileUploadID = :reconFileUploadID", ReconFileExtracts.class);
        query.setParameter("reconFileUploadID", reconFileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }

    public List<ReconFileExtracts> findDuplicateExtractsByCPGTransactionID(String cpgTransactionID, Integer fileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        var query = entityManager.createNativeQuery(EXTRACT_DTO + " cpgTransactionID = :cpgTransactionID " + SONAR_NUISANCE_PART +
                ANOTHER_SONAR_NUISANCE_PART, ReconFileExtracts.class);
        query.setParameter(CPG_TRANSACTION_ID, cpgTransactionID);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }


    public List<ReconFileExtracts> findDuplicateExtractsByPayerTransactionID(String payerTransactionID, Integer fileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery(EXTRACT_DTO + " payerTransactionID = :payerTransactionID " + SONAR_NUISANCE_PART +
                ANOTHER_SONAR_NUISANCE_PART , ReconFileExtracts.class);
        query.setParameter("payerTransactionID", payerTransactionID);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }


    public List<ReconFileExtracts> checkDuplicateExtractsByCPGTransactionIDAndPayerTransactionID(String cpgTransactionID, String payerTransactionID, Integer fileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery(EXTRACT_DTO + " payerTransactionID = :payerTransactionID AND cpgTransactionID = :cpgTransactionID " +
                SONAR_NUISANCE_PART + ANOTHER_SONAR_NUISANCE_PART , ReconFileExtracts.class);
        query.setParameter(CPG_TRANSACTION_ID, cpgTransactionID);
        query.setParameter("payerTransactionID", payerTransactionID);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;

    }

    public List<ReconFileExtracts> findDuplicatesByReceiptNumberAndCpgTransactionIDAndReconFileUploadID(String cpgTransactionID, String receiptNumber, Integer fileUploadID) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery(EXTRACT_DTO + "cpgTransactionID = :cpgTransactionID AND receiptNumber = :receiptNumber " +
                SONAR_NUISANCE_PART, ReconFileExtracts.class);
        query.setParameter(CPG_TRANSACTION_ID, cpgTransactionID);
        query.setParameter("receiptNumber", receiptNumber);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }


    public List<ReconFileExtracts> findDuplicatesByReceiptNumberAndReconFileUploadID(String receiptNumber, Integer fileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery(EXTRACT_DTO + " receiptNumber = :receiptNumber " +
                SONAR_NUISANCE_PART, ReconFileExtracts.class);
        query.setParameter("receiptNumber", receiptNumber);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }


    public List<ReconFileExtracts> findDuplicatesByCpgTransactionIDAndReconFileUploadID(String cpgTransactionID, Integer fileUploadID) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var query = entityManager.createNativeQuery(EXTRACT_DTO + "cpgTransactionID = :cpgTransactionID " +
                SONAR_NUISANCE_PART, ReconFileExtracts.class);
        query.setParameter(CPG_TRANSACTION_ID, cpgTransactionID);
        query.setParameter(FILE_UPLOAD_ID, fileUploadID);
        List<ReconFileExtracts> resultList = query.getResultList();
        entityManager.close();
        return resultList;
    }



}
