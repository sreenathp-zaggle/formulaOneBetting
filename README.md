# Formula 1 Betting Service

A REST API for Formula 1 betting operations including event listing, bet placement, and outcome settlement.

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Running the Application

1. **Start PostgreSQL Database**
   ```bash
   docker-compose up -d postgres
   ```

2. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

3. **Application will be available at:**
   - API Base URL: `http://localhost:8080`
   - Database: `localhost:5432` (username: `postgres`, password: `password`)

## 📋 API Endpoints

### 1. List F1 Events
```http
GET /events?year=2024&country=Monaco&sessionType=Race
```

**Query Parameters:**
- `year` (optional): Filter by year (e.g., 2024)
- `country` (optional): Filter by country (e.g., "Monaco")
- `sessionType` (optional): Filter by session type (e.g., "Race", "Qualifying")
- `provider` (optional): Data provider (default: "openf1")

**Response:**
```json
[
  {
    "eventId": "12345",
    "name": "Monaco Grand Prix - Race",
    "country": "Monaco",
    "year": 2024,
    "sessionType": "Race",
    "startTime": "2024-05-26T15:00:00Z",
    "drivers": [
      {
        "driverId": 1,
        "fullName": "Lewis Hamilton",
        "odds": 3
      }
    ]
  }
]
```

**🔍 Business Logic Algorithm:**
```
1. RECEIVE request with optional filters (year, country, sessionType)
2. CHECK database for existing events matching filters
3. IF events found in database:
   - RETURN cached events with drivers
4. ELSE:
   - CALL OpenF1 API with filters
   - FETCH session data from external API
   - FOR each session:
     - EXTRACT session details (name, country, year, etc.)
     - FETCH drivers for this session
     - ASSIGN random odds (2, 3, or 4) to each driver
     - BUILD event response DTO
   - SAVE events and drivers to database
   - RETURN event list with drivers
```

### 2. Place a Bet
```http
POST /bets
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "12345",
  "driverId": 1,
  "stake": 50.00
}
```

**Response:**
```json
{
  "betId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "PENDING",
  "odds": 3,
  "message": "placed"
}
```

**🔍 Business Logic Algorithm:**
```
1. VALIDATE input (userId, eventId, driverId, stake > 0)
2. ENSURE user exists:
   - IF user not found: CREATE user with 100 EUR gift balance
   - ELSE: USE existing user
3. VERIFY event exists:
   - CHECK event exists in database
   - IF not found: THROW error
4. VALIDATE driver for event:
   - CHECK if driver participates in this event
   - IF not found: FETCH drivers from OpenF1 API and save
   - GET driver odds
5. CHECK user balance:
   - IF insufficient funds: RETURN FAILED status
   - ELSE: WITHDRAW stake from user balance
6. CREATE bet record:
   - GENERATE unique bet ID
   - SET status to "PENDING"
   - SAVE bet to database
7. RETURN bet confirmation with odds
```

### 3. Set Event Outcome
```http
POST /events/{eventId}/outcome
Content-Type: application/json

{
  "winnerDriverId": 1
}
```

**Response:**
```json
{
  "eventId": "12345",
  "winnerDriverId": 1,
  "betsSettled": 5,
  "totalPayout": 150.00
}
```

**🔍 Business Logic Algorithm:**
```
1. VALIDATE input (eventId, winnerDriverId)
2. ATOMICALLY set event outcome:
   - IF outcome already set: THROW error
   - ELSE: SET outcome_driver_id in events table
3. FETCH all PENDING bets for this event
4. FOR each bet:
   - SET settled_at timestamp
   - IF bet.driverId == winnerDriverId:
     - SET status to "WON"
     - CALCULATE payout = stake × odds
     - CREDIT payout to user balance
     - ADD to totalPayout
   - ELSE:
     - SET status to "LOST"
   - SAVE bet to database
5. RETURN settlement summary:
   - Event ID and winner
   - Number of bets settled
   - Total payout amount
```

## 🏗️ Architecture

### Components
- **Controllers**: REST API endpoints
- **Services**: Business logic layer
- **Repositories**: Data access layer
- **Entities**: JPA entities for database mapping
- **DTOs**: Data transfer objects for API communication

### External Dependencies
- **OpenF1 API**: F1 event and driver data
- **PostgreSQL**: Primary database

## 🗄️ Database Schema

### Tables
- **users**: User accounts with balance
- **events**: F1 events/sessions
- **event_drivers**: Drivers participating in events
- **bets**: User bets placed on events

### Key Features
- Users get 100 EUR gift balance on first bet
- Odds are randomly assigned (2, 3, or 4)
- Atomic bet settlement with proper transaction handling

## 🔧 Configuration

### Application Properties
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/formulaone
    username: postgres
    password: password

openf1:
  base-url: https://api.openf1.org
  enabled: true
