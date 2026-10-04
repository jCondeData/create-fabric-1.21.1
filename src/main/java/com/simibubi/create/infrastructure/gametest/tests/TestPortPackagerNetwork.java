package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.tests.TestPortLogistics.*;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tester round 3: the packager paths rounds 1-2 left out. Spec (upstream Create 6.0.10): a stock
 * link on a packager puts the packager's inventory on a logistics network, and a request on that
 * network makes the packager pack exactly the ordered items, addressed; a frogport sitting on a
 * packager takes each new package and sends it onto its chain conveyor. Nothing is lost or
 * duplicated on the way.
 */
@GameTestGroup(path = "qa")
public class TestPortPackagerNetwork {

    static long countAll(CreateGameTestHelper helper, List<BlockPos> blocks, Item item) {
        long n = countInEntities(helper, item);
        for (BlockPos pos : blocks)
            n += countInContainer(helper, pos, item) + countInPackager(helper, pos, item);
        return n;
    }

    /**
     * Stock link on top of a packager whose chest holds 8 gold and 4 diamonds: the network summary
     * shows them, and a request for 5 gold and 1 diamond to "Base" makes one package with exactly
     * that, addressed to "Base", leaving 3 gold and 3 diamonds in the chest.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void stockLinkRequestPacksExactlyTheOrder(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 1, 3);
        BlockPos packager = new BlockPos(2, 1, 2);
        BlockPos link = new BlockPos(2, 2, 2);
        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(
                helper, chest, new ItemStack(Items.GOLD_INGOT, 8), new ItemStack(Items.DIAMOND, 4));
        helper.setBlock(packager, packager(Direction.NORTH)); // target = chest to the south
        helper.setBlock(
                link,
                AllBlocks.STOCK_LINK
                        .getDefaultState()
                        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
        UUID freq = UUID.randomUUID();
        List<BlockPos> all = List.of(chest, packager);

        int[] phase = {0};
        helper.onEachTick(
                () -> {
                    if (phase[0] != 0) return;
                    if (!(helper.getBlockEntity(link) instanceof PackagerLinkBlockEntity be))
                        return;
                    be.behaviour.freqId = freq;
                    LogisticallyLinkedBehaviour.keepAlive(be.behaviour);
                    phase[0] = 1;
                });
        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertTrue(phase[0] > 0, "link not set up yet");
                    if (phase[0] == 1) {
                        var summary = LogisticsManager.getSummaryOfNetwork(freq, true);
                        int gold = summary.getCountOf(new ItemStack(Items.GOLD_INGOT));
                        int diamonds = summary.getCountOf(new ItemStack(Items.DIAMOND));
                        if (gold != 8 || diamonds != 4)
                            helper.fail(
                                    "network shows " + gold + " gold, " + diamonds + " diamonds");
                        boolean sent =
                                LogisticsManager.broadcastPackageRequest(
                                        freq,
                                        RequestType.PLAYER,
                                        PackageOrderWithCrafts.simple(
                                                List.of(
                                                        new BigItemStack(
                                                                new ItemStack(Items.GOLD_INGOT), 5),
                                                        new BigItemStack(
                                                                new ItemStack(Items.DIAMOND), 1))),
                                        null,
                                        "Base");
                        helper.assertTrue(sent, "request was refused");
                        phase[0] = 2;
                    }
                    long gold = countAll(helper, all, Items.GOLD_INGOT);
                    long diamonds = countAll(helper, all, Items.DIAMOND);
                    if (gold != 8 || diamonds != 4)
                        helper.fail(
                                gold + " gold and " + diamonds + " diamonds exist, 8 and 4 did");
                    helper.assertTrue(
                            countInContainer(helper, chest, Items.GOLD_INGOT) == 3
                                    && countInContainer(helper, chest, Items.DIAMOND) == 3,
                            "chest not down to 3 gold, 3 diamonds yet");
                    PackagerBlockEntity be = (PackagerBlockEntity) helper.getBlockEntity(packager);
                    List<ItemStack> boxes = new ArrayList<>();
                    if (!be.heldBox.isEmpty()) boxes.add(be.heldBox);
                    for (var big : be.queuedExitingPackages)
                        for (int i = 0; i < big.count; i++) boxes.add(big.stack);
                    helper.assertTrue(boxes.size() == 1, "packages in packager: " + boxes);
                    ItemStack box = boxes.get(0);
                    helper.assertTrue(
                            PackageItem.isPackage(box)
                                    && "Base".equals(PackageItem.getAddress(box))
                                    && countInStack(box, Items.GOLD_INGOT) == 5
                                    && countInStack(box, Items.DIAMOND) == 1,
                            "package " + box + " address " + PackageItem.getAddress(box));
                    Create.LOGGER.info(
                            "[qa] stock link request: package to Base with 5 gold, 1 diamond");
                },
                () ->
                        "phase "
                                + phase[0]
                                + " chest gold "
                                + countInContainer(helper, chest, Items.GOLD_INGOT)
                                + " diamonds "
                                + countInContainer(helper, chest, Items.DIAMOND)
                                + " packager gold "
                                + countInPackager(helper, packager, Items.GOLD_INGOT));
    }

    @SuppressWarnings("unchecked")
    static List<ChainConveyorPackage> loopingPackages(ChainConveyorBlockEntity chain) {
        try {
            Field f = ChainConveyorBlockEntity.class.getDeclaredField("loopingPackages");
            f.setAccessible(true);
            return (List<ChainConveyorPackage>) f.get(chain);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A frogport sitting on a packager, targeting a turning chain conveyor: on a redstone pulse the
     * packager packs its chest (8 gold), the frogport takes the package out of the packager and
     * puts it on the chain, where it loops with all 8 gold inside; nothing stays behind.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void frogportTakesPackageFromPackagerOntoChain(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 3);
        BlockPos packager = new BlockPos(1, 1, 2);
        BlockPos port = new BlockPos(1, 2, 2);
        BlockPos motor = new BlockPos(4, 1, 2);
        BlockPos chain = new BlockPos(4, 2, 2);
        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(helper, chest, new ItemStack(Items.GOLD_INGOT, 8));
        helper.setBlock(packager, packager(Direction.NORTH)); // target = chest to the south
        helper.setBlock(port, AllBlocks.PACKAGE_FROGPORT.getDefaultState());
        helper.setBlock(
                motor,
                AllBlocks.CREATIVE_MOTOR
                        .getDefaultState()
                        .setValue(CreativeMotorBlock.FACING, Direction.UP));
        helper.setBlock(chain, AllBlocks.CHAIN_CONVEYOR.getDefaultState());
        FrogportBlockEntity frog = (FrogportBlockEntity) helper.getBlockEntity(port);
        frog.target =
                new PackagePortTarget.ChainConveyorFrogportTarget(
                        chain.subtract(port), 0f, (BlockPos) null, false);
        frog.notifyUpdate();
        helper.runAfterDelay(
                20, () -> helper.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK));

        List<BlockPos> blocks = List.of(chest, packager);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    ChainConveyorBlockEntity conveyor =
                            (ChainConveyorBlockEntity) helper.getBlockEntity(chain);
                    helper.assertTrue(conveyor.getSpeed() != 0, "chain conveyor not turning");
                    List<ChainConveyorPackage> looping = loopingPackages(conveyor);
                    long onChain = 0;
                    for (ChainConveyorPackage p : looping)
                        onChain += countInStack(p.item, Items.GOLD_INGOT);
                    long elsewhere = countAll(helper, blocks, Items.GOLD_INGOT);
                    if (onChain + elsewhere > 8)
                        helper.fail(onChain + " gold on the chain + " + elsewhere + " elsewhere");
                    helper.assertTrue(
                            looping.size() == 1 && onChain == 8 && elsewhere == 0,
                            "chain has "
                                    + looping.size()
                                    + " packages with "
                                    + onChain
                                    + " gold, "
                                    + elsewhere
                                    + " gold elsewhere");
                    Create.LOGGER.info("[qa] frogport moved the packager's package onto the chain");
                },
                () ->
                        "chest "
                                + countInContainer(helper, chest, Items.GOLD_INGOT)
                                + " packager "
                                + countInPackager(helper, packager, Items.GOLD_INGOT)
                                + " frog target "
                                + frog.target
                                + " entities "
                                + countInEntities(helper, Items.GOLD_INGOT));
    }
}
