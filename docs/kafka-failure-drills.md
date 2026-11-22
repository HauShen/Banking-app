# Kafka failure drills

Break the event-driven parts on purpose, watch what happens, and write it down. Each drill says what to do, what you
should see, and which part of the code is responsible. Fill in **What I saw** as you go; a difference between
"expected" and "saw" is the interesting part.

How the pieces fit (details in the README, section "Event-driven parts"):

```
transfer (one DB transaction) ──► outbox_events row ──► OutboxRelay ──► Kafka topic banking.transactions.v1
                                                                        ├──► group banking-notifications ──► notifications
                                                                        └──► group banking-alerts        ──► transaction_alerts
                                                       broken messages ──► banking.transactions.v1.DLT
```

## Before you start

```bash
docker compose --profile app --profile tools up -d --build
```

- App: http://localhost:3000 · Kafka UI: http://localhost:8081
- Sign up two customers, **alice** and **bob**. As `devadmin` / `DevAdmin123!`, open an account for each
  (admin → accounts → Open account). As alice, make a cash deposit of RM 1,000.

Commands used below (run them from the project folder):

| What | Command |
|---|---|
| App log | `docker compose logs -f app` (Ctrl+C stops following). Only the lines that matter: add `\| grep -E "Outbox\|Notifications\|Alerts\|Kafka topics"` (Git Bash) or `\| Select-String "Outbox\|Notifications\|Alerts\|Kafka topics"` (PowerShell) |
| SQL | `docker compose exec postgres psql -U hau -d banking_db -c "<query>"` (use your `POSTGRES_USER` / `POSTGRES_DB` if you changed them in `.env`) |
| Kafka tools | `docker compose exec kafka /opt/kafka/bin/<tool>.sh --bootstrap-server localhost:9092 …` |

Useful queries:

```sql
-- what is waiting in the outbox
SELECT id, event_type, message_key, attempts, published_at FROM outbox_events WHERE published_at IS NULL ORDER BY id;
-- counts before / after a drill
SELECT (SELECT count(*) FROM notifications) AS notifications,
       (SELECT count(*) FROM transaction_alerts) AS alerts,
       (SELECT count(*) FROM processed_events) AS processed;
```

---

## Drill 1 — The broker goes down while the app is running

**Question:** does a Kafka outage break transfers, or lose events?

1. Check that nothing is waiting: the first query above returns no rows.
2. `docker compose stop kafka`
3. As alice, send bob RM 10, twice.
4. Watch the app log for two minutes.
5. Run the outbox query again.
6. `docker compose start kafka`
7. Watch the log and the outbox query again; look at bob's bell.

**Expected**
- Both transfers succeed at once (201, balances updated). Kafka is not in the money path.
- Every few seconds: `Outbox: sending TRANSFER_COMPLETED <eventId> (row N) failed, will retry: …TimeoutException…`.
  Always the **same** row: the relay stops at the first failure so no event overtakes an earlier one. In the outbox
  query, that row's `attempts` keeps growing; the second row stays at 0.
- After about a minute, once a minute: `Outbox: 2 event(s) waiting to be sent to Kafka, the oldest since … Is the
  broker reachable?`
- Lots of `WARN … Connection to node … could not be established` from the Kafka clients (producer, both consumers,
  admin). Noise, expected.
- After `start`: within seconds both rows get `published_at`, in id order. Bob's bell shows the two new notifications
  within about 20 s (it polls).

**What it proves:** the transactional outbox. The event was saved with the money, in the same database transaction,
so it can't be lost; sending to Kafka is retried later (`OutboxRelay`, `OutboxHousekeeping`).

**What I saw:**

---

## Drill 2 — Every event is delivered again

**Question:** Kafka delivers *at least once*. What happens when consumers see the same events again?

1. Run the counts query and note the numbers.
2. Look at the consumer group: `… kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group banking-notifications`
   → `LAG` is 0 on every partition (it has read everything).
3. `docker compose stop app` (offsets can only be reset while the group has no running consumers).
4. Move both groups back to the start of the topic:
   ```bash
   docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --group banking-notifications --topic banking.transactions.v1 --reset-offsets --to-earliest --execute
   docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --group banking-alerts --topic banking.transactions.v1 --reset-offsets --to-earliest --execute
   ```
5. `--describe` again → `LAG` now equals the number of messages on each partition.
6. `docker compose start app` and watch the log.
7. Run the counts query and `--describe` again.

