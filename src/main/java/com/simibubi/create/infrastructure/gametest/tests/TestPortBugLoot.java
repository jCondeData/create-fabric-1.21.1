package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.tests.TestPortData.*;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * BUG (fails on 3ec980de36): piston extension poles and mechanical piston heads drop nothing. Both
 * blocks copy their properties from vanilla Blocks.PISTON_HEAD, which on Fabric also copies
 * PISTON_HEAD's "no loot table", so their generated loot tables are never used.
 */
@GameTestGroup(path = "qa")
public class TestPortBugLoot {

    /**
     * Every Create block whose loot table exists drops something when broken with a netherite
     * pickaxe (a loot table that fails to parse silently becomes empty).
     */
    @GameTest(template = "flat_7x6x7")
    public static void everyCreateBlockLootTableLoads(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    MinecraftServer server = helper.getLevel().getServer();
                    List<String> empty = new ArrayList<>();
                    int checked = 0;
                    for (Block block : BuiltInRegistries.BLOCK) {
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                        if (!id.getNamespace().equals(Create.ID)) continue;
                        ResourceLocation file =
                                ResourceLocation.fromNamespaceAndPath(
                                        Create.ID, "loot_table/blocks/" + id.getPath() + ".json");
                        if (server.getResourceManager().getResource(file).isEmpty()) continue;
                        checked++;
                        LootTable table =
                                server.reloadableRegistries().getLootTable(block.getLootTable());
                        if (table == LootTable.EMPTY) empty.add(id.getPath());
                    }
                    helper.assertTrue(empty.isEmpty(), "loot tables that failed to load: " + empty);
                    helper.assertTrue(
                            checked > 400, "only " + checked + " block loot tables found");
                    Create.LOGGER.info("[qa] {} Create block loot tables loaded", checked);
                    helper.succeed();
                });
    }

    /** A survival player breaking common Create blocks gets the block back. */
    @GameTest(template = "flat_7x6x7")
    public static void survivalPlayerGetsBlockDrops(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    List<BlockState> blocks =
                            List.of(
                                    AllBlocks.ANDESITE_CASING.getDefaultState(),
                                    AllBlocks.FLUID_TANK.getDefaultState(),
                                    AllBlocks.DEPOT.getDefaultState(),
                                    AllBlocks.PACKAGER.getDefaultState(),
                                    AllBlocks.MECHANICAL_PISTON.getDefaultState(),
                                    AllBlocks.PISTON_EXTENSION_POLE.getDefaultState(),
                                    AllBlocks.ZINC_ORE.getDefaultState(),
                                    AllBlocks.SPOUT.getDefaultState());
                    BlockPos pos = new BlockPos(2, 1, 2);
                    ServerPlayer player = helper.makeMockServerPlayerInLevel();
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    player.setItemInHand(
                            net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(Items.NETHERITE_PICKAXE));
                    for (BlockState state : blocks) {
                        helper.setBlock(pos, state);
                        var drops =
                                Block.getDrops(
                                        state,
                                        helper.getLevel(),
                                        helper.absolutePos(pos),
                                        helper.getLevel().getBlockEntity(helper.absolutePos(pos)),
                                        player,
                                        player.getMainHandItem());
                        Item expected =
                                state.is(AllBlocks.ZINC_ORE.get())
                                        ? AllItems.RAW_ZINC.get()
                                        : state.getBlock().asItem();
                        helper.assertTrue(
                                drops.stream().anyMatch(s -> s.is(expected)),
                                state.getBlock() + " dropped " + drops);
                        helper.setBlock(pos, Blocks.AIR);
                    }
                    // the piston head drops a pole (upstream: dropOther(PISTON_EXTENSION_POLE))
                    BlockState head = AllBlocks.MECHANICAL_PISTON_HEAD.getDefaultState();
                    var headDrops =
                            Block.getDrops(
                                    head,
                                    helper.getLevel(),
                                    helper.absolutePos(pos),
                                    null,
                                    player,
                                    player.getMainHandItem());
                    helper.assertTrue(
                            headDrops.stream()
                                    .anyMatch(s -> s.is(AllBlocks.PISTON_EXTENSION_POLE.asItem())),
                            "mechanical piston head dropped " + headDrops);
                    // 6.0.6 #7513: bound cardboard can be silk touched (without silk touch:
                    // cardboard + string)
                    ItemStack silk = new ItemStack(Items.NETHERITE_PICKAXE);
                    silk.enchant(
                            helper.getLevel()
                                    .registryAccess()
                                    .lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(
                                            net.minecraft.world.item.enchantment.Enchantments
                                                    .SILK_TOUCH),
                            1);
                    BlockState bound = AllBlocks.BOUND_CARDBOARD_BLOCK.getDefaultState();
                    var silkDrops =
                            Block.getDrops(
                                    bound,
                                    helper.getLevel(),
                                    helper.absolutePos(pos),
                                    null,
                                    player,
                                    silk);
                    helper.assertTrue(
                            silkDrops.stream().anyMatch(s -> s.is(bound.getBlock().asItem())),
                            "silk-touched bound cardboard dropped " + silkDrops);
                    helper.succeed();
                });
    }
}
