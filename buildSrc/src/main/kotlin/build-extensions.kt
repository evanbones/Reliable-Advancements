import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import java.io.File

/**
 * Extra resource roots under `src/main/overlays/<dir>`, added when the stonecutter predicate matches the current version.
 */
val RESOURCE_OVERLAYS: Map<String, String> = linkedMapOf(
    "pre-26.1" to "<26.1",
    "26.x" to ">=26.1",
)

/**
 * Name of the Fabric access widener for the current version, relative to the resource root.
 */
fun accessWidenerName(modId: String, unobfuscated: Boolean) =
    if (unobfuscated) "$modId.classtweaker" else "$modId.accesswidener"

/**
 * @param localMaven repo-local maven folder for locally built dependencies
 */
fun RepositoryHandler.reliableAdvancementsRepositories(localMaven: File) {
    mavenLocal()
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com/releases/") {
        name = "Terraformers (Mod Menu, EMI)"
        content {
            includeGroupAndSubgroups("com.terraformersmc")
            includeGroupAndSubgroups("dev.emi")
        }
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven (YACL)"
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "Quilt Maven"
        content { includeGroupAndSubgroups("org.quiltmc.parsers") }
    }
    maven("https://maven.cassian.cc/") {
        name = "Cassian's Maven"
        content { includeGroupAndSubgroups("cc.cassian") }
    }
    maven(localMaven) {
        name = "Local libs (CodecUI ports)"
        content { includeGroupAndSubgroups("net.mehvahdjukaar") }
    }
    maven("https://registry.somethingcatchy.net/repository/maven-public/") {
        name = "SomethingCatchy (CodecUI)"
        content { includeGroupAndSubgroups("net.mehvahdjukaar") }
    }
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
        content { includeGroupAndSubgroups("org.parchmentmc") }
    }
}
