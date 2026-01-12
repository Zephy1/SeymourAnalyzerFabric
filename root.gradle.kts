plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("gg.essential.multi-version.root")
}

preprocess.strictExtraMappings.set(true)
preprocess {
    val fabric12111 = createNode("1.21.11-fabric", 12111, "yarn")
    val fabric12110 = createNode("1.21.10-fabric", 12110, "yarn")
    val fabric12108 = createNode("1.21.8-fabric", 12108, "yarn")
    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")

    fabric12111.link(fabric12110)
    fabric12110.link(fabric12108)
    fabric12108.link(fabric12105)
}
