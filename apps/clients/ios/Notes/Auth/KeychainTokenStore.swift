import Foundation
import Security
import Shared
import os

/// Keeps the tokens between launches in one Keychain item, readable on this device only, once it
/// was first unlocked. Data it cannot read is dropped: the user signs in again.
nonisolated final class KeychainTokenStore: PlatformAdapter, TokenStore, @unchecked Sendable {
    private let service: String
    private static let logger = Logger(subsystem: "dev.ylp.notes", category: "session")

    init(service: String = "dev.ylp.notes.session") {
        self.service = service
        super.init()
    }

    private struct Stored: Codable {
        let accessToken: String
        let refreshToken: String?
        let expiresAt: Int64?
    }

    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service]
    }

    func load() async throws -> Tokens? {
        var item: CFTypeRef?
        let search = query.merging([
            kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne,
        ]) { $1 }
        guard SecItemCopyMatching(search as CFDictionary, &item) == errSecSuccess, let data = item as? Data
        else { return nil }
        guard let stored = try? JSONDecoder().decode(Stored.self, from: data) else {
            remove()
            return nil
        }
        return Tokens(
            accessToken: stored.accessToken,
            refreshToken: stored.refreshToken,
            expiresAt: stored.expiresAt.map { Instant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: $0) }
        )
    }

    func save(tokens: Tokens) async throws {
        let stored = Stored(
            accessToken: tokens.accessToken,
            refreshToken: tokens.refreshToken,
            expiresAt: tokens.expiresAt?.toEpochMilliseconds()
        )
        remove()
        let item = query.merging([
            kSecValueData as String: try JSONEncoder().encode(stored),
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]) { $1 }
        let status = SecItemAdd(item as CFDictionary, nil)
        if status != errSecSuccess {
            // The session lasts until the app stops: the next launch asks to sign in again.
            Self.logger.error("The session could not be kept: \(status)")
        }
    }

    func clear() async throws {
        remove()
    }

    /// Forgets the session at once, without waiting.
    func remove() {
        SecItemDelete(query as CFDictionary)
    }
}
