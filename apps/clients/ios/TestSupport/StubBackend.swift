import Foundation
import Network

/// The API and the identity provider on one local HTTP server, with notes kept in memory. It
/// answers as the real API does: pages, 404 for an unknown note, 401 for an unknown token.
nonisolated final class StubBackend: @unchecked Sendable {
    struct StoredNote: Equatable {
        var title: String
        var content: String
    }

    private let listener: NWListener
    private let queue = DispatchQueue(label: "dev.ylp.notes.stub")
    private let lock = NSLock()
    private var ids: [String] = []
    private var stored: [String: StoredNote] = [:]
    private var received: [String] = []
    private var validToken = "valid-token"
    private var refuseRefresh = false
    private var failure: Int?

    init() throws {
        let parameters = NWParameters.tcp
        parameters.requiredLocalEndpoint = .hostPort(host: .ipv4(.loopback), port: .any)
        listener = try NWListener(using: parameters)
    }

    /// Starts listening on a free port of 127.0.0.1.
    func start() throws {
        let ready = DispatchSemaphore(value: 0)
        listener.stateUpdateHandler = { state in
            if case .ready = state { ready.signal() }
        }
        listener.newConnectionHandler = { [weak self] in self?.serve($0) }
        listener.start(queue: queue)
        if ready.wait(timeout: .now() + 5) == .timedOut { throw URLError(.cannotConnectToHost) }
    }

    func stop() { listener.cancel() }

    func url(_ path: String) -> String { "http://127.0.0.1:\(listener.port?.rawValue ?? 0)\(path)" }

    // MARK: State the tests set and read

    @discardableResult
    func add(_ title: String, content: String = "") -> String {
        withLock {
            let id = UUID().uuidString.lowercased()
            ids.append(id)
            stored[id] = StoredNote(title: title, content: content)
            return id
        }
    }

    func remove(_ id: String) {
        withLock {
            ids.removeAll { $0 == id }
            stored[id] = nil
        }
    }

    var notes: [StoredNote] { withLock { ids.compactMap { stored[$0] } } }

    var requests: [String] { withLock { received } }

    /// Every token issued so far stops working, and the token endpoint refuses to refresh it.
    func endSessions() {
        withLock {
            validToken = "valid-\(UUID().uuidString)"
            refuseRefresh = true
        }
    }

    /// The API answers this status once, then behaves again.
    func failNext(with status: Int) { withLock { failure = status } }

    // MARK: HTTP

    private struct Request {
        let method: String
        let path: String
        let query: [String: String]
        let headers: [String: String]
        let body: Data

        /// Nil until the whole request has arrived.
        init?(_ data: Data) {
            guard let end = data.range(of: Data("\r\n\r\n".utf8)),
                let head = String(data: data[..<end.lowerBound], encoding: .utf8)
            else { return nil }
            var lines = head.components(separatedBy: "\r\n")
            let start = lines.removeFirst().split(separator: " ")
            guard start.count >= 2 else { return nil }
            var headers: [String: String] = [:]
            for line in lines {
                guard let colon = line.firstIndex(of: ":") else { continue }
                headers[line[..<colon].lowercased()] = line[line.index(after: colon)...]
                    .trimmingCharacters(in: .whitespaces)
            }
            let body = data[end.upperBound...]
            guard body.count >= Int(headers["content-length"] ?? "0") ?? 0 else { return nil }
            let target = URLComponents(string: String(start[1]))
            self.method = String(start[0])
            self.path = target?.path ?? ""
            self.query = Dictionary(
                (target?.queryItems ?? []).map { ($0.name, $0.value ?? "") }, uniquingKeysWith: { $1 })
            self.headers = headers
            self.body = Data(body)
        }

        var json: [String: Any] {
            (try? JSONSerialization.jsonObject(with: body)) as? [String: Any] ?? [:]
        }
    }

    private struct Response {
        let status: Int
        var body: Any?

        var bytes: Data {
            let payload = body.flatMap { try? JSONSerialization.data(withJSONObject: $0) } ?? Data()
            let type = status >= 400 ? "application/problem+json" : "application/json"
            var head = "HTTP/1.1 \(status) Stub\r\nContent-Length: \(payload.count)\r\nConnection: close\r\n"
            if !payload.isEmpty { head += "Content-Type: \(type)\r\n" }
            return Data((head + "\r\n").utf8) + payload
        }
    }

    private func serve(_ connection: NWConnection) {
        connection.start(queue: queue)
        receive(connection, buffer: Data())
    }

    private func receive(_ connection: NWConnection, buffer: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65_536) { [weak self] data, _, complete, error in
            guard let self else { return }
            let buffer = buffer + (data ?? Data())
            if let request = Request(buffer) {
                let response = withLock { respond(to: request) }
                connection.send(content: response.bytes, completion: .contentProcessed { _ in connection.cancel() })
            } else if complete || error != nil {
                connection.cancel()
            } else {
                receive(connection, buffer: buffer)
            }
        }
    }

    private func respond(to request: Request) -> Response {
        received.append("\(request.method) \(request.path)")
        switch request.path {
        case "/idp/.well-known/openid-configuration":
            return Response(
                status: 200,
                body: ["authorization_endpoint": url("/idp/authorize"), "token_endpoint": url("/idp/token")])
        case "/idp/token":
            let refreshing = String(data: request.body, encoding: .utf8)?.contains("grant_type=refresh_token") ?? false
            if refreshing && refuseRefresh { return Response(status: 400, body: ["error": "invalid_grant"]) }
            return Response(
                status: 200, body: ["access_token": validToken, "refresh_token": "refresh", "expires_in": 300])
        default:
            break
        }
        if request.headers["authorization"] != "Bearer \(validToken)" { return Response(status: 401) }
        if let status = failure {
            failure = nil
            return Response(status: status)
        }
        return api(request, path: String(request.path.dropFirst("/api/v1".count)))
    }

    private func api(_ request: Request, path: String) -> Response {
        if path == "/notes" && request.method == "GET" {
            let page = Int(request.query["page"] ?? "") ?? 0
            let size = Int(request.query["size"] ?? "") ?? 20
            let items = ids.dropFirst(page * size).prefix(size).map(note)
            return Response(
                status: 200,
                body: [
                    "items": items, "page": page, "size": size, "totalItems": ids.count,
                    "hasNext": (page + 1) * size < ids.count,
                ])
        }
        if path == "/notes" && request.method == "POST" {
            let id = UUID().uuidString.lowercased()
            ids.append(id)
            stored[id] = StoredNote(
                title: request.json["title"] as? String ?? "", content: request.json["content"] as? String ?? "")
            return Response(status: 201, body: note(id))
        }
        let id = String(path.dropFirst("/notes/".count))
        guard path.hasPrefix("/notes/"), stored[id] != nil else {
            return Response(status: 404, body: ["type": "about:blank", "title": "Not Found", "status": 404])
        }
        switch request.method {
        case "GET":
            return Response(status: 200, body: note(id))
        case "PUT":
            stored[id] = StoredNote(
                title: request.json["title"] as? String ?? "", content: request.json["content"] as? String ?? "")
            return Response(status: 200, body: note(id))
        case "DELETE":
            ids.removeAll { $0 == id }
            stored[id] = nil
            return Response(status: 204)
        default:
            return Response(status: 405)
        }
    }

    private func note(_ id: String) -> [String: Any] {
        let note = stored[id] ?? StoredNote(title: "", content: "")
        return [
            "id": id, "title": note.title, "content": note.content,
            "createdAt": "2026-10-29T08:00:00Z", "updatedAt": "2026-10-29T09:00:00Z",
        ]
    }

    private func withLock<T>(_ body: () -> T) -> T {
        lock.lock()
        defer { lock.unlock() }
        return body()
    }
}
