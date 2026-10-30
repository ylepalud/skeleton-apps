import Security
import XCTest

@testable import Notes

final class KeychainTokenStoreTests: XCTestCase {
    private let service = "dev.ylp.notes.tests.\(UUID().uuidString)"
    private lazy var store = KeychainTokenStore(service: service)

    override func tearDown() {
        store.remove()
    }

    func testNothingIsStoredAtFirst() async throws {
        let tokens = try await store.load()
        XCTAssertNil(tokens)
    }

    func testSavedTokensAreLoadedBack() async throws {
        let expiry = Instant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1_793_000_000_000)

        try await store.save(tokens: Tokens(accessToken: "access", refreshToken: "refresh", expiresAt: expiry))
        let loaded = try await KeychainTokenStore(service: service).load()

        XCTAssertEqual(loaded?.accessToken, "access")
        XCTAssertEqual(loaded?.refreshToken, "refresh")
        XCTAssertEqual(loaded?.expiresAt?.toEpochMilliseconds(), 1_793_000_000_000)
    }

    func testTokensWithoutRefreshOrExpiryAreKept() async throws {
        try await store.save(tokens: Tokens(accessToken: "access", refreshToken: nil, expiresAt: nil))
        let loaded = try await store.load()

        XCTAssertEqual(loaded?.accessToken, "access")
        XCTAssertNil(loaded?.refreshToken)
        XCTAssertNil(loaded?.expiresAt)
    }

    func testClearedTokensAreGone() async throws {
        try await store.save(tokens: Tokens(accessToken: "access", refreshToken: nil, expiresAt: nil))

        try await store.clear()

        let tokens = try await store.load()
        XCTAssertNil(tokens)
    }

    func testUnreadableDataIsDropped() async throws {
        let item: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service,
            kSecValueData as String: Data("not json".utf8),
        ]
        XCTAssertEqual(SecItemAdd(item as CFDictionary, nil), errSecSuccess)

        let tokens = try await store.load()

        XCTAssertNil(tokens)
        XCTAssertEqual(SecItemCopyMatching(item as CFDictionary, nil), errSecItemNotFound)
    }
}
