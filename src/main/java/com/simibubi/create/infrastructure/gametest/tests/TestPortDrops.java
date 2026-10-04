package com.simibubi.create.infrastructure.gametest.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Round-2 QA: block drops through the player's real entry points (a survival or creative player
 * breaking blocks, explosions, a real piston extension), and the sounds.json contents.
 *
 * <p>Spec: drops behave like upstream NeoForge Create 6.0.10. Upstream's Registrate copies a
 * vanilla block's properties with ofFullCopy and then resets the copied loot table, so every Create
 * block uses its own loot table create:blocks/&lt;id&gt; unless it says noLootTable itself (only
 * the crushing wheel controller does).
 */
@GameTestGroup(path = "qa")
public class TestPortDrops {

    static FakePlayer player(CreateGameTestHelper helper, GameType mode, ItemStack tool) {
        FakePlayer p =
                FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "qa-breaker"));
        p.setGameMode(mode);
        p.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return p;
    }

    static long droppedItems(CreateGameTestHelper helper, Item item) {
        long n = 0;
        for (ItemEntity e :
                helper.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(3)))
            if (e.getItem().is(item)) n += e.getItem().getCount();
        return n;
    }

    static long droppedItemsTotal(CreateGameTestHelper helper) {
        long n = 0;
        for (ItemEntity e :
                helper.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(3)))
            n += e.getItem().getCount();
        return n;
    }

    /**
     * Every registered Create block uses its own loot table (the one upstream generates), so no
     * block silently inherits "no drops" or another block's drops from the vanilla block it copies
     * its properties from. Only the crushing wheel controller has no loot table (as upstream).
     */
    @GameTest(template = "flat_7x6x7")
    public static void everyCreateBlockUsesItsOwnLootTable(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    List<String> wrong = new ArrayList<>();
                    int checked = 0;
                    MinecraftServer server = helper.getLevel().getServer();
                    for (Block block : BuiltInRegistries.BLOCK) {
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                        if (!id.getNamespace().equals(Create.ID)) continue;
                        checked++;
                        ResourceKey<LootTable> own =
                                ResourceKey.create(Registries.LOOT_TABLE, id.withPrefix("blocks/"));
                        ResourceKey<LootTable> actual = block.getLootTable();
                        boolean hasFile =
                                server.getResourceManager()
                                        .getResource(
                                                id.withPath(
                                                        "loot_table/blocks/"
                                                                + id.getPath()
                                                                + ".json"))
                                        .isPresent();
                        if (block == AllBlocks.CRUSHING_WHEEL_CONTROLLER.get()) {
                            if (!actual.equals(BuiltInLootTables.EMPTY))
                                wrong.add(id.getPath() + " -> " + actual.location());
                        } else if (hasFile) {
                            // upstream generates this table: the block must use it
                            if (!actual.equals(own))
                                wrong.add(id.getPath() + " -> " + actual.location());
                        } else if (server.reloadableRegistries().getLootTable(actual)
                                != LootTable.EMPTY) {
                            // no table of its own (fluids, technical blocks): drops nothing
                            // upstream, so it must not borrow another block's drops
                            wrong.add(id.getPath() + " -> " + actual.location());
                        }
                    }
                    helper.assertTrue(
                            wrong.isEmpty(),
                            wrong.size() + " Create blocks use the wrong loot table: " + wrong);
                    helper.assertTrue(checked > 600, "only " + checked + " Create blocks");
                    Create.LOGGER.info("[qa] {} Create blocks use their own loot table", checked);
                    helper.succeed();
                });
    }

    /**
     * A survival player breaking a lone extension pole, a lone piston head and a pole with silk
     * touch gets a pole back each time; a creative player gets nothing.
     */
    @GameTest(template = "flat_7x6x7")
    public static void playersBreakingPolesAndHeads(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    Item pole = AllBlocks.PISTON_EXTENSION_POLE.asItem();
                    BlockPos pos = new BlockPos(3, 2, 3);
                    ItemStack silkPick = new ItemStack(Items.IRON_PICKAXE);
                    silkPick.enchant(
                            helper.getLevel()
                                    .registryAccess()
                                    .lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.SILK_TOUCH),
                            1);
                    FakePlayer survival =
                            player(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_PICKAXE));
                    FakePlayer silk = player(helper, GameType.SURVIVAL, silkPick);
                    FakePlayer creative =
                            player(helper, GameType.CREATIVE, new ItemStack(Items.IRON_PICKAXE));
                    helper.assertTrue(
                            !survival.isCreative() && creative.isCreative(), "game modes");

                    BlockState[] states = {
                        AllBlocks.PISTON_EXTENSION_POLE.getDefaultState(),
                        AllBlocks.MECHANICAL_PISTON_HEAD.getDefaultState(),
                        AllBlocks.PISTON_EXTENSION_POLE.getDefaultState()
                    };
                    FakePlayer[] breakers = {survival, survival, silk};
                    long expected = 0;
                    for (int i = 0; i < states.length; i++) {
                        helper.setBlock(pos, states[i]);
                        boolean broken = breakers[i].gameMode.destroyBlock(helper.absolutePos(pos));
                        helper.assertTrue(broken, "could not break " + states[i]);
                        helper.assertBlockPresent(Blocks.AIR, pos);
                        expected++;
                        long got = droppedItems(helper, pole);
                        helper.assertTrue(
                                got == expected,
                                "after breaking "
                                        + states[i].getBlock()
                                        + " with "
                                        + breakers[i].getMainHandItem()
                                        + ": "
                                        + got
                                        + " poles dropped, expected "
                                        + expected);
                    }
                    for (BlockState state : new BlockState[] {states[0], states[1]}) {
                        helper.setBlock(pos, state);
                        creative.gameMode.destroyBlock(helper.absolutePos(pos));
                        helper.assertBlockPresent(Blocks.AIR, pos);
                    }
                    helper.assertTrue(
                            droppedItems(helper, pole) == expected,
                            "a creative player got drops: " + droppedItems(helper, pole));
                    Create.LOGGER.info(
                            "[qa] survival breaks dropped {} poles, creative none", expected);
                    helper.succeed();
                });
    }

    /**
     * TNT-style explosions (no drop decay by default) drop the pole and the head's pole, as
     * upstream's survives_explosion loot tables do.
     */
    @GameTest(template = "flat_15x6x15")
    public static void explodedPolesAndHeadsDropPoles(CreateGameTestHelper helper) {
        BlockPos polePos = new BlockPos(4, 2, 7);
        BlockPos headPos = new BlockPos(10, 2, 7);
        helper.setBlock(polePos, AllBlocks.PISTON_EXTENSION_POLE.getDefaultState());
        helper.setBlock(headPos, AllBlocks.MECHANICAL_PISTON_HEAD.getDefaultState());
        helper.runAfterDelay(
                2,
                () -> { // qa-wrap: assertion messages survive
                    for (BlockPos pos : new BlockPos[] {polePos, headPos}) {
                        BlockPos abs = helper.absolutePos(pos);
                        helper.getLevel()
                                .explode(
                                        null,
                                        abs.getX() + 0.5,
                                        abs.getY() + 0.5,
                                        abs.getZ() + 0.5,
                                        2f,
                                        Level.ExplosionInteraction.TNT);
                    }
                });
        helper.runAfterDelay(
                4,
                () -> {
                    helper.assertBlockPresent(Blocks.AIR, polePos);
                    helper.assertBlockPresent(Blocks.AIR, headPos);
                    long poles = droppedItems(helper, AllBlocks.PISTON_EXTENSION_POLE.asItem());
                    helper.assertTrue(
                            poles == 2, "explosions dropped " + poles + " poles, expected 2");
                    Create.LOGGER.info("[qa] exploded pole + head dropped {} poles", poles);
                    helper.succeed();
                });
    }

    /**
     * A real mechanical piston with two poles behind it: a creative motor turns it, it assembles,
     * extends two blocks and disassembles at full extension as base, pole, head (nothing lost or
     * dropped). Then a survival player breaks the pole of the extended piston: the head breaks with
     * it, the player gets both poles back (the head drops a pole) and the piston is retracted.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.FIFTEEN_SECONDS)
    public static void survivalPlayerBreaksExtendedPistonPole(CreateGameTestHelper helper) {
        extendThenBreakPole(helper, GameType.SURVIVAL, 2);
    }

    /** As above, but a creative player gets nothing back. */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.FIFTEEN_SECONDS)
    public static void creativePlayerBreaksExtendedPistonPole(CreateGameTestHelper helper) {
        extendThenBreakPole(helper, GameType.CREATIVE, 0);
    }

    private static void extendThenBreakPole(
            CreateGameTestHelper helper, GameType mode, int expectedPoles) {
        BlockPos base = new BlockPos(3, 2, 4);
        // the motor on the east side, facing west, turns the piston the extending way
        BlockPos motor = new BlockPos(4, 2, 4);
        Item poleItem = AllBlocks.PISTON_EXTENSION_POLE.asItem();
        helper.setBlock(
                base,
                AllBlocks.MECHANICAL_PISTON
                        .getDefaultState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                        .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        for (int z = 5; z <= 6; z++)
            helper.setBlock(
                    new BlockPos(3, 2, z),
                    AllBlocks.PISTON_EXTENSION_POLE
                            .getDefaultState()
                            .setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.runAfterDelay(
                2,
                () ->
                        helper.setBlock(
                                motor,
                                AllBlocks.CREATIVE_MOTOR
                                        .getDefaultState()
                                        .setValue(CreativeMotorBlock.FACING, Direction.WEST)));

        BlockPos pole1 = new BlockPos(3, 2, 3);
        BlockPos head = new BlockPos(3, 2, 2);
        boolean[] broke = {false};
        helper.succeedWhenWithDiagnostics(
                () -> {
                    if (!broke[0]) {
                        // wait for the extension to finish and the contraption to disassemble
                        BlockState baseState = helper.getBlockState(base);
                        helper.assertTrue(
                                baseState.getValue(MechanicalPistonBlock.STATE)
                                        == PistonState.EXTENDED,
                                "piston not extended yet");
                        helper.assertBlockPresent(AllBlocks.PISTON_EXTENSION_POLE.get(), pole1);
                        helper.assertBlockPresent(AllBlocks.MECHANICAL_PISTON_HEAD.get(), head);
                        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 1));
                        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 5));
                        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 6));
                        helper.assertTrue(
                                droppedItemsTotal(helper) == 0,
                                "extending the piston dropped items");
                        // the extension is done: stop the motor and break the first pole
                        helper.setBlock(motor, Blocks.AIR);
                        FakePlayer breaker =
                                player(helper, mode, new ItemStack(Items.IRON_PICKAXE));
                        breaker.gameMode.destroyBlock(helper.absolutePos(pole1));
                        broke[0] = true;
                        Create.LOGGER.info(
                                "[qa] piston extended (base, pole, head); {} player broke the pole",
                                mode);
                    }
                    helper.assertBlockPresent(Blocks.AIR, pole1);
                    helper.assertBlockPresent(Blocks.AIR, head);
                    helper.assertTrue(
                            helper.getBlockState(base).getValue(MechanicalPistonBlock.STATE)
                                    == PistonState.RETRACTED,
                            "piston base should be retracted");
                    long poles = droppedItems(helper, poleItem);
                    helper.assertTrue(
                            poles == expectedPoles,
                            mode + " player got " + poles + " poles, expected " + expectedPoles);
                    helper.assertTrue(
                            droppedItemsTotal(helper) == expectedPoles,
                            "other items dropped: " + droppedItemsTotal(helper));
                    Create.LOGGER.info("[qa] {} player got {} poles back", mode, poles);
                },
                () -> {
                    String d = helper.snapshot(base, pole1, head);
                    return d.length() > 700 ? d.substring(0, 700) : d;
                });
    }

    /**
     * Every sound file sounds.json points at is shipped in the mod (a missing .ogg is silent and
     * logs "File ... does not exist"), and every subtitle it names is translated.
     */
    @GameTest(template = "flat_7x6x7")
    public static void soundsJsonPointsAtShippedFilesAndTranslatedSubtitles(
            CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    var mod = FabricLoader.getInstance().getModContainer(Create.ID).orElseThrow();
                    Optional<Path> file = mod.findPath("assets/create/sounds.json");
                    helper.assertTrue(file.isPresent(), "assets/create/sounds.json is missing");
                    JsonObject sounds;
                    try (Reader reader = Files.newBufferedReader(file.get())) {
                        sounds = JsonParser.parseReader(reader).getAsJsonObject();
                    } catch (Exception e) {
                        helper.fail("sounds.json unreadable: " + e);
                        return;
                    }
                    List<String> missingFiles = new ArrayList<>();
                    List<String> untranslated = new ArrayList<>();
                    List<String> unknownEvents = new ArrayList<>();
                    int files = 0;
                    for (Map.Entry<String, JsonElement> entry : sounds.entrySet()) {
                        JsonObject def = entry.getValue().getAsJsonObject();
                        if (!BuiltInRegistries.SOUND_EVENT.containsKey(
                                ResourceLocation.fromNamespaceAndPath(Create.ID, entry.getKey())))
                            unknownEvents.add(entry.getKey());
                        if (def.has("subtitle")
                                && !Language.getInstance().has(def.get("subtitle").getAsString()))
                            untranslated.add(def.get("subtitle").getAsString());
                        for (JsonElement s : def.getAsJsonArray("sounds")) {
                            String name;
                            String type = "file";
                            if (s.isJsonObject()) {
                                name = s.getAsJsonObject().get("name").getAsString();
                                if (s.getAsJsonObject().has("type"))
                                    type = s.getAsJsonObject().get("type").getAsString();
                            } else name = s.getAsString();
                            ResourceLocation loc = ResourceLocation.parse(name);
                            if (type.equals("event")) {
                                if (!BuiltInRegistries.SOUND_EVENT.containsKey(loc))
                                    unknownEvents.add(entry.getKey() + " -> event " + loc);
                                continue;
                            }
                            files++;
                            String path =
                                    "assets/"
                                            + loc.getNamespace()
                                            + "/sounds/"
                                            + loc.getPath()
                                            + ".ogg";
                            boolean shipped =
                                    loc.getNamespace().equals(Create.ID)
                                            ? mod.findPath(path).isPresent()
                                            : true; // vanilla sounds come from the client assets
                            if (!shipped) missingFiles.add(entry.getKey() + " -> " + path);
                        }
                    }
                    helper.assertTrue(
                            missingFiles.isEmpty(), "sound files not shipped: " + missingFiles);
                    helper.assertTrue(
                            untranslated.isEmpty(), "subtitles not translated: " + untranslated);
                    helper.assertTrue(
                            unknownEvents.isEmpty(),
                            "sounds.json names unregistered events: " + unknownEvents);
                    helper.assertTrue(
                            sounds.size() > 70 && files > 30,
                            "only " + sounds.size() + " events / " + files + " files");
                    Create.LOGGER.info(
                            "[qa] sounds.json: {} events, {} shipped files, all subtitles"
                                    + " translated",
                            sounds.size(),
                            files);
                    helper.succeed();
                });
    }
}
