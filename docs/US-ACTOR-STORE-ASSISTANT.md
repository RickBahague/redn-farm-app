# User Stories — Store Assistant

The store assistant takes customer orders, maintains the customer list, and records remittances. Prices are **never entered manually** — they are pre-filled from SRPs computed by management presets and acquisitions.

See [USER_STORIES.md](./USER_STORIES.md) for full acceptance criteria.

## Stories owned by this actor

| ID | Story | Status |
|----|-------|--------|
| AUTH-US-01 | Login to the app | ✅ |
| AUTH-US-02 | Log out | ✅ |
| AUTH-US-03 | Persist session across app restarts | ✅ |
| AUTH-US-05 | Change own password | 📋 |
| SYS-US-03 | About screen | ✅ |
| ORD-US-01 | Take a new customer order (channel-based SRP pre-fill) | ✅/📋 |
| ORD-US-02 | Apply per-kg or per-piece SRP based on product unit type | ✅/📋 |
| ORD-US-03 | View order history | ✅ |
| ORD-US-04 | Edit an unpaid order | ✅/📋 |
| ORD-US-05 | Mark an order as paid | ✅ |
| ORD-US-06 | Mark an order as delivered | ✅ |
| ORD-US-07 | Delete an unpaid order | ✅ |
| ORD-US-08 | View active SRPs before taking an order | 📋 |
| ORD-US-09 | Print an order | ✅ |
| ORD-US-10 | View order detail | 📋 |
| CUS-US-01 | View all customers | ✅ |
| CUS-US-02 | Add a new customer | ✅ |
| CUS-US-03 | Edit a customer | ✅ |
| CUS-US-04 | Delete a customer | ✅ |
| CUS-US-05 | Customer type maps to a default sales channel | 📋 |
| INV-US-06 | Active SRP is always the most recent acquisition | 📋 |
| REM-US-01 | Record a remittance | ✅ |
| REM-US-02 | View remittance history | ✅ |
| REM-US-03 | Edit or delete a remittance | ✅ *(remittance rows only once **DISB-US-01–03** ship)* |
| DISB-US-02 | View remittances and disbursements on one screen | ✅ |

## Pricing boundary

The store assistant is a **consumer** of pricing, not a producer. The pricing chain is:

```
Admin defines presets (MGT-US-01–03)
  ↓
Purchasing assistant records acquisition (INV-US-01)
  ↓
SRPs auto-computed per channel (INV-US-05)
  ↓
Store assistant selects channel → SRP pre-filled on order (ORD-US-01)
```

The store assistant cannot edit SRP values, preset parameters, or channel markups.
