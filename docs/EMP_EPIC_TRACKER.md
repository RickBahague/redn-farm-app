# Employee epic (EMP-US-*) — implementation tracker

**Canon:** [`USER_STORIES.md`](./USER_STORIES.md) Epic 5 (Employees & Payroll).
**Maintenance:** For cross-doc status sync, use the **Canonical status table** in [`PARTIAL_IMPLEMENTATION_PLAN.md`](./PARTIAL_IMPLEMENTATION_PLAN.md).

**Single net pay definition (EMP-US-05 + EMP-US-06 rows):**

```
net_pay = amount + cash_advance_amount
```

Treat null `cash_advance_amount` as `0`. **`liquidated_amount` is not included** in net pay (recording + outstanding balance only). Use **`EmployeePayment.netPayAmount()`** in app code; history and form stay aligned.

---

## EMP-US-05 — Record an employee payment

| Acceptance | Status | Notes |
|------------|--------|--------|
| AC1 Gross wage + date paid required; received optional | `[x]` | `PaymentFormScreen`; gross > 0; snackbars on invalid save |
| AC2 Optional cash advance | `[x]` | |
| AC3 Optional liquidated | `[x]` | |
| AC4 Live net pay summary | `[x]` | Gross + advance; liquidated labeled recording-only; negative-net warning |
| AC5 Signature draw / type | `[x]` | `SignatureCanvasField` 200dp |
| AC6 Linked to employee | `[x]` | `employee_id`, route args |
| Full-screen form | `[x]` | `employee_payment_form/...`, `paymentId = -1` new |

**Tests:** `EmployeePaymentNetPayTest` (`netPayAmount()` / BUG-EMP-01).

---

## EMP-US-06 — View employee payment history

| Acceptance | Status | Notes |
|------------|--------|--------|
| AC1 Per-payment fields + net pay = EMP-US-05 formula | `[x]` | `PaymentCard` → `netPayAmount()` |
| AC2a Filter: All / Today / Week / Month | `[x]` | `DateFilterPeriod` by `date_paid` |
| AC2b Filter: last month, 3/6 months, custom range | `[x]` | `EmployeePaymentScreen` includes Last Month, Last 3 Months, Last 6 Months, and Custom Range (start/end pickers) |
| AC3 Period summary card (sums for filtered rows) | `[x]` | `EmployeePaymentScreen` — gross / advances / liquidated for filtered rows |
| AC4 Lifetime outstanding advance (always visible) | `[x]` | `EmployeePaymentScreen` — all-time sums, ignores filter |

**Primary files:** `EmployeePaymentScreen.kt`, `PaymentCard.kt`, `EmployeePaymentViewModel.kt`, `EmployeePaymentAggregates.kt`.

**Tests:** `EmployeePaymentAggregatesTest` (period totals + lifetime outstanding).

**Story status in USER_STORIES.md:** ✅ (AC2b shipped).

---

## EMP-US-07 — Edit or delete an employee payment

| Acceptance | Status | Notes |
|------------|--------|--------|
| AC1 All fields updatable | `[x]` | `PaymentFormScreen` edit route |
| AC2 Confirm before delete | `[x]` | `AlertDialog` on list |

---

*Last updated: 2026-04-09 — EMP-US-06 AC2b shipped (extra presets + custom range).*
