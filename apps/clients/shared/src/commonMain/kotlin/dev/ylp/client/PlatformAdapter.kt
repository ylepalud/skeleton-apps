package dev.ylp.client

/**
 * The base of a port implementation written in Swift. Swift export lets a Swift class implement
 * a Kotlin interface only when it also inherits from an exported Kotlin class: this one.
 *
 * ```swift
 * final class KeychainTokenStore: PlatformAdapter, TokenStore { ... }
 * ```
 */
open class PlatformAdapter
