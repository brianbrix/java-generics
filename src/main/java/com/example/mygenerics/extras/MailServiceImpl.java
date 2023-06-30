package io.credable.processorservice.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.credable.processorservice.db.repo.ProfileConfigRepo;
import io.credable.processorservice.db.repo.ReconciliationRepo;
import io.credable.processorservice.dto.KafkaMessage;
import io.credable.processorservice.dto.SummaryDto;
import io.credable.processorservice.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class MailServiceImpl implements MailService {
    private final JavaMailSender mailSender;
    private final Configuration freeMarkerConfiguration;
    private final MongoTemplate mongoTemplate;
    private final ProfileConfigRepo profileConfigRepo;
    private final ReconciliationRepo reconciliationRepo;
    @Value("${app.summary.url}")
    private String summaryUrl;

    @Autowired
    public MailServiceImpl(JavaMailSender mailSender, FreeMarkerConfigurationFactoryBean freeMarkerConfigurationFactoryBean, MongoTemplate mongoTemplate, ProfileConfigRepo profileConfigRepo, ReconciliationRepo reconciliationRepo) {
        this.mailSender = mailSender;
        this.freeMarkerConfiguration = freeMarkerConfigurationFactoryBean.getObject();
        this.mongoTemplate = mongoTemplate;
        this.profileConfigRepo = profileConfigRepo;
        this.reconciliationRepo = reconciliationRepo;
    }

    @Override
    public void sendEmail(List<String> to, List<String> cc, String subject, String templateName, String[] attachmentPaths, Object data) {
        try {

            KafkaMessage kafkaMessage = (KafkaMessage) data;
            var profile = profileConfigRepo.findById(UUID.fromString(kafkaMessage.getProfileId())).orElseThrow(() -> new IllegalStateException("Couldn't find profile'"));
            var recon = reconciliationRepo.findById(UUID.fromString(kafkaMessage.getReconId())).orElseThrow(() -> new IllegalStateException("Couldn't find Recon'"));
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            to.addAll(profile.getCommEmails());
            log.info("Sending email");
            log.info("TO: " + Arrays.toString(to.toArray(new String[0])));
            log.info("Subject: " + subject);
            log.info("CC: " + cc);
            mimeMessage.setHeader("from", "Credable Perfect Match. <reconciliation@credable.io>");
            mimeMessage.setHeader("Reply-To", "Credable Perfect Match. <reconciliation@credable.io>");

            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setTo(to.toArray(new String[0]));

//            messageHelper.setFrom("reconciliation@credable.io","Credable Perfect Match. ");
            messageHelper.setCc(cc.toArray(new String[0]));
            messageHelper.setSubject(subject);
            Query query = new Query();
            query.fields().exclude("_id");
            var rs = mongoTemplate.findOne(query, SummaryDto.class, kafkaMessage.getReconId() + "_summary");

            Template template = freeMarkerConfiguration.getTemplate("email-template_summary.ftl");
            assert rs != null;
            var contextVariables = Map.of("message", "Recon Summary for %s".formatted(profile.getProfileName()), "data", rs, "pr", profile, "recon", recon, "url", summaryUrl + kafkaMessage.getReconId());
            String emailBody = FreeMarkerTemplateUtils.processTemplateIntoString(template, contextVariables);


//            log.info("Email Body: {}",emailBody);
//            var newAttachmentPaths = ArrayUtils.add(attachmentPaths, newHtmlFile.getAbsolutePath());
            messageHelper.setText(emailBody, true);
            for (String attachmentPath : attachmentPaths) {
                if (attachmentPath != null && !attachmentPath.isEmpty()) {

                    File file = new File(attachmentPath);
                    FileSystemResource resource = new FileSystemResource(file);
                    messageHelper.addAttachment(file.getName(), resource);
                }
            }

            mailSender.send(mimeMessage);
            log.info("Email sent...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
