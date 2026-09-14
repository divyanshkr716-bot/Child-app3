# Game Centre — Supabase Internet Relay

This server stores Parent/Child pairing data in **Supabase Postgres** instead of a local `data.json` file.
The Child maintains an outbound WebSocket to this relay, so Parent and Child can be on different 4G/5G networks and can be thousands of kilometres apart.

## Flow

Parent 4G/5G → HTTPS → Game Centre relay → Supabase Postgres (pairing/device state)
Child 4G/5G → WSS → Game Centre relay ← HTTPS tunnel ← Parent

The database is for identity/pairing/state; the realtime command path is WebSocket/HTTPS.

## Setup

1. Create a Supabase project.
2. Open Supabase SQL Editor and run `supabase.sql`.
3. Set server environment variables from `.env.example`:
   - `SUPABASE_URL`
   - `SUPABASE_SERVICE_ROLE_KEY` (server only; never put this key in an APK)
4. Run `npm install` then `npm start` on a public Node 20+ host.
5. Put the public HTTPS/WSS URL into both Android `CloudConfig.kt` files.

For production, terminate TLS at the hosting provider/reverse proxy so the Android apps use `https://` and `wss://`.
