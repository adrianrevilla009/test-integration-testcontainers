package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Orders service AWS touchpoints (S3 invoices, SQS events) against LocalStack, never a real account. */
@Testcontainers
class OrdersAwsTest {

    @Container
    static final LocalStackContainer AWS =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
                    .withServices(Service.S3, Service.SQS);

    // LocalStack accepts any credentials; these are dummies from the container, not real secrets.
    static final StaticCredentialsProvider CREDS =
            StaticCredentialsProvider.create(AwsBasicCredentials.create(AWS.getAccessKey(), AWS.getSecretKey()));

    @Test
    void invoiceRoundTripThroughS3() {
        try (var s3 = S3Client.builder().endpointOverride(AWS.getEndpointOverride(Service.S3))
                .credentialsProvider(CREDS).region(Region.of(AWS.getRegion())).forcePathStyle(true).build()) {
            s3.createBucket(b -> b.bucket("invoices"));
            s3.putObject(b -> b.bucket("invoices").key("order-1.txt"), RequestBody.fromString("total=42"));
            var body = s3.getObjectAsBytes(b -> b.bucket("invoices").key("order-1.txt")).asUtf8String();
            assertEquals("total=42", body);
        }
    }

    @Test
    void orderEventThroughSqs() {
        try (var sqs = SqsClient.builder().endpointOverride(AWS.getEndpointOverride(Service.SQS))
                .credentialsProvider(CREDS).region(Region.of(AWS.getRegion())).build()) {
            var url = sqs.createQueue(b -> b.queueName("order-events")).queueUrl();
            sqs.sendMessage(b -> b.queueUrl(url).messageBody("order-1 CREATED"));
            var msgs = sqs.receiveMessage(b -> b.queueUrl(url).waitTimeSeconds(5).maxNumberOfMessages(1)).messages();
            assertEquals("order-1 CREATED", msgs.get(0).body());
        }
    }
}
