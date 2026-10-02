@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
    id("maven-publish")
}

val minecraft = stonecutter.current.version
val mcVersion = stonecutter.current.project.substringBeforeLast('-')
fun dep(name: String) = property("deps.$name") as String
fun optDep(name: String) = findProperty("deps.$name") as String?
fun since(predicate: String) = stonecutter.eval(minecraft, predicate)

val modId = property("mod.id") as String
val accessWidener = accessWidenerName(modId, since(">=26.1"))

for ((dir, predicate) in RESOURCE_OVERLAYS) {
    if (since(predicate)) sourceSets.main { resources.srcDir(rootProject.file("src/main/overlays/$dir")) }
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft"] = prop("mod.mc_dep_fabric")
        this["mod_id"] = prop("mod.id")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["fabric_loader_version"] = prop("deps.fabric_loader")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
        this["access_widener"] = accessWidener
    }
    inputs.properties(props)

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(props)
    }
}

tasks.named("processResources") {
    dependsOn(":${stonecutter.current.project}:stonecutterGenerate")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = modId

repositories {
    reliableAdvancementsRepositories()
}

dependencies {
    minecraft("com.mojang:minecraft:${dep("minecraft")}")
    if (!since(">=26.1")) {
        val loom = project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()
        "mappings"(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${dep("minecraft")}:${dep("parchment")}@zip")
        })
    }
    modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")

    // YACL
    val yacl = "dev.isxander:yet-another-config-lib:${dep("yacl")}"
    modCompileOnly(yacl)
    modLocalRuntime(yacl)

    // Mod Menu
    modImplementation("com.terraformersmc:modmenu:${dep("modmenu")}")

    // CodecUI
    optDep("codecui")?.let {
        modImplementation("net.mehvahdjukaar:codecui-fabric:$it")
        include("net.mehvahdjukaar:codecui-fabric:$it")
    }

    // EMI
    optDep("emi")?.let { modLocalRuntime("dev.emi:emi-fabric:$it") }
}

val accessWidenerFile = rootProject.file("src/main/overlays/${if (since(">=26.1")) "26.x" else "pre-26.1"}/$accessWidener")
if (accessWidenerFile.exists()) {
    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
        accessWidenerPath.set(accessWidenerFile)
    }
}

extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
    runs {
        named("client") {
            runDir("run/client")
        }
        named("server") {
            runDir("run/server")
        }
    }
}

tasks {
    processResources {
        exclude(
            "**/neoforge.mods.toml", "**/mods.toml", "**/accesstransformer.cfg",
            "**/*.neoforge.mixins.json"
        )
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(loomx.modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

val javaVer = (property("deps.java_version") as String).toInt()
java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVer)
    sourceCompatibility = JavaVersion.toVersion(javaVer)
    targetCompatibility = JavaVersion.toVersion(javaVer)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVer
}

val modName = property("mod.name") as String
tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", "LICENSE_$modName")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = property("mod.group") as String
            artifactId = "$modId-fabric"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} Fabric $mcVersion - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api")
        optional("yacl")
        optional("modmenu")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api")
        optional("yacl")
        optional("modmenu")
        client = true
        server = true
    }
}
