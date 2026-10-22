plugins {
    base
}

// The convention plugins are a product of this repository: their functional
// tests run with every root `check`.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
