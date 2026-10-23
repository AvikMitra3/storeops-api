# storeops-api

## SLA alerting configuration
HIGH and CRITICAL tasks with a `dueDate` that are not DONE raise an `SLA_BREACH` notification for the assigned
Department Lead, and an `ESCALATION` notification for the store's STORE_MANAGER after a grace period.
Keys in `application.yml`:

| Key | Default | Meaning |
|---|---|---|
| `storeops.sla.grace-period` | `PT30M` | Time after `dueDate` before escalating to the store manager |
| `storeops.sla.scan-interval` | `PT1M` | How often the SLA scan runs |
| `storeops.sla.scan-enabled` | `true` | Set `false` to disable the scheduled scan (used by tests) |
