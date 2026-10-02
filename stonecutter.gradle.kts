plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.18.2" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
    id("org.moddedmc.wiki.toolkit") version "0.4.1"
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    val loader = node.metadata.project.substringAfterLast('-')
    constants.match(loader, "fabric", "neoforge")
    fun rename(from: String, to: String) = listOf("(?<!\")\\b$from\\b(?!\")", to, "(?<!\")\\b$to\\b(?!\")", from)
    replacements {
        regex(eval(node.metadata.version, ">=26.1")) {
            for ((a, b) in listOf(
                "ResourceLocation" to "Identifier",
                "ResourceLocationArgument" to "IdentifierArgument",
                "GuiGraphics" to "GuiGraphicsExtractor",
                "readResourceLocation" to "readIdentifier",
                "writeResourceLocation" to "writeIdentifier",
            )) {
                val (p1, r1, p2, r2) = rename(a, b)
                replace(p1, r1, p2, r2)
            }
            replace(
                "net\\.minecraft\\.advancements\\.critereon\\b", "net.minecraft.advancements.criterion",
                "net\\.minecraft\\.advancements\\.criterion\\b", "net.minecraft.advancements.critereon"
            )
            for ((a, b) in listOf(
                "render" to "extractRenderState",
                "renderWidget" to "extractWidgetRenderState",
                "renderBackground" to "extractBackground",
            )) {
                replace("\\b$a\\(", "$b(", "\\b$b\\(", "$a(")
            }
            fun gfx(from: String, to: String) = replace(
                "(?<=\\b(?:gfx|guiGraphics)\\.)$from\\(", "$to(",
                "(?<=\\b(?:gfx|guiGraphics)\\.)$to\\(", "$from("
            )
            gfx("drawCenteredString", "centeredText")
            gfx("renderOutline", "outline")
            gfx("renderFakeItem", "fakeItem")
            gfx("renderItem", "item")
            gfx("renderTooltip", "setTooltipForNextFrame")
            replace(
                "(?<=\\bBuiltInRegistries\\.\\w{1,40})\\.get\\(", ".getValue(",
                "(?<=\\bBuiltInRegistries\\.\\w{1,40})\\.getValue\\(", ".get("
            )
            replace("(?<!\\btag)\\.location\\(\\)", ".identifier()", "\\.identifier\\(\\)", ".location()")
        }
        string(eval(node.metadata.version, ">=26.1")) {
            replace(
                "player.hasPermissions(2)",
                "player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)"
            )
            replace(".flushDirty(player)", ".flushDirty(player, true)")
            replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")
            replace("PayloadTypeRegistry.playC2S()", "PayloadTypeRegistry.serverboundPlay()")
            replace("PayloadTypeRegistry.playS2C()", "PayloadTypeRegistry.clientboundPlay()")
            replace("Screens.getButtons(", "Screens.getWidgets(")
        }
        regex(eval(node.metadata.version, ">=26.2")) {
            replace("(?<!\\.gui)\\.setScreen\\(", ".gui.setScreen(", "\\.gui\\.setScreen\\(", ".setScreen(")
            replace(
                "(?<=\\b(?:minecraft|mc|getInstance\\(\\)))\\.screen\\b(?!\\()", ".gui.screen()",
                "\\.gui\\.screen\\(\\)", ".screen"
            )
        }
        string(eval(node.metadata.version, ">=26.2")) {
            replace("import net.minecraft.advancements.CriterionTrigger;", "import net.minecraft.advancements.triggers.CriterionTrigger;")
        }
        regex(eval(node.metadata.version, ">=26.3")) {
            replace(
                "(?<=\\b(?:clientAdvancements|conn\\.getAdvancements\\(\\)))\\.getTree\\(\\)", ".tree()",
                "(?<=\\b(?:clientAdvancements|conn\\.getAdvancements\\(\\)))\\.tree\\(\\)", ".getTree()"
            )
        }
        string(eval(node.metadata.version, ">=26.1")) {
            val mouse = "net.minecraft.client.input.MouseButtonEvent event"
            val unpack = "double mouseX = event.x(), mouseY = event.y(); int button = event.button();"
            replace(
                "boolean mouseClicked(double mouseX, double mouseY, int button) {",
                "boolean mouseClicked($mouse, boolean doubleClick) { $unpack"
            )
            replace(".mouseClicked(mouseX, mouseY, button)", ".mouseClicked(event, doubleClick)")
            replace(
                "boolean mouseReleased(double mouseX, double mouseY, int button) {",
                "boolean mouseReleased($mouse) { $unpack"
            )
            replace(".mouseReleased(mouseX, mouseY, button)", ".mouseReleased(event)")
            replace(
                "boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {",
                "boolean mouseDragged($mouse, double dragX, double dragY) { $unpack"
            )
            replace(".mouseDragged(mouseX, mouseY, button, dragX, dragY)", ".mouseDragged(event, dragX, dragY)")
            replace(
                "void onClick(double mouseX, double mouseY) {",
                "void onClick($mouse, boolean doubleClick) { double mouseX = event.x(), mouseY = event.y();"
            )
            replace(
                "boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
                "boolean keyPressed(net.minecraft.client.input.KeyEvent event) { int keyCode = event.key(), modifiers = event.modifiers();"
            )
            replace(".keyPressed(keyCode, scanCode, modifiers)", ".keyPressed(event)")
            replace(
                "boolean charTyped(char codePoint, int modifiers) {",
                "boolean charTyped(net.minecraft.client.input.CharacterEvent event) { char codePoint = (char) event.codepoint();"
            )
            replace("void onPress() {", "void onPress(net.minecraft.client.input.InputWithModifiers input) {")
        }
    }
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

wiki {
    wikiAccessToken = System.getenv("WIKI_ACCESS_TOKEN")
    docs.create("reliable-advancements") {
        root = file("docs/")
    }
}
