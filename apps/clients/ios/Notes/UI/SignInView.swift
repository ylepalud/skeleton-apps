import Shared
import SwiftUI

@Observable
final class SignInModel {
    private(set) var busy = false
    private(set) var message: String?
    private(set) var signedIn = false
    private let client: SharedClient

    init(client: SharedClient) {
        self.client = client
    }

    func signIn() async {
        busy = true
        message = nil
        defer { busy = false }
        do {
            switch try await client.signIn.execute().sealedType() {
            case .signedIn: signedIn = true
            case .cancelled: message = "Sign-in cancelled."
            case .failed(let failed): message = "Sign-in failed: \(failed.value.message)"
            }
        } catch {
            message = "Sign-in failed: \(error.localizedDescription)"
        }
    }
}

struct SignInView: View {
    @State private var model: SignInModel
    private let onSignedIn: () -> Void

    init(client: SharedClient, onSignedIn: @escaping () -> Void) {
        _model = State(initialValue: SignInModel(client: client))
        self.onSignedIn = onSignedIn
    }

    var body: some View {
        VStack(spacing: 16) {
            Text("Notes").font(.largeTitle)
            Button("Sign in") { Task { await model.signIn() } }
                .buttonStyle(.borderedProminent)
                .disabled(model.busy)
            if let message = model.message {
                Text(message).foregroundStyle(.red)
            }
        }
        .padding(24)
        .onChange(of: model.signedIn) { if model.signedIn { onSignedIn() } }
    }
}
