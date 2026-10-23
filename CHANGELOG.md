# Changelog

## 0.1.0 (2026-10-23)


### Features

* **api:** accept authorization code tokens from the local provider ([75d6a64](https://github.com/ylepalud/skeletton-apps/commit/75d6a64bc9fc2ac76d0e759dfb1ad48fd0140e0a))
* **api:** add notes openapi contract ([f1768e8](https://github.com/ylepalud/skeletton-apps/commit/f1768e8bd95b64a2355aeea9c2b797577758757e))
* **api:** map business errors to problem details ([1077b43](https://github.com/ylepalud/skeletton-apps/commit/1077b43f9bf3ad9eacd1e88ce4f16a349c696b13))
* **api:** secure the api with jwt and expose actuator on a management port ([233d5c5](https://github.com/ylepalud/skeletton-apps/commit/233d5c5221b72e83b621ae1db4cd3c8cc4d1e7b5))
* **bruno:** add the api collection generated from the contract ([a2d8c74](https://github.com/ylepalud/skeletton-apps/commit/a2d8c74326ce5b73df43b97d0b5a4de4c594cda8))
* **notes:** add notes domain and use cases ([cf06de3](https://github.com/ylepalud/skeletton-apps/commit/cf06de37d37e44f22973f9a72243648405493282))
* **notes:** expose notes over rest ([3f3529b](https://github.com/ylepalud/skeletton-apps/commit/3f3529b21055d046cbbcaf14cfeb7c6531cfa79d))
* **notes:** persist notes in postgres ([a2b83f8](https://github.com/ylepalud/skeletton-apps/commit/a2b83f8802607d106760d1b74d15b0fdf3a4bdeb))
* **web:** add the angular app to manage notes with oidc sign-in ([d4f24f9](https://github.com/ylepalud/skeletton-apps/commit/d4f24f9c849e1d3dbba458fbb4f7cf6bc9703a6b))


### Bug Fixes

* **build:** resolve docker from the caller's PATH ([2eed14d](https://github.com/ylepalud/skeletton-apps/commit/2eed14db672ff101c468a15aeea83a76eb9ca893))
* **ci:** release from main and scan for high findings only ([#2](https://github.com/ylepalud/skeletton-apps/issues/2)) ([a57f6c3](https://github.com/ylepalud/skeletton-apps/commit/a57f6c38ce87de67a1d9d87d5f019d85fc09c858))
* **conventions:** keep development-only dependencies out of jib images ([55cd701](https://github.com/ylepalud/skeletton-apps/commit/55cd701de72bd4907e8194c7dade8fefa3eb8227))
* **conventions:** keep the e2e task compatible with the configuration cache ([fc1028b](https://github.com/ylepalud/skeletton-apps/commit/fc1028ba62895115ed80352bfaf987bccf8707d8))
* **conventions:** scope frontend client generation inputs to contract files ([2d146a9](https://github.com/ylepalud/skeletton-apps/commit/2d146a93f9fb22909863e159d6c5e5db547deb57))
* **deps:** update spring boot to 4.1.2 ([#3](https://github.com/ylepalud/skeletton-apps/issues/3)) ([0d7af29](https://github.com/ylepalud/skeletton-apps/commit/0d7af29e402e69dbc16610669c36039eeecf90d9))