**Expected**
- For every event: `Notifications: event <id> was already processed, skipping the redelivery`, and for every
  transfer also `Alerts: event <id> was already processed, skipping the redelivery`.
- Counts unchanged: no duplicate notifications or alerts. `LAG` back to 0 within seconds.
- If you already did drill 3, the broken message is read again too and lands in the dead-letter topic again (two more
  copies).

**What it proves:** idempotent consumers. Each consumer writes `(consumer, event_id)` into `processed_events` in the
same transaction as its work, so a second delivery changes nothing (`RecordNotificationsService`,
`CheckTransferService`). The offsets on the broker are each group's reading position, and the two groups are
independent.

**What I saw:**

---

## Drill 3 — A broken message

**Question:** what does a message that can never be processed do to the consumers?

1. Kafka UI → Topics → `banking.transactions.v1` → **Produce Message**: key `1000000001`, value `this is not json` →
   Produce. (Or from the terminal:
   `echo "1000000001:this is not json" | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic banking.transactions.v1 --property parse.key=true --property key.separator=:`)
2. Look at the app log.
3. Kafka UI → topic `banking.transactions.v1.DLT` → Messages; open a message's headers.
4. As alice, send bob RM 5 → bob still gets his notification.

**Expected**
- Right away, an error from Spring Kafka's error handler in **each** consumer, with
  `UnprocessableMessageException: Message is not valid JSON`. No retries: retrying can't fix it.
- The DLT holds **two** copies of the message (one per consumer group), value `this is not json`, with headers such as
  `kafka_dlt-original-topic`, `kafka_dlt-original-offset`, `kafka_dlt-original-consumer-group`
  (`banking-notifications` / `banking-alerts`), `kafka_dlt-exception-fqcn`, `kafka_dlt-exception-message`.
- The next transfer is processed normally.

**What it proves:** one bad message never blocks a partition, and nothing is dropped silently: it is parked with the
reason (`KafkaConsumerConfig`, `EventEnvelope`, `UnprocessableMessageException`). Other failures (e.g. the database)
are tried 3 times, 1 s apart, before they are dead-lettered.

**What I saw:**

---

## Drill 4 — A brand-new broker starts after the app

**Question:** what if the app starts first and the broker later, and the broker is new (a new environment, or its
disk was lost)? With the broker's auto-create off, somebody has to create the topics.

**Background:** Spring Kafka's `KafkaAdmin` creates the topics once, at startup, and only logs "Could not configure
topics" if the broker isn't there. Before the fix, the topics would never be created, and the relay would fail with
`Topic banking.transactions.v1 not present in metadata after 5000 ms` until the app was restarted. Found while
planning this drill; fixed by `KafkaTopicsInitializer` (tries again every 30 s until the topics exist).

1. `docker compose stop app kafka`
2. Remove the broker and its data (**deletes all Kafka messages and consumer offsets**; the database is untouched):
   ```bash
   docker compose rm -f kafka
   docker volume ls                       # find the one ending in _kafkadata, e.g. banking-app_kafkadata
   docker volume rm banking-app_kafkadata
   ```
3. Start only the app: `docker compose up -d --no-deps app` (without `--no-deps`, Compose would start Kafka first).
4. Log: after about 10 s `Could not configure topics` (from `KafkaAdmin`); the app is up (http://localhost:3000 works).
5. As alice, send bob RM 5 → 201. The relay starts warning (drill 1).
6. Every 30 s: `Kafka topics are not in place yet, will try again: …`
7. Start a new broker: `docker compose up -d kafka`
8. Watch the log and Kafka UI.

**Expected**
- Within 30 s of the broker being up: `Kafka topics are in place`. Kafka UI shows `banking.transactions.v1` and
  `banking.transactions.v1.DLT`, 3 partitions each.
- The relay sends the waiting event; bob gets his notification. The consumers may need a little longer than the
  relay: they find the new topic at their next metadata refresh.

**What it proves:** the app copes with infrastructure that comes up in any order.

**What I saw:**

---

## Results

| Date | Drill | As expected? | Notes |
|---|---|---|---|
| | 1 — broker down | | |
| | 2 — redelivery | | |
| | 3 — broken message | | |
| | 4 — late, new broker | | |

## Afterwards

`docker compose --profile app --profile tools down` stops everything and keeps the data. Add `-v` to delete the
volumes too (database and Kafka).
