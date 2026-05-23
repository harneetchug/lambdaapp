package com.example;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PicklistCreatedHandler implements RequestHandler<SQSEvent, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    // -----------------------------
    // CloudWatch Client
    // -----------------------------
    private final AmazonCloudWatch cloudWatch =
            AmazonCloudWatchClientBuilder.defaultClient();

    private static final String NAMESPACE = "ConversationSummaryService";

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        String requestId = context.getAwsRequestId();
        long startTime = System.currentTimeMillis();

        log.info("Lambda triggered for PICKLIST_ITEM_CREATED. requestId={}, event={}", requestId, sqsEvent);

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {

            try {
                String body = msg.getBody();
                log.info("Raw SQS message body={}", body);

                // -----------------------------
                // 1. Parse event
                // -----------------------------
                PicklistEvent event = mapper.readValue(body, PicklistEvent.class);

                log.info("Processing picklist event. reservationId={}, interactionId={}, channel={}",
                        event.reservationId, event.interactionId, event.channel);

                // -----------------------------
                // 2. Fetch conversation context
                // -----------------------------
                log.info("Fetching conversation for interactionId={}", event.interactionId);

                String conversationData = fetchConversation(event.interactionId);

                // -----------------------------
                // 3. Generate summary
                // -----------------------------
                log.info("Generating LLM-based summary for reservationId={}", event.reservationId);

                String summary = generateSummary(conversationData);

                // -----------------------------
                // 4. Persist result
                // -----------------------------
                log.info("Saving summary for reservationId={}", event.reservationId);

                saveSummary(event.reservationId, summary);

                // -----------------------------
                // 5. SUCCESS METRICS
                // -----------------------------
                publishMetric("SummarySuccess", 1.0);
                publishLatency(System.currentTimeMillis() - startTime);

                log.info("Summary generated successfully for reservationId={}", event.reservationId);

            } catch (Exception ex) {

                // -----------------------------
                // ERROR METRICS
                // -----------------------------
                publishMetric("SummaryFailure", 1.0);

                log.error("ERROR processing PICKLIST_ITEM_CREATED event", ex);

                throw new RuntimeException("Processing failed", ex);
            }
        }

        return "SUCCESS";
    }

    // =====================================================
    // BUSINESS LOGIC
    // =====================================================
    private String fetchConversation(String interactionId) {
        log.info("Fetching conversation for interactionId={}", interactionId);
        return "Customer requested refund for cancelled booking. Agent is reviewing.";
    }

    private String generateSummary(String conversationData) {
        log.info("Generating LLM-based summary");
        return "Customer requested refund due to cancellation. Agent is processing refund request.";
    }

    private void saveSummary(String reservationId, String summary) {
        log.info("Saving summary for reservationId={}", reservationId);
    }

    // =====================================================
    // CLOUDWATCH METRICS
    // =====================================================

    private void publishMetric(String metricName, double value) {

        try {
            MetricDatum datum = new MetricDatum()
                    .withMetricName(metricName)
                    .withValue(value)
                    .withUnit(StandardUnit.Count);

            PutMetricDataRequest request = new PutMetricDataRequest()
                    .withNamespace(NAMESPACE)
                    .withMetricData(datum);

            cloudWatch.putMetricData(request);

            log.info("Published CloudWatch metric: {} = {}", metricName, value);

        } catch (Exception e) {
            log.error("Failed to publish metric: {}", metricName, e);
        }
    }

    private void publishLatency(long durationMs) {

        try {
            MetricDatum datum = new MetricDatum()
                    .withMetricName("SummaryLatency")
                    .withValue((double) durationMs)
                    .withUnit(StandardUnit.Milliseconds);

            cloudWatch.putMetricData(new PutMetricDataRequest()
                    .withNamespace(NAMESPACE)
                    .withMetricData(datum));

            log.info("Published latency metric: {} ms", durationMs);

        } catch (Exception e) {
            log.error("Failed to publish latency metric", e);
        }
    }
}