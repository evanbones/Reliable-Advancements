plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
    id("maven-publish")
}

val minecraft = stonecutter.current.version
val mcVersion = stonecutter.current.project.substringBeforeLast('-')
fun dep(name: String) = property("deps.$name") as String
fun optDep(name: String) = findProperty("deps.$name") as String?
fun since(predicate: String) = stonecutter.eval(minecraft, predicate)

val modId = property("mod.id") as String

for ((dir, predicate) in RESOURCE_OVERLAYS) {
    if (since(predicate)) sourceSets.main { resources.srcDir(rootProject.file("src/main/overlays/$dir")) }
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft_version_range"] = prop("mod.mc_dep_forgelike")
        this["mod_id"] = prop("mod.id")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["neoforge_loader_version_range"] = prop("deps.neoforge_loader_version_range")
        this["neoforge_version"] = prop("deps.neoforge")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
    }
    inputs.properties(props)

    filesMatching(listOf("pack.mcmeta", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = modId

repositories {
    reliableAdvancementsRepositories(rootProject.file("libs/maven"))
}

neoForge {
    enable {
        version = dep("neoforge")
        isDisableRecompilation = true
    }
    validateAccessTransformers = true

    val at = rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformers.from(at.absolutePath)

    optDep("parchment")?.let {
        parchment {
            minecraftVersion = dep("minecraft")
            mappingsVersion = it
        }
    }

    runs {
        configureEach {
            systemProperty("neoforge.warnings.onlyin.hide", "true")
        }
        register("client") {
            gameDirectory = file("run/client")
            client()
        }
        register("server") {
            gameDirectory = file("run/server")
            server()
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude(
            "**/fabric.mod.json", "**/*.accesswidener", "**/*.classtweaker", "**/mods.toml",
            "**/*.fabric.mixins.json"
        )
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

val localRuntime: Configuration = configurations.create("localRuntime")
configurations.runtimeClasspath { extendsFrom(localRuntime) }

dependencies {
    // YACL
    val yacl = "dev.isxander:yet-another-config-lib:${dep("yacl")}"
    compileOnly(yacl)
    localRuntime(yacl)

    // CodecUI
    optDep("codecui")?.let {
        implementation("net.mehvahdjukaar:codecui-neoforge:$it")
        "jarJar"("net.mehvahdjukaar:codecui-neoforge:$it")
    }

    // EMI
    optDep("emi")?.let { localRuntime("dev.emi:emi-neoforge:$it") }
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
            artifactId = "$modId-neoforge"
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
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} NeoForge $mcVersion - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        optional("yacl")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        optional("yacl")
        client = true
        server = true
    }
}
