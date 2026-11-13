# GodBox

A WorldBox-style god mode for Minecraft: top-down view, bombs, elements, kingdoms, bosses, world laws and terraforming.

- Minecraft 1.21.1, Fabric Loader 0.16.14, Fabric API, Java 21
- Version 3.3.0, MIT licensed (per `fabric.mod.json`)

## Building

```
./gradlew build
```

The mod JAR is written to `build/libs/godbox-3.3.0.jar`. Use `./gradlew runClient` or `./gradlew runServer` to start a development instance.

## About this source tree

The original source was lost, so this tree was recovered from the released `godbox-3.2.0.jar`: the JAR was remapped from intermediary to Yarn names (`1.21.1+build.3`) and decompiled with Vineflower 1.11.1.

- Comments and the original formatting are gone, and some local variables have generated names such as `var3`.
- Lambdas, switch statements and enhanced `for` loops are the decompiler's reconstruction and may be shaped differently from the code as originally written.
- Nine spots were adjusted by hand so the code compiles: duplicate local variable names in `Creatures`, `Treaties` and `CivPlus`, and the `this` casts in `WorldMixin` and `ServerWorldMixin`.
