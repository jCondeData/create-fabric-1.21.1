# Builds

`create-fabric-6.0.10.0+mc1.21.1.jar` — Create 6.0.10 for Fabric 1.21.1, built from `main` (b8dfa78bb3).

- One jar: bundles Porting Lib, Flywheel, Ponder, Registrate, Forge Config API Port and Milk Lib.
  Needs only Fabric API 0.116.1 or newer (the Cobbleverse pack has 0.116.17).
- Put it in `mods/` on the server and on every client. Back up the server world first.
- Tested: 122 GameTests, a server and a client with only this jar + Fabric API, and the
  Cobbleverse 1.7.42 server (independent tester verdict: PASS WITH RISKS — see the repo's CLAUDE.md).
- Private build for Jesse's Cobbleverse server — don't redistribute (Create's assets are All Rights Reserved).

949e4aaddcbb8697d7baafc5f53b715b2201ab7a881e252c927c5a2709b30944  create-fabric-6.0.10.0+mc1.21.1.jar
