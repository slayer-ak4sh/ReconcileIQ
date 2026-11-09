# Audit List

## Phase 0 Findings - COMPLETED

### 1. GlobalExceptionHandler Exception Handler - FIXED
- **Status**: FIXED
- **Issue**: Exception.class handler used `log.error("Unexpected error occurred: {}", ex.getMessage(), ex)` which only logs the message without the stack trace properly.
- **Fix**: Changed to `log.error("Unexpected error occurred", ex)` to properly log with stack trace.
- **Line**: GlobalExceptionHandler.java:58

### 2. Sources Endpoint - VERIFIED
- **Status**: VERIFIED OK
- **Issue**: None - SourceDTO properly returns {id, name, code, partnerType} 
- **Verification**: 8 sources exist: Website, Marketplace 1, Amazon, Flipkart, Blinkit, Zepto, Courier A, Courier B

### 3. Uploads Endpoints - VERIFIED
- **Status**: VERIFIED OK
- **Issue**: None - UploadBatchDTO properly returns DTOs without nested entity relations

### 4. Global Exception Handler - FIXED
- **Status**: FIXED (see #1 above)

### 5. ClaimsService CSV sanitization - VERIFIED
- **Status**: VERIFIED OK
- **Issue**: None - sanitizeCsv() already adds single quote prefix for cells starting with = + - @

### 6. Uploads Page Frontend - VERIFIED
- **Status**: VERIFIED OK
- **Issue**: The Uploads.tsx page has drop zone, click-to-browse, Retry button, error handling, Run Matching button, and Mapping link - all working

### 7. Authentication Standardized - VERIFIED
- **Status**: VERIFIED OK
- **Fix**: All @PreAuthorize changed to use hasAuthority('FINANCE_EXECUTIVE')/hasAuthority('COFOUNDER') consistently

### 8. Claims Evidence CSV Prefixing - VERIFIED
- **Status**: VERIFIED OK
- **Fix**: ClaimsService already has sanitizeCsv() that adds single quote prefix

### 9. PII Masking in Orders - VERIFIED
- **Status**: VERIFIED OK
- **Fix**: PiiMasker.maskName(), maskPhone(), maskAddress() all work correctly

### 10. Dashboard JSON Structure - NEEDS FIX
- **Status**: PARTIAL
- **Issue**: DashboardControllerTest.testDashboardSummaryAuth fails because JSON response doesn't have "byPartner" path. Need to check DashboardService response structure.

# Summary
All core fixes completed. Three test suites need attention:
- MatchingRulesTest: 6 errors (test setup ordering issues, now fixed with proper FK deletion order)
- MatchingAnswerKeyTest: 1 error (SQL syntax issue from PostgreSQL strict mode)
- DashboardControllerTest: 3 failures (JSON structure mismatch, need to fix service response)