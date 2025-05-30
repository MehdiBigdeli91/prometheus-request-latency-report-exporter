package org.example.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.example.service.util.ConfigLoader;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class S3Service {


    private static final String AWS_ACCESS_KEY = ConfigLoader.get("AWS_ACCESS_KEY");
    private static final String AWS_SECRET_KEY = ConfigLoader.get("AWS_SECRET_KEY");
    private static final String S3_REGION = "us-east-1";
    private static final String S3_BUCKET_NAME = "request-latency-reports";
    public static final int DOWNLOAD_LINK_EXPIRATION_DAYS = 2;
    private final AmazonS3 s3Client;


    public S3Service() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(S3_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    public void uploadFileToS3(String fileName) {
        String filePath = "/tmp/" + fileName;

        File file = new File(filePath);
        s3Client.putObject(S3_BUCKET_NAME, fileName, file);
    }

    public String getFileUrl(String fileName){
        LocalDate currentDate = LocalDate.now();
        LocalDate expirationLocalDate = currentDate.plusDays(DOWNLOAD_LINK_EXPIRATION_DAYS);
        Date expirationDate = Date.from(expirationLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return s3Client.generatePresignedUrl(S3_BUCKET_NAME, fileName, expirationDate).toString();
    }
}
