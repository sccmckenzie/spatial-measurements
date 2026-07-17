"""FastAPI app: serves the page, streams metrics via SSE."""

import asyncio
import json
from contextlib import asynccontextmanager
from pathlib import Path

import asyncpg
from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from sse_starlette.sse import EventSourceResponse


STATIC_DIR = Path(__file__).parent / "static"
POLL_INTERVAL_S = 0.1
SCAN_LIMIT = 10
DATABASE_URL = 'postgresql://scanner_ro:applesauce@localhost:5432/postgres'

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.pool = await asyncpg.create_pool(DATABASE_URL)
    try:
        yield
    finally:
        await app.state.pool.close()


app = FastAPI(lifespan=lifespan)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")
app.mount("/vendor", StaticFiles(directory=Path(__file__).parent.parent / "vendor"), name="vendor")


@app.get("/")
async def index():
    return FileResponse(STATIC_DIR / "index.html")


@app.get("/stream")
async def stream():
    async def event_source():
        # TODO(v2): replace polling with Postgres LISTEN/NOTIFY on metrics inserts
        while True:
            async with app.state.pool.acquire() as conn:
                rows = await conn.fetch(
                    "SELECT scan_id, x, y, measurement_value, modified_at "
                    "FROM raw.measurement "
                    "WHERE scan_id IN ("
                    "  SELECT scan_id FROM raw.measurement "
                    "  GROUP BY scan_id ORDER BY scan_id ASC LIMIT $1"
                    ") "
                    "ORDER BY scan_id, modified_at",
                    SCAN_LIMIT,
                )
            payload = {
                "data": [
                    {
                        "scan_id": r["scan_id"],
                        "x": r["x"],
                        "y": r["y"],
                        "measurement_value": r["measurement_value"],
                        "modified_at": r["modified_at"].isoformat(),
                    }
                    for r in rows
                ]
            }
            yield {"data": json.dumps(payload)}
            await asyncio.sleep(POLL_INTERVAL_S)

    return EventSourceResponse(event_source())