```

## 🧪 Testing

### Manual Testing
1. **List Events**: `GET http://localhost:8080/events`
2. **Place Bet**: `POST http://localhost:8080/bets` with JSON body
3. **Set Outcome**: `POST http://localhost:8080/events/{eventId}/outcome`

### Example cURL Commands
```bash
# List events
curl "http://localhost:8080/events?year=2024"

# Place a bet
curl -X POST http://localhost:8080/bets \
  -H "Content-Type: application/json" \
  -d '{"userId":"550e8400-e29b-41d4-a716-446655440000","eventId":"12345","driverId":1,"stake":50.00}'

# Set outcome
curl -X POST http://localhost:8080/events/12345/outcome \
  -H "Content-Type: application/json" \
  -d '{"winnerDriverId":1}'
```

## 🚨 Error Handling

The API returns consistent error responses:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Driver 123 not found for event 456",
  "path": "/api/bets"
}
```

## 📝 Business Rules & Logic

### Core Business Rules
- Users start with 100 EUR gift balance
- Odds are randomly assigned (2, 3, or 4)
- Bets can only be placed on valid drivers for the event
- Event outcomes can only be set once
- Winning bets pay out: `stake × odds`

### 🧠 Complete Business Logic Flow

#### **1. Event Discovery Flow**
```
User Request → Database Check → External API (if needed) → Response
     ↓              ↓                    ↓                    ↓
  Filters      Cache Hit?         OpenF1 API Call      Event List
     ↓              ↓                    ↓                    ↓
  year,        Return cached      Fetch sessions      With drivers
  country,     events with        and drivers         and odds
  sessionType  drivers
```

#### **2. Bet Placement Flow**
```
User Request → Validation → User Check → Event Check → Driver Check → Balance Check → Bet Creation
     ↓              ↓           ↓           ↓           ↓           ↓           ↓
  JSON Input    Required     Create if    Verify      Validate    Sufficient   Save to
  Validation   fields       not found    exists      driver      funds?       database
     ↓              ↓           ↓           ↓           ↓           ↓           ↓
  userId,       All fields   Gift 100€    Event ID    Driver ID  Withdraw    PENDING
  eventId,      present      balance     exists       for event   stake       status
  driverId,
  stake > 0
```

#### **3. Settlement Flow**
```
Outcome Request → Atomic Check → Fetch Bets → Process Each Bet → Update Balances → Response
       ↓              ↓            ↓            ↓                ↓              ↓
   winnerDriverId   Already set?  PENDING     WON/LOST        Credit users   Summary
       ↓              ↓            ↓            ↓                ↓              ↓
   Event ID      Throw error    All bets     Calculate       Winners get    Count &
   + Winner      if yes         for event    payouts         paid out       totals
```

### 🔄 State Transitions

#### **User States**
```
New User → 100 EUR Gift → Bet Placement → Balance Update
    ↓           ↓              ↓              ↓
  Created    Initial       Stake Withdrawn  Payout Added
  with ID    Balance       on bet placement  on win
```

#### **Bet States**
```
PENDING → WON (if winner) → User credited
    ↓         ↓
  Placed    LOST (if loser) → No payout
```

#### **Event States**
```
Active → Settled (outcome set) → No more bets allowed
   ↓           ↓
  Open for   Winner determined
  betting    All bets settled
```

### 💰 Financial Logic

#### **Balance Management**
```
User Balance = Initial Gift + Winnings - Stakes
     ↓              ↓           ↓         ↓
   Current     100 EUR      Payouts    Bet Amounts
   Amount      on creation   received   placed
```

#### **Payout Calculation**
```
Winning Payout = Stake × Odds
     ↓              ↓       ↓
   Amount        Bet      Driver
   Received      Amount   Odds
```

#### **Odds Assignment**
```
Random Odds = {2, 3, 4} (equal probability)
     ↓           ↓
   Assigned    Driver gets
   per driver  random value
```

### 🛡️ Validation Rules

#### **Input Validation**
- **User ID**: Must be valid UUID
- **Event ID**: Must exist in database
- **Driver ID**: Must be valid for the event
- **Stake**: Must be > 0 and ≤ user balance

#### **Business Validation**
- **Event Status**: Must not be settled
- **Driver**: Must participate in the event
- **Balance**: Must have sufficient funds
- **Outcome**: Can only be set once per event

### 🔒 Transaction Safety

#### **Atomic Operations**
- **Bet Placement**: User balance withdrawal + bet creation
- **Settlement**: Event outcome + all bet updates + user payouts
- **Rollback**: If any step fails, entire transaction rolls back

#### **Concurrency Control**
- **Balance Updates**: Database-level locking prevents double-spending
- **Outcome Setting**: Atomic check prevents duplicate settlements
- **Bet Creation**: Unique constraints prevent duplicate bets

## 🔍 Troubleshooting

### Common Issues
1. **Database Connection**: Ensure PostgreSQL is running
2. **Port Conflicts**: Default port 8080 must be available
3. **External API**: OpenF1 API must be accessible

