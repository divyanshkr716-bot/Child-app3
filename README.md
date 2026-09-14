# Game Centre — Real Parent/Child pairing

This build uses a real local-network pairing flow with a unique 8-character code.

## Pairing
1. Install/open the Parent APK and open **Add Child Device**.
2. Parent displays one unique 8-character code.
3. Install/open the Child APK on a second Android device.
4. Both devices must be on the same Wi-Fi/LAN.
5. On Child, enter only the code and tap **Find & Connect to Parent**.
6. Child discovers the Parent through Android Network Service Discovery (NSD), resolves the Parent endpoint, and sends the code plus the Child's generated authentication token.
7. Parent accepts only an exact match for the currently advertised code and registers the Child.
8. The Parent rotates the code after a successful pairing, making that code one-time.
9. Parent then verifies the Child through the authenticated Child daemon heartbeat.

No Parent IP address is entered by the user. A reachable server without a matching code is not treated as a successful pairing.

## Scope
This is LAN pairing. A globally routable code that works across different networks requires a real backend/relay service; this source does not pretend that two phones on unrelated networks can connect without one.


## Internet / 4G-5G mode
The project now contains `server/`, a real outbound WebSocket relay. Parent and Child no longer need to be on the same Wi-Fi. Deploy the server with HTTPS/WSS, then set its public URL in both `CloudConfig.kt` files. The Child makes an outbound WSS connection, so carrier NAT does not require a public Child IP. The Parent uses `/tunnel/<childId>` for the existing authenticated API surface.

Pairing is server-side: Parent receives an 8-character code, Child submits it to the cloud, and the Parent polls its paired devices.
