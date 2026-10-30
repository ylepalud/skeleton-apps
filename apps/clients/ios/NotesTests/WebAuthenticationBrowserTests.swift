import XCTest

@testable import Notes

@MainActor
final class WebAuthenticationBrowserTests: XCTestCase {
    func testTheRedirectIsHandedBack() async throws {
        var opened: (URL, String)?
        let browser = WebAuthenticationBrowser { page, scheme, completion in
            opened = (page, scheme)
            completion(URL(string: "dev.ylp.notes://callback?code=abc&state=xyz"))
        }

        let redirect = try await browser.authorize(
            url: "https://idp.example/authorize?state=xyz", redirectUri: "dev.ylp.notes://callback")

        XCTAssertEqual(redirect, "dev.ylp.notes://callback?code=abc&state=xyz")
        XCTAssertEqual(opened?.0.absoluteString, "https://idp.example/authorize?state=xyz")
        XCTAssertEqual(opened?.1, "dev.ylp.notes")
    }

    func testClosingThePageCancels() async throws {
        let browser = WebAuthenticationBrowser { _, _, completion in completion(nil) }

        let redirect = try await browser.authorize(
            url: "https://idp.example/authorize", redirectUri: "dev.ylp.notes://callback")

        XCTAssertNil(redirect)
    }

    func testARedirectWithoutSchemeOpensNothing() async throws {
        let browser = WebAuthenticationBrowser { _, _, _ in XCTFail("no page should open") }

        let redirect = try await browser.authorize(url: "https://idp.example/authorize", redirectUri: "callback")

        XCTAssertNil(redirect)
    }
}
