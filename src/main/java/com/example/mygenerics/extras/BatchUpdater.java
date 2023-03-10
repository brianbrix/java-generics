package com.example.mygenerics.extras;


@AllArgsConstructor
@Service
public class BatchUpdater {

    @Autowired
    MultitenantJdbcTemplateProvider multitenantJdbcTemplateProvider;
    @Autowired
    RedisRepoService redisRepoService;

    /**
     * @param attributes
     */
    @Retryable(value = DataAccessException.class, maxAttempts =4 )
    public void write(List<Map<String, Object>> attributes, boolean isFinal) {

        Logger.info("Starting update... "+attributes);
         String sql = "UPDATE table1 SET matchStatus = ?, overallStatus = ?, updatedBy = ?, " +
                " overallStatusHistory= CONCAT(COALESCE(overallStatusHistory,''), IF(overallStatusHistory IS NULL,'',', ')," +
                "?) WHERE reconFileExtractID = ? AND matchStatus IS NULL";
        JdbcTemplate jdbcTemplate = multitenantJdbcTemplateProvider.getJdbcTemplate();
        BatchPreparedStatementSetter pss;
        String extractIds[] = new String[attributes.size()];
        if(!isFinal) {
            pss = new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Map<String, Object> argument = attributes.get(i);
                    extractIds[i] = argument.get("extractID").toString();
                    ps.setInt(1, (Integer) argument.get("status"));
                    ps.setInt(2, (Integer) argument.get("overallStatus"));
                    ps.setInt(3, (Integer) argument.get("updatedBy"));
                    ps.setLong(5, (Long) argument.get("extractID"));
                    ps.setString(4, (String) argument.get("statusHistory"));
                }

                @Override
                public int getBatchSize() {
                    return attributes.size();
                }
            };

        }
            else
            {

                sql = "UPDATE table2 SET description =?"
                        + " ,updatedBy =?"
                        + " WHERE reconFileUploadID = ?";
                pss = new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Map<String, Object> argument = attributes.get(i);

                        
                        ps.setString(1, (String) argument.get("description"));
                        ps.setInt(2, (Integer) argument.get("updatedBy"));
                        ps.setInt(3, (Integer) argument.get("reconFileUploadID"));

                    }
                    @Override
                    public int getBatchSize() {
                        return attributes.size();
                    }
                };


        }

        int [] rowsInserted = jdbcTemplate.batchUpdate(sql, pss);
        Logger.info("Updated count :: " + rowsInserted.length);
        if (rowsInserted.length > 0 && !isFinal) {
                Logger.info("Saving extracts to Redis: "+ Arrays.toString(extractIds));
                redisRepoService.rightPushListToRedis("reconFileExtracts", extractIds);
            }



    }



}
