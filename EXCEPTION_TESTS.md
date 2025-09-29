# 🧪 Corrected Exception Testing Guide

This guide provides test commands for **ALL** exception types handled by your `ApiExceptionHandler` with **CORRECT** endpoints and validation.

## 📋 Exception Types to Test

1. **MethodArgumentNotValidException** (400) - Validation errors
2. **IllegalArgumentException** (400) - Bad request errors
3. **IllegalStateException** (409) - Conflict errors
4. **HttpClientException** (502) - External API failures
5. **DataIntegrityViolationException** (409) - Database constraint violations
6. **Exception** (500) - Catch-all for unexpected errors

---

## 1. 🚫 MethodArgumentNotValidException (400) - Validation Errors

### **Test Commands:**

**Missing Required Fields:**
```bash
# Missing eventId
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "driverId": 1,
    "stake": 50.00
}'

# Missing userId
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
}'

# Missing driverId
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "stake": 50.00
}'

# Missing stake
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1
}'
```

**Invalid Field Values:**
```bash
# Invalid stake (negative)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": -10.00
}'

# Invalid stake (zero)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 0.00
}'

# Invalid stake (below minimum 0.01)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 0.005
}'
```

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "eventId: Event ID is required",
  "path": "/bets/place"
}
```

---

## 2. ⚠️ IllegalArgumentException (400) - Bad Request Errors

### **Test Commands:**

**Non-existent Event:**
# Event that doesn't exist in database
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "non-existent-event-12345",
    "driverId": 1,
    "stake": 50.00
}'

**Invalid Driver for Event:**
# Driver not participating in the event
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 999,
    "stake": 50.00
}'

**Insufficient Balance:**
# Stake higher than user balance (after user has 100 EUR)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 150.00
}'

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Event not found: non-existent-event-12345",
  "path": "/bets/place"
}
```

---

## 3. 🔒 IllegalStateException (409) - Conflict Errors

### **Test Commands:**

**Outcome Already Set:**
# First, set an outcome
curl --location 'http://localhost:8080/events/12345/outcome' \
--header 'Content-Type: application/json' \
--data '{
    "winnerDriverId": 1
}'

# Then try to set it again (should fail)
curl --location 'http://localhost:8080/events/12345/outcome' \
--header 'Content-Type: application/json' \
--data '{
    "winnerDriverId": 2
}'

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "outcome already set",
  "path": "/events/12345/outcome"
}
```

## 4. 🌐 HttpClientException (502) - External API Failures

### **Test Commands:**

**OpenF1 API Rate Limiting:**
```bash
# Make multiple rapid requests to trigger rate limiting
for i in {1..10}; do
  curl --location 'http://localhost:8080/events/list?provider=openf1' &
done
wait
```

**OpenF1 API Down (if you can simulate):**
```bash
# This would require modifying the OpenF1 base URL to an invalid endpoint
curl --location 'http://localhost:8080/events/list?provider=openf1'
```

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "External API error",
  "path": "/events/list"
}
```

## 5. 🗄️ DataIntegrityViolationException (409) - Database Constraint Violations

### **Test Commands:**

**Duplicate Bet (if you have unique constraints):**
```bash
# Place the same bet twice (if there are unique constraints)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
}'

# Try to place the exact same bet again
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
}'
```

**Foreign Key Constraint Violation:**
# Try to place bet on non-existent event (if foreign key constraints are enabled)
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "invalid-event-id",
    "driverId": 1,
    "stake": 50.00
}'

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Data constraint violation",
  "path": "/bets/place"
}
```


## 6. 💥 Exception (500) - Catch-all for Unexpected Errors

### **Test Commands:**

**Malformed JSON:**
# Invalid JSON syntax
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
'

# Missing Content-Type header
curl --location 'http://localhost:8080/bets/place' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
}'
```

**Invalid UUID Format:**
# Invalid UUID format
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "invalid-uuid-format",
    "eventId": "12345",
    "driverId": 1,
    "stake": 50.00
}'

**Expected Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Internal server error",
  "path": "/bets/place"
}
```

## 7. 🏥 Health Check Endpoint

### **Test Commands:**

**Valid Health Check:**
```bash
curl --location 'http://localhost:8080/health'
```

**Expected Response:**
```json
{
  "status": "UP",
  "message": "Application is running"
}
```

## 🎯 Testing Strategy

### **Step 1: Test Valid Scenarios First**
```bash
# 1. Check health
curl --location 'http://localhost:8080/health'

# 2. List events to get valid eventId
curl --location 'http://localhost:8080/events/list'

# 3. Place a valid bet
curl --location 'http://localhost:8080/bets/place' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "VALID_EVENT_ID_FROM_STEP_2",
    "driverId": 1,
    "stake": 50.00
}'
```

## 📊 Expected Status Codes Summary

| Exception Type | HTTP Status | When It Occurs |
|----------------|-------------|-----------------|
| `MethodArgumentNotValidException` | 400 | Validation errors |
| `IllegalArgumentException` | 400 | Business logic errors |
| `IllegalStateException` | 409 | Conflict situations |
| `HttpClientException` | 502 | External API failures |
| `DataIntegrityViolationException` | 409 | Database constraints |
| `Exception` | 500 | Unexpected errors |

## 🔍 Verification Checklist

- [ ] **400 Bad Request**: Validation and business logic errors
- [ ] **409 Conflict**: Duplicate operations and constraint violations
- [ ] **502 Bad Gateway**: External API failures
- [ ] **500 Internal Server Error**: Unexpected errors
- [ ] **Consistent Error Format**: All errors return same JSON structure
- [ ] **Proper HTTP Status Codes**: Correct status for each error type
- [ ] **Error Messages**: Clear, user-friendly messages
- [ ] **Path Information**: Error responses include request path