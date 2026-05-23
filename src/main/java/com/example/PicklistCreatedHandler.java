package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PicklistCreatedHandler implements RequestHandler<SQSEvent , String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        String requestId = context.getAwsRequestId();

        log.info("Lambda triggered for PICKLIST_ITEM_CREATED. requestId={}, event={}", requestId, sqsEvent);

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            try {
                String body = msg.getBody();
                log.info("Raw SQS message body={}", body);

                // 2. Convert JSON → POJO
                PicklistEvent event =
                        mapper.readValue(body, PicklistEvent.class);

                // -----------------------------
                // 1. Extract event data
                // -----------------------------


                log.info("Processing picklist event. reservationId={}, interactionId={}, channel={}",
                        event.reservationId, event.interactionId, event.channel);

                // -----------------------------
                // 2. Fetch conversation context
                // -----------------------------
                log.info("Fetching conversation for interactionId={}", event.interactionId);

                String conversationData = fetchConversation(event.interactionId);

                // -----------------------------
                // 3. Generate summary (LLM / API)
                // -----------------------------
                log.info("Generating LLM-based summary for reservationId={}", event.reservationId);

                String summary = generateSummary(conversationData);

                // -----------------------------
                // 4. Persist result
                // -----------------------------
                log.info("Saving summary for reservationId={}", event.reservationId);

                saveSummary(event.reservationId, summary);

                // -----------------------------
                // 5. Success logging
                // -----------------------------
                log.info("Summary generated successfully for reservationId={}", event.reservationId);

                return "SUCCESS";

            } catch (Exception ex) {

                // -----------------------------
                // ERROR HANDLING (CloudWatch Logs)
                // -----------------------------
                log.error("ERROR processing PICKLIST_ITEM_CREATED event", ex);

                throw new RuntimeException("Processing failed", ex);
            }
        }

        return "Error in processing request.";
    }

    // -----------------------------
    // Helper methods (business logic)
    // -----------------------------

    private String fetchConversation(String interactionId) {

        log.info("Fetching conversation for interactionId={}", interactionId);

        // Simulated downstream service / DB call
        return "Customer requested refund for cancelled booking. Agent is reviewing.";
    }

    private String generateSummary(String conversationData) {

        log.info("Generating LLM-based summary");

        // Simulated LLM / API response
        return "Customer requested refund due to cancellation. Agent is processing refund request.";
    }

    private void saveSummary(String reservationId, String summary) {

        log.info("Saving summary for reservationId={}", reservationId);

        // Simulated persistence (DB / S3 / API)
    }
}