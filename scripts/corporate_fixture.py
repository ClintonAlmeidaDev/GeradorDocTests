#!/usr/bin/env python3
"""Loopback-only corporate API fixture. No real identities, credentials or external calls."""
import argparse
import http.server
import json

CLIENT_SECRET = 'FICTIONAL_CLIENT_SECRET_7934'
TOKEN = 'FICTIONAL_ACCESS_TOKEN_7934'
ORDER_ID = 'ORDER-FICTITIOUS-001'


class CorporateHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def reply(self, status, body):
        data = json.dumps(body).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        try:
            body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', '0'))))
        except (ValueError, json.JSONDecodeError):
            return self.reply(400, {'code': 'INVALID_JSON'})
        if self.path == '/oauth/token':
            if body.get('clientId') != 'fictional-audit-client' or body.get('clientSecret') != CLIENT_SECRET:
                return self.reply(401, {'code': 'INVALID_CLIENT'})
            return self.reply(200, {'accessToken': TOKEN, 'tokenType': 'Bearer', 'expiresIn': 300})
        if self.headers.get('Authorization') != 'Bearer ' + TOKEN:
            return self.reply(401, {'code': 'UNAUTHORIZED'})
        if self.path != '/orders':
            return self.reply(404, {'code': 'NOT_FOUND'})
        if not isinstance(body.get('amount'), (float, int)) or body['amount'] <= 0:
            return self.reply(422, {'code': 'INVALID_AMOUNT'})
        if not self.headers.get('X-Correlation-ID') or not self.headers.get('Idempotency-Key'):
            return self.reply(400, {'code': 'MISSING_TRACE_HEADERS'})
        return self.reply(201, {'id': ORDER_ID, 'status': 'APPROVED', **body})

    def do_GET(self):
        if self.headers.get('Authorization') != 'Bearer ' + TOKEN:
            return self.reply(401, {'code': 'UNAUTHORIZED'})
        if self.path != '/orders/' + ORDER_ID:
            return self.reply(404, {'code': 'NOT_FOUND'})
        return self.reply(200, {'id': ORDER_ID, 'status': 'APPROVED', 'amount': 125.50, 'currency': 'BRL'})


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=8765)
    args = parser.parse_args()
    server = http.server.ThreadingHTTPServer(('127.0.0.1', args.port), CorporateHandler)
    print(f'Fictional corporate fixture: http://127.0.0.1:{server.server_port}', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
