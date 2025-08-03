package com.banking.transactions;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionProcessor {
    
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> processBatch(@RequestBody BatchRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<String> processedIds = new ArrayList<>();
        
        // High-throughput batch processing logic
        for (TransactionItem transaction : request.getTransactions()) {
            if (isValidTransaction(transaction)) {
                String transactionId = processTransaction(transaction);
                processedIds.add(transactionId);
            }
        }
        
        response.put("status", "COMPLETED");
        response.put("processed_count", processedIds.size());
        response.put("transaction_ids", processedIds);
        response.put("batch_id", "BATCH-" + System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Simulate high-throughput metrics
        metrics.put("transactions_per_second", 2847);
        metrics.put("avg_processing_time_ms", 23);
        metrics.put("active_threads", 45);
        metrics.put("queue_size", 156);
        metrics.put("circuit_breaker_status", "CLOSED");
        
        return ResponseEntity.ok(metrics);
    }
    
    private boolean isValidTransaction(TransactionItem transaction) {
        return transaction.getAmount() != null && 
               transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               transaction.getFromAccount() != null &&
               transaction.getToAccount() != null;
    }
    
    private String processTransaction(TransactionItem transaction) {
        // Simulate transaction processing
        return "TXN-" + System.currentTimeMillis() + "-" + Math.random();
    }
    
    public static class BatchRequest {
        private List<TransactionItem> transactions;
        
        public List<TransactionItem> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionItem> transactions) { this.transactions = transactions; }
    }
    
    public static class TransactionItem {
        private BigDecimal amount;
        private String fromAccount;
        private String toAccount;
        private String type;
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getFromAccount() { return fromAccount; }
        public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
        public String getToAccount() { return toAccount; }
        public void setToAccount(String toAccount) { this.toAccount = toAccount; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
