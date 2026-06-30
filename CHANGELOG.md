# Changelog

## [0.0.30](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.29...r2p-v0.0.30) (2026-06-30)


### Features

* **ui:** restyle server UI with R²P Console branding ([#171](https://github.com/SchweizerischeBundesbahnen/r2p/issues/171)) ([db5c978](https://github.com/SchweizerischeBundesbahnen/r2p/commit/db5c97864e39849ff6178d2a14a3e6449a7dad1d))


### Miscellaneous Chores

* **main:** release r2p 0.0.30-SNAPSHOT ([#167](https://github.com/SchweizerischeBundesbahnen/r2p/issues/167)) ([ff685a6](https://github.com/SchweizerischeBundesbahnen/r2p/commit/ff685a681c074b8055f5c25c6a7ded3d475cd119))


### Build System

* **deps-dev:** bump the commitlint group with 2 updates ([#170](https://github.com/SchweizerischeBundesbahnen/r2p/issues/170)) ([bdd6000](https://github.com/SchweizerischeBundesbahnen/r2p/commit/bdd6000a74cb514fe9d469b3358d2a9ddeb0a1e4))
* **deps:** bump actions/checkout from 6 to 7 ([#169](https://github.com/SchweizerischeBundesbahnen/r2p/issues/169)) ([06a6a03](https://github.com/SchweizerischeBundesbahnen/r2p/commit/06a6a036158406d726937bd22ec88f021916446a))

## [0.0.29](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.28...r2p-v0.0.29) (2026-06-24)


### Features

* enforce edit authorization for activities and route ([#166](https://github.com/SchweizerischeBundesbahnen/r2p/issues/166)) ([f676159](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f6761592245039ff7f20fc37e5665fc45300150f))


### Miscellaneous Chores

* **main:** release r2p 0.0.29-SNAPSHOT ([#164](https://github.com/SchweizerischeBundesbahnen/r2p/issues/164)) ([da51cbf](https://github.com/SchweizerischeBundesbahnen/r2p/commit/da51cbf229bb93b2721c3ca28eefab404a7036e2))

## [0.0.28](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.27...r2p-v0.0.28) (2026-06-23)


### Features

* add additional partners ([#158](https://github.com/SchweizerischeBundesbahnen/r2p/issues/158)) ([421f716](https://github.com/SchweizerischeBundesbahnen/r2p/commit/421f71676253f626a804143b70bcdace90a6b2b4))
* add endpoint to determine the instanceowner ([#159](https://github.com/SchweizerischeBundesbahnen/r2p/issues/159)) ([91d1f76](https://github.com/SchweizerischeBundesbahnen/r2p/commit/91d1f7680d274f19691666dd55197ca0923e080c))
* align train data model with train.yaml 1.0.10 ([#163](https://github.com/SchweizerischeBundesbahnen/r2p/issues/163)) ([b765705](https://github.com/SchweizerischeBundesbahnen/r2p/commit/b76570578ca2c42d3d2823aa99388d252c6d0146))


### Bug Fixes

* return 401 for unauthenticated API requests instead of 302 redirect ([#162](https://github.com/SchweizerischeBundesbahnen/r2p/issues/162)) ([cae026e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/cae026ecdf57bf5faa4cf71718204a89f905cc57))


### Miscellaneous Chores

* **main:** release r2p 0.0.28-SNAPSHOT ([#156](https://github.com/SchweizerischeBundesbahnen/r2p/issues/156)) ([e834a08](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e834a08c235d3926cf6d9dbfb46eaf42ff498f5d))


### Code Refactoring

* remove PartnerStats.leadRU in favour of /v1/instance-owner … ([#161](https://github.com/SchweizerischeBundesbahnen/r2p/issues/161)) ([dd12739](https://github.com/SchweizerischeBundesbahnen/r2p/commit/dd12739a60467eec6f44ad48f08487b28ead255e))

## [0.0.27](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.26...r2p-v0.0.27) (2026-06-11)


### Miscellaneous Chores

* **main:** release r2p 0.0.27-SNAPSHOT ([#151](https://github.com/SchweizerischeBundesbahnen/r2p/issues/151)) ([9feec03](https://github.com/SchweizerischeBundesbahnen/r2p/commit/9feec03c3419a9a3487faacde763e2cc89021146))


### Build System

* **deps-dev:** bump org.openapitools:openapi-generator-maven-plugin ([#155](https://github.com/SchweizerischeBundesbahnen/r2p/issues/155)) ([7ef43a3](https://github.com/SchweizerischeBundesbahnen/r2p/commit/7ef43a39d4cb75b8b2f0254b353352345045e966))
* **deps-dev:** bump the commitlint group with 2 updates ([#153](https://github.com/SchweizerischeBundesbahnen/r2p/issues/153)) ([fb9185d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/fb9185dc323f98b3cf073a23d742a8f8b92e8cfd))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#154](https://github.com/SchweizerischeBundesbahnen/r2p/issues/154)) ([289fa0f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/289fa0f0081dbb1ea5445194b63ac931b833a679))

## [0.0.26](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.25...r2p-v0.0.26) (2026-06-01)


### Bug Fixes

* branch deletion no longer fails on corrupt payload ([#149](https://github.com/SchweizerischeBundesbahnen/r2p/issues/149)) ([b80e81a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/b80e81a3b3afcc7b291f8a106edea7ede21144f7))


### Miscellaneous Chores

* **main:** release r2p 0.0.26-SNAPSHOT ([#147](https://github.com/SchweizerischeBundesbahnen/r2p/issues/147)) ([39b862f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/39b862f7279b4f08ca837133de2d7df6795da4d3))

## [0.0.25](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.24...r2p-v0.0.25) (2026-05-29)


### Features

* add keycloak setup ([#145](https://github.com/SchweizerischeBundesbahnen/r2p/issues/145)) ([4a41497](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4a414978888d5ca125e7d9337551e913d1b2b7dd))


### Bug Fixes

* skip trains with corrupt payloads during listTrains ([#146](https://github.com/SchweizerischeBundesbahnen/r2p/issues/146)) ([4bacf59](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4bacf596e7ca88731019f8475ebfe664c257aa92))


### Miscellaneous Chores

* **main:** release r2p 0.0.25-SNAPSHOT ([#142](https://github.com/SchweizerischeBundesbahnen/r2p/issues/142)) ([67dc680](https://github.com/SchweizerischeBundesbahnen/r2p/commit/67dc68033c3319c8889207c5185fab7f91a1913e))

## [0.0.24](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.23...r2p-v0.0.24) (2026-05-20)


### Bug Fixes

* bpsglo 14065: add cors config to security config ([#141](https://github.com/SchweizerischeBundesbahnen/r2p/issues/141)) ([a97a354](https://github.com/SchweizerischeBundesbahnen/r2p/commit/a97a3540e7757bda8713b257c43334340e3fe7ee))


### Miscellaneous Chores

* **main:** release r2p 0.0.24-SNAPSHOT ([#139](https://github.com/SchweizerischeBundesbahnen/r2p/issues/139)) ([6ee5186](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6ee5186391553ed329b5395a46741cd4a79225bd))

## [0.0.23](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.22...r2p-v0.0.23) (2026-05-20)


### Features

* remove sbb references ([#138](https://github.com/SchweizerischeBundesbahnen/r2p/issues/138)) ([2d1a40e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/2d1a40eed33dffbaee603d979a8b37f4d8009e39))


### Miscellaneous Chores

* **main:** release r2p 0.0.23-SNAPSHOT ([#133](https://github.com/SchweizerischeBundesbahnen/r2p/issues/133)) ([9d80a8c](https://github.com/SchweizerischeBundesbahnen/r2p/commit/9d80a8c1e8356204c0b9ce146ed3629614518318))
* rename app-token-input ([#135](https://github.com/SchweizerischeBundesbahnen/r2p/issues/135)) ([50fa5bb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/50fa5bb3faca3e46145956296545962871856d4c))


### Build System

* **deps-dev:** bump fast-uri from 3.1.0 to 3.1.2 ([#130](https://github.com/SchweizerischeBundesbahnen/r2p/issues/130)) ([d3b8210](https://github.com/SchweizerischeBundesbahnen/r2p/commit/d3b8210664806f67074b38ea4f1681de9d74ad2b))
* **deps-dev:** bump org.codehaus.mojo:license-maven-plugin ([#116](https://github.com/SchweizerischeBundesbahnen/r2p/issues/116)) ([80758b4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/80758b409ffed721227a91b2b4be209ee75dc2d4))
* **deps-dev:** bump the commitlint group with 2 updates ([#137](https://github.com/SchweizerischeBundesbahnen/r2p/issues/137)) ([510cc50](https://github.com/SchweizerischeBundesbahnen/r2p/commit/510cc50ba19efd1f6129a3ced8d5b3e23d58a9be))
* **deps:** bump org.webjars.npm:alpinejs from 3.15.11 to 3.15.12 ([#136](https://github.com/SchweizerischeBundesbahnen/r2p/issues/136)) ([f0696ba](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f0696ba4841c22a6d0ca77ecbfccb94cd0e40e22))

## [0.0.22](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.21...r2p-v0.0.22) (2026-05-12)


### Bug Fixes

* yaml lint automerge condition ([dd7664a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/dd7664a9ee2e36aaacacdc55a55cb9fdc6c2ebaa))


### Miscellaneous Chores

* **main:** release r2p 0.0.22-SNAPSHOT ([#131](https://github.com/SchweizerischeBundesbahnen/r2p/issues/131)) ([8acc1d0](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8acc1d068b6a6902f7e50e4b29a06131f14799d5))

## [0.0.21](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.20...r2p-v0.0.21) (2026-05-12)


### Features

* mandatory comments for operations and webclient design update ([8edada3](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8edada31f76a10cc878b46d464211e735ef984e2))


### Miscellaneous Chores

* **main:** release r2p 0.0.21-SNAPSHOT ([#126](https://github.com/SchweizerischeBundesbahnen/r2p/issues/126)) ([bc9697c](https://github.com/SchweizerischeBundesbahnen/r2p/commit/bc9697c07215e90b46391b8a426072e4be63d647))
* remove webclient artifacts ([#128](https://github.com/SchweizerischeBundesbahnen/r2p/issues/128)) ([c219523](https://github.com/SchweizerischeBundesbahnen/r2p/commit/c2195238a5c6ed945f44c984394c03e5d57ce610))


### Continuous Integration

* [#111](https://github.com/SchweizerischeBundesbahnen/r2p/issues/111) replace custom automerge action ([#129](https://github.com/SchweizerischeBundesbahnen/r2p/issues/129)) ([f2b5709](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f2b570967cb417e07897fa9581c301493eca2364))

## [0.0.20](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.19...r2p-v0.0.20) (2026-05-08)


### Bug Fixes

* bpsglo-14279: get, list and stats endpoint omit deleted trains ([#118](https://github.com/SchweizerischeBundesbahnen/r2p/issues/118)) ([a54ff24](https://github.com/SchweizerischeBundesbahnen/r2p/commit/a54ff24d46affc22a652a83cf845c88471ced1fc))


### Documentation

* document authorization limitation ([#120](https://github.com/SchweizerischeBundesbahnen/r2p/issues/120)) ([4aa0773](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4aa0773c62d41119695ec6f0aaa6ab7e3800b369))
* moved REST endpoint and domain model documentation to wiki ([#119](https://github.com/SchweizerischeBundesbahnen/r2p/issues/119)) ([5c9f649](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5c9f6497d88aad88dbc603eaba30af05862cda61))


### Miscellaneous Chores

* **main:** release r2p 0.0.20-SNAPSHOT ([#114](https://github.com/SchweizerischeBundesbahnen/r2p/issues/114)) ([d9a86b9](https://github.com/SchweizerischeBundesbahnen/r2p/commit/d9a86b9ade66fcbd204c39cea140b63e8680d252))


### Build System

* **deps-dev:** bump org.openapitools:openapi-generator-maven-plugin ([#122](https://github.com/SchweizerischeBundesbahnen/r2p/issues/122)) ([21df1fb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/21df1fb5b0ded391a932a3ced0b90d939019fc86))
* **deps-dev:** bump the commitlint group with 2 updates ([#123](https://github.com/SchweizerischeBundesbahnen/r2p/issues/123)) ([bcba1ab](https://github.com/SchweizerischeBundesbahnen/r2p/commit/bcba1ab57cb6805f720f0bdc27d41aec58ce39f6))
* **deps:** bump googleapis/release-please-action from 4 to 5 ([#117](https://github.com/SchweizerischeBundesbahnen/r2p/issues/117)) ([85b53f4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/85b53f4ca4830f829d54c569c913e97596046474))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#121](https://github.com/SchweizerischeBundesbahnen/r2p/issues/121)) ([210d0aa](https://github.com/SchweizerischeBundesbahnen/r2p/commit/210d0aacdf1effccb8f5a6377840681ee4f89814))


### Continuous Integration

* using github app token ([#124](https://github.com/SchweizerischeBundesbahnen/r2p/issues/124)) ([916cc01](https://github.com/SchweizerischeBundesbahnen/r2p/commit/916cc014fdf750aa921a6715b17b02f6a725706e))

## [0.0.19](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.18...r2p-v0.0.19) (2026-04-23)


### Features

* add information about third party license ([5b37a2d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5b37a2dc9790db0c932b990f3debd3655d2f1fb4))
* add r2p webclient testdatengenerator ([18f6702](https://github.com/SchweizerischeBundesbahnen/r2p/commit/18f6702a04795e2157c061d5d08198e1c9237e0a))
* design draft of the webclient ([80f9f2b](https://github.com/SchweizerischeBundesbahnen/r2p/commit/80f9f2b102e8d500c1fd6c652b4d0259f4c5cd63))


### Documentation

* document silent ignoring of properties as open point and add a minimal train ([#105](https://github.com/SchweizerischeBundesbahnen/r2p/issues/105)) ([5f65598](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5f65598d4b9dcef7331d4ce9caf1530753f9ef03))
* remove redundant documentation, other improvements ([f125035](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f12503515aea9cd310732fb036d7d9444dc9f215))
* update openapi specs, publish generated api doc as html ([10c55b9](https://github.com/SchweizerischeBundesbahnen/r2p/commit/10c55b959140d0e1fcd1209087949d8b5a6eae37))
* update the documentation ([#101](https://github.com/SchweizerischeBundesbahnen/r2p/issues/101)) ([08e959b](https://github.com/SchweizerischeBundesbahnen/r2p/commit/08e959ba15f6d66bcd9739f17cf6ad88ac538e28))


### Miscellaneous Chores

* **main:** release r2p 0.0.19-SNAPSHOT ([#100](https://github.com/SchweizerischeBundesbahnen/r2p/issues/100)) ([f014a79](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f014a7910281314536880c8d324ecb365a2c9c27))


### Build System

* **deps-dev:** bump org.openapitools:openapi-generator-maven-plugin ([#103](https://github.com/SchweizerischeBundesbahnen/r2p/issues/103)) ([c766a75](https://github.com/SchweizerischeBundesbahnen/r2p/commit/c766a75d8bc80ababa405fab46699652ddf517eb))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([#108](https://github.com/SchweizerischeBundesbahnen/r2p/issues/108)) ([47ac02e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/47ac02ed57da21ce8e17725909092c98f8e14a5b))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#102](https://github.com/SchweizerischeBundesbahnen/r2p/issues/102)) ([b335b1a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/b335b1a174dc1846ebc0cfa77539ac6b8dad4fcc))
* **deps:** bump org.webjars.npm:alpinejs from 3.15.8 to 3.15.11 ([#106](https://github.com/SchweizerischeBundesbahnen/r2p/issues/106)) ([8aa669a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8aa669a569da6f40de370f0ed9965aabbe760cab))

## [0.0.18](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.17...r2p-v0.0.18) (2026-03-25)


### Features

* merge requests and their approval and rejection ([e7086f4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e7086f4221a868d88337de7c208d31b6f86f8a9c))


### Miscellaneous Chores

* **main:** release r2p 0.0.18-SNAPSHOT ([#94](https://github.com/SchweizerischeBundesbahnen/r2p/issues/94)) ([db35c3d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/db35c3d983cf5a7b42b694faab1f4ceeac83d704))


### Build System

* **deps-dev:** bump the commitlint group with 2 updates ([#96](https://github.com/SchweizerischeBundesbahnen/r2p/issues/96)) ([6f20b0e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6f20b0e1421f81e5855f0c3b8debfb3c2878c8ed))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#97](https://github.com/SchweizerischeBundesbahnen/r2p/issues/97)) ([cb0dd40](https://github.com/SchweizerischeBundesbahnen/r2p/commit/cb0dd401330243b8ca081e316fd17a6b514f667b))
* **deps:** bump org.webjars.npm:alpinejs from 3.14.9 to 3.15.8 ([#95](https://github.com/SchweizerischeBundesbahnen/r2p/issues/95)) ([a42ee31](https://github.com/SchweizerischeBundesbahnen/r2p/commit/a42ee312292f0e6aa64401a5cf61e63e0d5488a2))

## [0.0.17](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.16...r2p-v0.0.17) (2026-03-18)


### Bug Fixes

* show correct version number, other minor improvements ([e3c91ae](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e3c91aefb8ab7b7d33bb9be5cf7bebd669351d25))


### Miscellaneous Chores

* **main:** release r2p 0.0.17-SNAPSHOT ([#91](https://github.com/SchweizerischeBundesbahnen/r2p/issues/91)) ([f60c6bf](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f60c6bfb8f6dfecc9dee6b745ae994a7eddf969a))

## [0.0.16](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.15...r2p-v0.0.16) (2026-03-17)


### Features

* add server UI ([5792638](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5792638ff3aeea61047061312ce5e876e68a936f))
* better error messages when using the api ([#82](https://github.com/SchweizerischeBundesbahnen/r2p/issues/82)) ([e03d065](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e03d065283cf49e93d5beefcbb31e34c4fb7f461))


### Bug Fixes

* ADR 0001 diagram ([#77](https://github.com/SchweizerischeBundesbahnen/r2p/issues/77)) ([e57c34d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e57c34d8d3661423ec94070397b41c7ef5d63f53))


### Documentation

* decide ADR 0001 ([#71](https://github.com/SchweizerischeBundesbahnen/r2p/issues/71)) ([c0469e4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/c0469e478f824a1c83297b32ebfc7e95d55028cd))
* update copilot instructions ([#79](https://github.com/SchweizerischeBundesbahnen/r2p/issues/79)) ([fece0e9](https://github.com/SchweizerischeBundesbahnen/r2p/commit/fece0e927bcda9284b627eee91b4f594f25916ad))


### Miscellaneous Chores

* **main:** release r2p 0.0.16-SNAPSHOT ([#76](https://github.com/SchweizerischeBundesbahnen/r2p/issues/76)) ([df439fa](https://github.com/SchweizerischeBundesbahnen/r2p/commit/df439fa5f7afb62e413c243538edcce030b45428))


### Tests

* improve test coverage ([#80](https://github.com/SchweizerischeBundesbahnen/r2p/issues/80)) ([5876367](https://github.com/SchweizerischeBundesbahnen/r2p/commit/58763672b17d3acffe978bfa432903e6605ce0f9))


### Build System

* **deps-dev:** bump the commitlint group with 2 updates ([#88](https://github.com/SchweizerischeBundesbahnen/r2p/issues/88)) ([6a2c690](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6a2c6908d6c1018657e5300217435f5f549a80f3))
* **deps-dev:** bump the commitlint group with 2 updates ([#90](https://github.com/SchweizerischeBundesbahnen/r2p/issues/90)) ([41d3dd6](https://github.com/SchweizerischeBundesbahnen/r2p/commit/41d3dd6983bd91eae9ecb496920c4d6b51fb3a30))
* **deps:** bump actions/download-artifact from 7 to 8 ([#85](https://github.com/SchweizerischeBundesbahnen/r2p/issues/85)) ([05414a4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/05414a49c37627c08d67d20a3543fb05eeda8127))
* **deps:** bump actions/setup-node from 4 to 6 ([#83](https://github.com/SchweizerischeBundesbahnen/r2p/issues/83)) ([a991732](https://github.com/SchweizerischeBundesbahnen/r2p/commit/a99173214aa8a167c67cb24ac5617f1302cadd6c))
* **deps:** bump actions/upload-artifact from 6 to 7 ([#86](https://github.com/SchweizerischeBundesbahnen/r2p/issues/86)) ([6b38395](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6b38395fd490ae0d9d473070d7492ea34dfda93f))
* **deps:** bump docker/login-action from 3 to 4 ([#84](https://github.com/SchweizerischeBundesbahnen/r2p/issues/84)) ([ecf6618](https://github.com/SchweizerischeBundesbahnen/r2p/commit/ecf6618e2b1324b2c506d64919aaa296306c06dd))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([#87](https://github.com/SchweizerischeBundesbahnen/r2p/issues/87)) ([e9eaa30](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e9eaa30d7cb2b2b3a624544e163c1933c25c7edf))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#66](https://github.com/SchweizerischeBundesbahnen/r2p/issues/66)) ([027a038](https://github.com/SchweizerischeBundesbahnen/r2p/commit/027a038412dcb4c3b2d2a63ed15ebd80afc57305))


### Continuous Integration

* add spec lint with output and result upload ([#81](https://github.com/SchweizerischeBundesbahnen/r2p/issues/81)) ([182592f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/182592f3081b1e8051f95d56a7a7564cffe28e87))

## [0.0.15](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.14...r2p-v0.0.15) (2026-02-20)


### Bug Fixes

* docker image creation failed ([8657419](https://github.com/SchweizerischeBundesbahnen/r2p/commit/865741901050fe9a165c42c68c4b59b9a1b76d85))


### Miscellaneous Chores

* **main:** release r2p 0.0.15-SNAPSHOT ([#74](https://github.com/SchweizerischeBundesbahnen/r2p/issues/74)) ([9575fba](https://github.com/SchweizerischeBundesbahnen/r2p/commit/9575fba40d1f8a08756d16021d674095780e248f))

## [0.0.14](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.13...r2p-v0.0.14) (2026-02-20)


### Bug Fixes

* docker image creation failed ([3d2783f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/3d2783fc474d3dd71fd5dc8fa0a14ab342272ecc))


### Miscellaneous Chores

* **main:** release r2p 0.0.14-SNAPSHOT ([#72](https://github.com/SchweizerischeBundesbahnen/r2p/issues/72)) ([9e228c2](https://github.com/SchweizerischeBundesbahnen/r2p/commit/9e228c2689506f04180d59132e8accd9f2298bbf))

## [0.0.13](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.12...r2p-v0.0.13) (2026-02-20)


### Miscellaneous Chores

* **main:** release r2p 0.0.13-SNAPSHOT ([#63](https://github.com/SchweizerischeBundesbahnen/r2p/issues/63)) ([fb7467f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/fb7467f3deb5cb85a205c75b7f9f52b25e967fc0))


### Build System

* **deps-dev:** bump ajv from 8.17.1 to 8.18.0 ([#70](https://github.com/SchweizerischeBundesbahnen/r2p/issues/70)) ([dd3fc8a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/dd3fc8a88dd4c5b32810d3e45c351227449af045))
* **deps-dev:** bump org.openapitools:openapi-generator-maven-plugin ([#68](https://github.com/SchweizerischeBundesbahnen/r2p/issues/68)) ([e299abb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/e299abbd7918ed9b2eb3c14fbe97827cbfb600e2))
* **deps-dev:** bump the commitlint group with 2 updates ([#67](https://github.com/SchweizerischeBundesbahnen/r2p/issues/67)) ([6fced68](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6fced682cd17da74ee2c69fd7fb45f6cb94e097c))
* use Spring Boot buildpacks ([#64](https://github.com/SchweizerischeBundesbahnen/r2p/issues/64)) ([46e227f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/46e227ff792c6de938d5a726c2092ccdc5f8be57))

## [0.0.12](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.11...r2p-v0.0.12) (2026-02-13)


### Features

* enable automatic updates ([#60](https://github.com/SchweizerischeBundesbahnen/r2p/issues/60)) ([9f08ae8](https://github.com/SchweizerischeBundesbahnen/r2p/commit/9f08ae848a034471cfb13fb27b00323d1e7e9fb1))


### Documentation

* updated copilot-instructions, readme, manual and added a description to the openapi endpoints ([d86c03a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/d86c03ab01b53899c574cbc54d8fd431669777cc))


### Miscellaneous Chores

* **main:** release r2p 0.0.12-SNAPSHOT ([#59](https://github.com/SchweizerischeBundesbahnen/r2p/issues/59)) ([d13fb54](https://github.com/SchweizerischeBundesbahnen/r2p/commit/d13fb54386c9163f9c54120c170ab55b1b244afe))

## [0.0.11](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.10...r2p-v0.0.11) (2026-02-12)


### Features

* implement changes requested at review at 2026-01-09 ([1f481d3](https://github.com/SchweizerischeBundesbahnen/r2p/commit/1f481d3798cd02e86a227c6b95d84196a19c3eae))
* update spec ([#55](https://github.com/SchweizerischeBundesbahnen/r2p/issues/55)) ([8a58e3a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8a58e3accb25cffa1bd041c73c58724bdf9c3444))


### Documentation

* add copilot-instructions ([#51](https://github.com/SchweizerischeBundesbahnen/r2p/issues/51)) ([01cea2f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/01cea2f0e378c4f91d3a5c7ad023d086c4e89ee6))
* describe openshift deployment ([#50](https://github.com/SchweizerischeBundesbahnen/r2p/issues/50)) ([b2e47ff](https://github.com/SchweizerischeBundesbahnen/r2p/commit/b2e47ff2441add5f05d8a2ac6deb4d6ecdc306e5))


### Miscellaneous Chores

* **main:** release r2p 0.0.11-SNAPSHOT ([#49](https://github.com/SchweizerischeBundesbahnen/r2p/issues/49)) ([4718c5b](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4718c5b7c9fc1b90ef6c703185e6be0faef44bf2))


### Build System

* **deps-dev:** bump org.openapitools:openapi-generator-maven-plugin ([#53](https://github.com/SchweizerischeBundesbahnen/r2p/issues/53)) ([c71c820](https://github.com/SchweizerischeBundesbahnen/r2p/commit/c71c820756b8745d96ebb49477438e4f46ea47dc))
* **deps-dev:** bump the commitlint group with 2 updates ([#57](https://github.com/SchweizerischeBundesbahnen/r2p/issues/57)) ([933dbb6](https://github.com/SchweizerischeBundesbahnen/r2p/commit/933dbb63d13454c742d78092a2848d695f89c5e8))
* **deps:** bump org.openapitools:jackson-databind-nullable ([#56](https://github.com/SchweizerischeBundesbahnen/r2p/issues/56)) ([0ae44eb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/0ae44eb25a5fd5e8b36ff5c235db27a32e0cc1d6))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#54](https://github.com/SchweizerischeBundesbahnen/r2p/issues/54)) ([d85386d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/d85386dec1b38cc14f1f0500ff2fabbdc6672621))

## [0.0.10](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.9...r2p-v0.0.10) (2026-01-16)


### Features

* enable actuator endpoints ([#48](https://github.com/SchweizerischeBundesbahnen/r2p/issues/48)) ([ecebbf6](https://github.com/SchweizerischeBundesbahnen/r2p/commit/ecebbf63c3f27c29bc560dddc4e8313fbdaf78ee))


### Miscellaneous Chores

* **main:** release r2p 0.0.10-SNAPSHOT ([#45](https://github.com/SchweizerischeBundesbahnen/r2p/issues/45)) ([4115709](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4115709669d8b37aa3a9a5d0abc4b193a28143ce))


### Build System

* **deps-dev:** bump the commitlint group with 2 updates ([#46](https://github.com/SchweizerischeBundesbahnen/r2p/issues/46)) ([2072654](https://github.com/SchweizerischeBundesbahnen/r2p/commit/207265454de866061ccdac4904933680d76cae79))

## [0.0.9](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.8...r2p-v0.0.9) (2026-01-08)


### Features

* display validation messages ([#43](https://github.com/SchweizerischeBundesbahnen/r2p/issues/43)) ([eb7c6fb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/eb7c6fbd0d73598ff15be41763ccdaa94daf33dc))
* prepare api for changelog implementation ([3c073fe](https://github.com/SchweizerischeBundesbahnen/r2p/commit/3c073fe288d9da846586ab1c6449b9d39fe9d1a3))
* store changelog and implement /changes endpoints ([#44](https://github.com/SchweizerischeBundesbahnen/r2p/issues/44)) ([7769a9a](https://github.com/SchweizerischeBundesbahnen/r2p/commit/7769a9a00b8dfd5a5105eaf0bc70e6cc430dcfa8))


### Miscellaneous Chores

* **main:** release r2p 0.0.9-SNAPSHOT ([#32](https://github.com/SchweizerischeBundesbahnen/r2p/issues/32)) ([94a16fe](https://github.com/SchweizerischeBundesbahnen/r2p/commit/94a16fe8ecf3f36dc2c11ea696a806318c9d80fa))


### Build System

* **deps-dev:** bump @commitlint/cli from 20.2.0 to 20.3.0 ([#38](https://github.com/SchweizerischeBundesbahnen/r2p/issues/38)) ([1083a64](https://github.com/SchweizerischeBundesbahnen/r2p/commit/1083a64076268fabd0f86e635cf6197b247b8104))
* **deps-dev:** bump @commitlint/config-conventional from 20.2.0 to 20.3.0 ([#37](https://github.com/SchweizerischeBundesbahnen/r2p/issues/37)) ([87cb6a1](https://github.com/SchweizerischeBundesbahnen/r2p/commit/87cb6a112604d10fa4fee3da438d2ad3e2fb822c))
* **deps:** bump org.codehaus.mojo:exec-maven-plugin ([#34](https://github.com/SchweizerischeBundesbahnen/r2p/issues/34)) ([cefd67e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/cefd67e7622fdcaa80592e5345d7fd79b659c118))
* **deps:** bump org.openapitools:openapi-generator-maven-plugin ([#35](https://github.com/SchweizerischeBundesbahnen/r2p/issues/35)) ([8c92772](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8c92772cb36f4cb1ea023eb7e5a81541f99aef04))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([#41](https://github.com/SchweizerischeBundesbahnen/r2p/issues/41)) ([f99dc8d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/f99dc8d7f1498a5b163c89cce96459aea62c55f3))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#36](https://github.com/SchweizerischeBundesbahnen/r2p/issues/36)) ([3575dad](https://github.com/SchweizerischeBundesbahnen/r2p/commit/3575dad7ab0990fbf9a3427c512b15abba751b87))
* **deps:** bump org.springframework.boot:spring-boot-starter-parent ([#42](https://github.com/SchweizerischeBundesbahnen/r2p/issues/42)) ([7f611cb](https://github.com/SchweizerischeBundesbahnen/r2p/commit/7f611cb871fd4ecd844c6aba40719e067d11e0de))


### Continuous Integration

* group commitlint dependencies ([#40](https://github.com/SchweizerischeBundesbahnen/r2p/issues/40)) ([dbd9cc2](https://github.com/SchweizerischeBundesbahnen/r2p/commit/dbd9cc2c9dc1bedd424c668a3e8b99cf8be51246))

## [0.0.8](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.7...r2p-v0.0.8) (2025-12-23)


### Features

* implementation of train and branch write/query endpoints including persistence ([914bb88](https://github.com/SchweizerischeBundesbahnen/r2p/commit/914bb88a14c70746a06e2bb8eaec25a89567fa55))


### Documentation

* document docker image ([cfb75fc](https://github.com/SchweizerischeBundesbahnen/r2p/commit/cfb75fc28b01be834be889466efb2e2c13883cfa))


### Miscellaneous Chores

* **main:** release r2p 0.0.8-SNAPSHOT ([#29](https://github.com/SchweizerischeBundesbahnen/r2p/issues/29)) ([57e8b59](https://github.com/SchweizerischeBundesbahnen/r2p/commit/57e8b59e0916d4f91aee6fe56a44f9bb1c8bbfd0))

## [0.0.7](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.6...r2p-v0.0.7) (2025-12-17)


### Miscellaneous Chores

* **main:** release r2p 0.0.7-SNAPSHOT ([#27](https://github.com/SchweizerischeBundesbahnen/r2p/issues/27)) ([4b9c649](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4b9c6490a09dc3ab6b4089a82a94b45472115018))


### Continuous Integration

* checkout the repo for the upload docs step ([6ef38ff](https://github.com/SchweizerischeBundesbahnen/r2p/commit/6ef38ff63a4a0a18e95512b1a0208e1316f95a89))
* use pat to allow triggering automerge ([934271c](https://github.com/SchweizerischeBundesbahnen/r2p/commit/934271c05b1e2d322d14a105c12dc9e2f5ea77ff))

## [0.0.6](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.5...r2p-v0.0.6) (2025-12-17)


### Miscellaneous Chores

* **main:** release r2p 0.0.6-SNAPSHOT ([#25](https://github.com/SchweizerischeBundesbahnen/r2p/issues/25)) ([941db19](https://github.com/SchweizerischeBundesbahnen/r2p/commit/941db197b62c815a2285347d6d7695db0eb8c36e))


### Continuous Integration

* fix docker image build ([ee6be16](https://github.com/SchweizerischeBundesbahnen/r2p/commit/ee6be168eb93348915265a80fcbc0ba56432901a))
* limit the artifacts to upload to the release ([5d5f22e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5d5f22e291cd7adfbe2cdc3b1a58595282c5250d))

## [0.0.5](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.4...r2p-v0.0.5) (2025-12-17)


### Miscellaneous Chores

* **main:** release r2p 0.0.5-SNAPSHOT ([#19](https://github.com/SchweizerischeBundesbahnen/r2p/issues/19)) ([3332daf](https://github.com/SchweizerischeBundesbahnen/r2p/commit/3332daf6cf084b3d13b7022d965da7f988c86f5f))


### Continuous Integration

* add a docker image ([#20](https://github.com/SchweizerischeBundesbahnen/r2p/issues/20)) ([14d5241](https://github.com/SchweizerischeBundesbahnen/r2p/commit/14d524125113251035fc9fa45df27078f66a8865))
* automerge snapshot prs ([#21](https://github.com/SchweizerischeBundesbahnen/r2p/issues/21)) ([8d7f975](https://github.com/SchweizerischeBundesbahnen/r2p/commit/8d7f975bfce98969fef7c0e5461b44da4f3c585f))
* fix native image generation ([#22](https://github.com/SchweizerischeBundesbahnen/r2p/issues/22)) ([4c355d8](https://github.com/SchweizerischeBundesbahnen/r2p/commit/4c355d806a45dfccd66eb94ad5897207d58def03))
* squash automerged prs ([#23](https://github.com/SchweizerischeBundesbahnen/r2p/issues/23)) ([6175082](https://github.com/SchweizerischeBundesbahnen/r2p/commit/617508202ed28a3eec0dd9da35cb7bd9b0b37cce))

## [0.0.4](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.3...r2p-v0.0.4) (2025-12-17)


### Features

* setup githug actions to automerge snapshot update prs and lint the commit message ([5e7e298](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5e7e2980757f78188d21c3f332ac305b9c33a56b))


### Bug Fixes

* wrong label for automerge ([0984c1e](https://github.com/SchweizerischeBundesbahnen/r2p/commit/0984c1ef687569a0085b0f304737d5642bbf9f05))


### Miscellaneous Chores

* **main:** release r2p 0.0.4-SNAPSHOT ([#15](https://github.com/SchweizerischeBundesbahnen/r2p/issues/15)) ([c690d1d](https://github.com/SchweizerischeBundesbahnen/r2p/commit/c690d1da83d9b73137023bc8999174d0ff11de89))


### Build System

* **deps:** bump actions/download-artifact from 4 to 7 ([#11](https://github.com/SchweizerischeBundesbahnen/r2p/issues/11)) ([5969dc1](https://github.com/SchweizerischeBundesbahnen/r2p/commit/5969dc1180c133b29bf0bad01249bf8a52905c9e))
* **deps:** bump actions/upload-artifact from 4 to 6 ([#18](https://github.com/SchweizerischeBundesbahnen/r2p/issues/18)) ([86492e4](https://github.com/SchweizerischeBundesbahnen/r2p/commit/86492e46607e209f16103241af151df05171bd4d))


### Continuous Integration

* adjust release-please config ([88a4f0f](https://github.com/SchweizerischeBundesbahnen/r2p/commit/88a4f0f934258f0f40fe85e43b455d34586ada42))
* remove docker configuration for dependabot ([#17](https://github.com/SchweizerischeBundesbahnen/r2p/issues/17)) ([1dd88ab](https://github.com/SchweizerischeBundesbahnen/r2p/commit/1dd88ab102b60a43171814898875fcc712ff9588))

## [0.0.3](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.2...r2p-v0.0.3) (2025-12-15)


### Features

* API adaption according to review feedback, move API train data model to separate file ([1e308d3](https://github.com/SchweizerischeBundesbahnen/r2p/commit/1e308d3a17f0e34418f336cf5372cdc1cb5d6eeb))


### Bug Fixes

* need platform specific profiles ([7a30c05](https://github.com/SchweizerischeBundesbahnen/r2p/commit/7a30c05deeb556655811b656024c7fb02e66c6e1))
* need platform specific profiles ([576ee88](https://github.com/SchweizerischeBundesbahnen/r2p/commit/576ee88b7746701dd748b4d7afc0af29f5f5ea56))

## [0.0.2](https://github.com/SchweizerischeBundesbahnen/r2p/compare/r2p-v0.0.1...r2p-v0.0.2) (2025-12-10)


### Bug Fixes

* release please did not create an actual release ([89a04f9](https://github.com/SchweizerischeBundesbahnen/r2p/commit/89a04f9916a98391ab263e66be70b892a4c7d1a8))


### Reverts

* "fix: release please did not create an actual release" ([312d64b](https://github.com/SchweizerischeBundesbahnen/r2p/commit/312d64bc4a101caf0abc5d9b526f3d887bf0843a))
