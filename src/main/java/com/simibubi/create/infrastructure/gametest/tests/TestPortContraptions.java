package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/** Contraption storage keeps item components (6.0.10 #9788: enchanted items disappearing). */
@GameTestGroup(path = "contraptions")
public class TestPortContraptions {

    static boolean containsExact(Storage<ItemVariant> storage, ItemStack expected) {
        for (StorageView<ItemVariant> v : storage.nonEmptyViews())
            if (v.getResource().matches(expected) && v.getAmount() >= expected.getCount())
                return true;
        return false;
    }

    /**
     * Same machine as TestContraptions.mountedItemExtract, but the barrel on the contraption holds
     * an enchanted sword, a renamed item and a potion. They must come out of the moving
     * contraption's storage with their components intact.
     */
    @GameTest(template = "mounted_item_extract", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void mountedStorageKeepsEnchantedAndNamedItems(CreateGameTestHelper helper) {
        BlockPos barrel = new BlockPos(1, 3, 2);
        BlockPos lever = new BlockPos(1, 5, 1);
        BlockPos outputPos = new BlockPos(4, 2, 1);

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS),
                2);
        ItemStack named = new ItemStack(Items.DIAMOND, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("loot"));
        ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        List<ItemStack> special = List.of(sword, named, potion);

        Container c = (Container) helper.getBlockEntity(barrel);
        c.clearContent();
        for (int i = 0; i < special.size(); i++) c.setItem(i, special.get(i).copy());
        c.setChanged();

        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    Storage<ItemVariant> out = helper.itemStorageAt(outputPos);
                    for (ItemStack s : special)
                        helper.assertTrue(
                                containsExact(out, s),
                                "output is missing " + s + " " + s.getComponentsPatch());
                    helper.powerLever(lever);
                    helper.assertContainerEmpty(barrel);
                    Create.LOGGER.info(
                            "[qa] contraption storage kept enchanted/named/potion items");
                });
    }
}
