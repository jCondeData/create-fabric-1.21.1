package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.infrastructure.fabric.transfer.TransferUtil;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;

/**
 * Fluids on Fabric are droplets (81000 = 1 bucket = 1000 mB upstream; a bottle is 27000, the Fabric
 * bottle). Covers spouts, item drains, tank capacity and the FluidStack codecs, including potion
 * fluids whose data components must survive every hop (upstream 6.0.9 "Fix potion-based fluid
 * recipes #9563", 6.0.7 "contraptions emptying a tank with potions causing a network disconnect
 * #9074").
 */
@GameTestGroup(path = "qa")
public class TestPortFluids {

    static long insertFluid(Storage<FluidVariant> storage, FluidStack stack) {
        try (Transaction t = Transaction.openOuter()) {
            long inserted = storage.insert(stack.getVariant(), stack.getAmount(), t);
            t.commit();
            return inserted;
        }
    }

    static long insertItem(Storage<ItemVariant> storage, ItemStack stack) {
        try (Transaction t = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), t);
            t.commit();
            return inserted;
        }
    }

    static ItemStack firstStack(Storage<ItemVariant> storage) {
        for (StorageView<ItemVariant> view : storage.nonEmptyViews())
            return view.getResource().toStack((int) view.getAmount());
        return ItemStack.EMPTY;
    }

    static FluidStack potion(
            long amount,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return PotionFluid.of(amount, new PotionContents(potion), BottleType.REGULAR);
    }

    /** A spout full of healing potion fills a glass bottle on a depot with a healing potion. */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void spoutFillsGlassBottleWithPotion(CreateGameTestHelper helper) {
        BlockPos depot = new BlockPos(2, 1, 2);
        BlockPos spout = new BlockPos(2, 3, 2);
        helper.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
        helper.setBlock(spout, AllBlocks.SPOUT.getDefaultState());
        long in =
                insertFluid(
                        helper.fluidStorageAt(spout),
                        potion(FluidConstants.BUCKET, Potions.HEALING));
        helper.assertTrue(in == FluidConstants.BUCKET, "spout accepted " + in + " of 81000");
        insertItem(helper.itemStorageAt(depot), new ItemStack(Items.GLASS_BOTTLE));

        helper.succeedWhenWithDiagnostics(
                () -> {
                    ItemStack held = firstStack(helper.itemStorageAt(depot));
                    helper.assertTrue(held.is(Items.POTION), "depot holds " + held);
                    PotionContents contents = held.get(DataComponents.POTION_CONTENTS);
                    helper.assertTrue(
                            contents != null && contents.is(Potions.HEALING),
                            "filled potion is " + contents);
                    long left = helper.getTankContents(spout).getAmount();
                    helper.assertTrue(
                            left == FluidConstants.BUCKET - FluidConstants.BOTTLE,
                            "spout should have used one bottle (27000), left " + left);
                    Create.LOGGER.info(
                            "[qa] spout filled a healing potion, {} droplets left", left);
                },
                () ->
                        "depot: "
                                + firstStack(helper.itemStorageAt(depot))
                                + ", spout: "
                                + helper.getTankContents(spout).getAmount());
    }

    /**
     * Glowstone dust is made by spouting night vision potion onto cinder flour. A different potion
     * must not work (6.0.9 #9563: potion fluid recipes must check the potion).
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void spoutPotionRecipeChecksThePotion(CreateGameTestHelper helper) {
        BlockPos depotA = new BlockPos(1, 1, 2);
        BlockPos spoutA = new BlockPos(1, 3, 2);
        BlockPos depotB = new BlockPos(4, 1, 2);
        BlockPos spoutB = new BlockPos(4, 3, 2);
        for (BlockPos d : new BlockPos[] {depotA, depotB})
            helper.setBlock(d, AllBlocks.DEPOT.getDefaultState());
        for (BlockPos s : new BlockPos[] {spoutA, spoutB})
            helper.setBlock(s, AllBlocks.SPOUT.getDefaultState());
        insertFluid(
                helper.fluidStorageAt(spoutA), potion(FluidConstants.BUCKET, Potions.NIGHT_VISION));
        insertFluid(
                helper.fluidStorageAt(spoutB), potion(FluidConstants.BUCKET, Potions.SWIFTNESS));
        insertItem(helper.itemStorageAt(depotA), new ItemStack(AllItems.CINDER_FLOUR.get()));
        insertItem(helper.itemStorageAt(depotB), new ItemStack(AllItems.CINDER_FLOUR.get()));

        helper.runAfterDelay(
                120,
                () -> {
                    ItemStack a = firstStack(helper.itemStorageAt(depotA));
                    ItemStack b = firstStack(helper.itemStorageAt(depotB));
                    helper.assertTrue(
                            a.is(Items.GLOWSTONE_DUST), "night vision + cinder flour gave " + a);
                    helper.assertTrue(
                            b.is(AllItems.CINDER_FLOUR.get()),
                            "swiftness + cinder flour should do nothing, gave " + b);
                    long usedA = FluidConstants.BUCKET - helper.getTankContents(spoutA).getAmount();
                    helper.assertTrue(
                            usedA == 2025, "glowstone uses 25 mB = 2025 droplets, used " + usedA);
                    helper.assertTrue(
                            helper.getTankContents(spoutB).getAmount() == FluidConstants.BUCKET,
                            "swiftness spout should not have spent fluid");
                    Create.LOGGER.info("[qa] potion recipe matched only the night vision potion");
                    helper.succeed();
                });
    }

    /** An item drain empties a potion into its tank: one bottle = 27000 droplets, contents kept. */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void drainEmptiesPotionKeepingItsEffect(CreateGameTestHelper helper) {
        BlockPos drain = new BlockPos(2, 2, 2); // y=1 is the floor layer; the exit must be open
        helper.setBlock(drain, AllBlocks.ITEM_DRAIN.getDefaultState());
        Storage<ItemVariant> side =
                ItemStorage.SIDED.find(
                        helper.getLevel(), helper.absolutePos(drain), Direction.NORTH);
        helper.assertTrue(side != null, "item drain exposes no item storage on its side");
        ItemStack strength = new ItemStack(Items.POTION);
        strength.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));
        long in = insertItem(side, strength);
        helper.assertTrue(in == 1, "drain refused the potion");

        helper.succeedWhenWithDiagnostics(
                () -> {
                    FluidStack contained = helper.getTankContents(drain);
                    FluidStack expected = potion(FluidConstants.BOTTLE, Potions.STRENGTH);
                    helper.assertTrue(
                            FluidStack.isSameFluidSameComponents(contained, expected),
                            "drain holds "
                                    + contained.getVariant()
                                    + " "
                                    + contained.getComponentsPatch());
                    helper.assertTrue(
                            contained.getAmount() == FluidConstants.BOTTLE,
                            "drain holds " + contained.getAmount() + " droplets, expected 27000");
                    // the emptied bottle rolls on and is ejected on the far side (nothing there)
                    long ejected = TestPortLogistics.countInEntities(helper, Items.GLASS_BOTTLE);
                    long inDrain = TestPortLogistics.countIn(side, Items.GLASS_BOTTLE);
                    helper.assertTrue(
                            ejected == 1 && inDrain == 0,
                            "emptied glass bottle: " + ejected + " ejected, " + inDrain + " in the drain");
                    Create.LOGGER.info("[qa] drained strength potion: {}", contained.getAmount());
                },
                () ->
                        "tank: "
                                + helper.getTankContents(drain).getVariant()
                                + " x "
                                + helper.getTankContents(drain).getAmount());
    }

    /** A single fluid tank holds fluidTankCapacity (default 8) buckets, and refuses the rest. */
    @GameTest(template = "flat_7x6x7")
    public static void fluidTankHoldsEightBucketsPerBlock(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    BlockPos tank = new BlockPos(2, 1, 2);
                    helper.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
                    long accepted =
                            insertFluid(
                                    helper.fluidStorageAt(tank),
                                    new FluidStack(Fluids.WATER, 10 * FluidConstants.BUCKET));
                    helper.assertTrue(
                            accepted == 8 * FluidConstants.BUCKET,
                            "tank accepted " + accepted + " droplets, expected 8 buckets = 648000");
                    helper.assertTrue(
                            helper.getTankCapacity(tank) == 8 * FluidConstants.BUCKET,
                            "capacity " + helper.getTankCapacity(tank));
                    // and it gives exactly that back
                    try (Transaction t = Transaction.openOuter()) {
                        long out =
                                helper.fluidStorageAt(tank)
                                        .extract(
                                                FluidVariant.of(Fluids.WATER),
                                                100 * FluidConstants.BUCKET,
                                                t);
                        helper.assertTrue(out == 8 * FluidConstants.BUCKET, "extracted " + out);
                    }
                    Create.LOGGER.info("[qa] tank capacity {}", accepted);
                    helper.succeed();
                });
    }

    /**
     * FluidStack writes the Fabric format and reads NeoForge's ({id, amount in mB}) converting to
     * droplets; potion components survive NBT and the network codec.
     */
    @GameTest(template = "flat_7x6x7")
    public static void fluidStackCodecsKeepAmountsAndPotions(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    var registries = helper.getLevel().registryAccess();
                    var ops = registries.createSerializationContext(NbtOps.INSTANCE);

                    FluidStack potion = potion(12345, Potions.LONG_FIRE_RESISTANCE);
                    Tag saved = FluidStack.CODEC.encodeStart(ops, potion).getOrThrow();
                    FluidStack loaded = FluidStack.CODEC.parse(ops, saved).getOrThrow();
                    helper.assertTrue(
                            FluidStack.isSameFluidSameComponents(potion, loaded)
                                    && loaded.getAmount() == 12345,
                            "NBT round trip changed the stack: " + saved);

                    CompoundTag neo = new CompoundTag();
                    neo.putString("id", "minecraft:water");
                    neo.putInt("amount", 1000);
                    FluidStack fromNeo = FluidStack.parseOptional(registries, neo);
                    helper.assertTrue(
                            fromNeo.getFluid() == Fluids.WATER
                                    && fromNeo.getAmount() == FluidConstants.BUCKET,
                            "NeoForge {id:water, amount:1000} read as "
                                    + fromNeo.getFluid()
                                    + " x "
                                    + fromNeo.getAmount());

                    RegistryFriendlyByteBuf buf =
                            new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
                    FluidStack.STREAM_CODEC.encode(buf, potion);
                    FluidStack.STREAM_CODEC.encode(buf, FluidStack.EMPTY);
                    FluidStack.STREAM_CODEC.encode(buf, new FluidStack(Fluids.WATER, 1));
                    FluidStack net = FluidStack.STREAM_CODEC.decode(buf);
                    FluidStack netEmpty = FluidStack.STREAM_CODEC.decode(buf);
                    FluidStack oneDroplet = FluidStack.STREAM_CODEC.decode(buf);
                    helper.assertTrue(
                            oneDroplet.getFluid() == Fluids.WATER && oneDroplet.getAmount() == 1,
                            "a 1-droplet stack came back from the network as "
                                    + oneDroplet.getAmount());
                    helper.assertTrue(
                            FluidStack.isSameFluidSameComponents(potion, net)
                                    && net.getAmount() == 12345,
                            "network round trip changed the potion fluid");
                    helper.assertTrue(
                            netEmpty.isEmpty() && buf.readableBytes() == 0,
                            "empty stack / leftover bytes");

                    FluidStack emptyLoaded =
                            FluidStack.parseOptional(registries, new CompoundTag());
                    helper.assertTrue(emptyLoaded.isEmpty(), "empty tag should load as EMPTY");
                    Create.LOGGER.info("[qa] FluidStack codecs ok: {}", saved);
                    helper.succeed();
                });
    }

    /** Sanity: TransferUtil sees the same storage the helper does (guards the other tests). */
    @GameTest(template = "flat_7x6x7")
    public static void spoutExposesNoFluidStorageBelow(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    BlockPos spout = new BlockPos(2, 2, 2);
                    helper.setBlock(spout, AllBlocks.SPOUT.getDefaultState());
                    helper.assertTrue(
                            TransferUtil.getFluidStorage(
                                            helper.getLevel(),
                                            helper.absolutePos(spout),
                                            Direction.DOWN)
                                    == null,
                            "spout must not expose its tank to the block below (it fills items"
                                    + " there)");
                    helper.assertTrue(
                            TransferUtil.getFluidStorage(
                                            helper.getLevel(),
                                            helper.absolutePos(spout),
                                            Direction.UP)
                                    != null,
                            "spout must accept fluid from above");
                    helper.succeed();
                });
    }
}
