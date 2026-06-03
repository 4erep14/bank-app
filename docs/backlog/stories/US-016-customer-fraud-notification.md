## US-016: Customer Notification on Blocked Transaction

**Epic:** EPIC-04
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]` `[Messaging]`

**As a** customer,
**I want to** be notified when one of my transactions is blocked due to fraud detection,
**So that** I am immediately aware and can contact support if the block was in error.

**Acceptance Criteria:**
- [ ] AC1: When a transaction is blocked (US-015), a notification event is published to the Notification Service
- [ ] AC2: A `Notification` record is persisted for the affected customer containing: `customerId`, `type = TRANSACTION_BLOCKED`, `transactionId`, `amount`, `timestamp`, `triggeredRuleName`, `status = SENT`
- [ ] AC3: Authenticated customer can call `GET /api/v1/notifications` to retrieve their paginated notifications; default page size is 20
- [ ] AC4: Each notification record in the response includes: `id`, `type`, `transactionId`, `amount`, `timestamp`, `triggeredRuleName`, `status`
- [ ] AC5: A customer can only retrieve their own notifications; requests scoped to another customer's data return 403
- [ ] AC6: Unauthenticated requests to `/api/v1/notifications` return 401

**Out of Scope:** Real SMS/email delivery (stubbed/logged only), push notifications, notification preferences, notification read/unread status

**Dependencies:** US-015

**Notes / Open Questions:**
- Assumption: Actual SMS/email delivery is out of scope; the Notification Service will log the notification to console/DB only.
