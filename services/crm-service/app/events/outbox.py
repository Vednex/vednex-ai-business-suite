import json
import logging
import threading

import sqlalchemy as sa

from app.core.config import get_settings
from app.db.base import SessionLocal

logger = logging.getLogger(__name__)

_POLL_INTERVAL_SECONDS = 5
_BATCH_SIZE = 25


def _publish_to_rabbitmq(payload: dict) -> None:
    settings = get_settings()
    if not settings.rabbitmq_url:
        logger.debug("RabbitMQ not configured, skipping publish")
        return

    import pika

    params = pika.URLParameters(settings.rabbitmq_url)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    channel.exchange_declare(exchange=settings.rabbitmq_exchange, exchange_type="topic", durable=True)
    channel.basic_publish(
        exchange=settings.rabbitmq_exchange,
        routing_key=payload.get("eventType", ""),
        body=json.dumps(payload, default=str),
        properties=pika.BasicProperties(
            delivery_mode=2,
            headers={
                "eventId": str(payload.get("eventId")),
                "eventType": payload.get("eventType"),
                "eventVersion": str(payload.get("eventVersion")),
                "correlationId": str(payload.get("correlationId")),
                "producer": payload.get("producer"),
            },
        ),
    )
    connection.close()


def _process_batch() -> int:
    db = SessionLocal()
    try:
        rows = db.execute(
            sa.text(
                "SELECT id, payload FROM outbox_events "
                "WHERE status = 'PENDING' AND next_retry_at <= now() "
                "ORDER BY created_at LIMIT :limit"
            ),
            {"limit": _BATCH_SIZE},
        ).fetchall()

        if not rows:
            return 0

        for row in rows:
            event_id = row[0]
            payload = row[1] if isinstance(row[1], dict) else json.loads(row[1])
            try:
                _publish_to_rabbitmq(payload)
                db.execute(
                    sa.text(
                        "UPDATE outbox_events SET status = 'PUBLISHED', published_at = now() WHERE id = :id"
                    ),
                    {"id": event_id},
                )
                logger.info("Published event %s", payload.get("eventType"))
            except Exception:
                logger.exception("Failed to publish event %s", event_id)
                db.execute(
                    sa.text(
                        "UPDATE outbox_events SET retry_count = retry_count + 1, "
                        "status = CASE WHEN retry_count + 1 >= 10 THEN 'FAILED' ELSE 'PENDING' END, "
                        "next_retry_at = now() + interval '1 second' * power(2, retry_count), "
                        "last_error = :err WHERE id = :id"
                    ),
                    {"id": event_id, "err": str(payload)[:1000]},
                )

        db.commit()
        return len(rows)
    except Exception:
        db.rollback()
        logger.exception("Outbox batch error")
        return 0
    finally:
        db.close()


def _poll_loop(stop_event: threading.Event) -> None:
    logger.info("Outbox poller started (interval=%ds)", _POLL_INTERVAL_SECONDS)
    while not stop_event.is_set():
        processed = _process_batch()
        if processed == 0:
            stop_event.wait(_POLL_INTERVAL_SECONDS)
        else:
            stop_event.wait(0.1)
    logger.info("Outbox poller stopped")


def start_outbox_poller() -> threading.Event:
    stop_event = threading.Event()
    thread = threading.Thread(target=_poll_loop, args=(stop_event,), daemon=True, name="outbox-poller")
    thread.start()
    return stop_event
