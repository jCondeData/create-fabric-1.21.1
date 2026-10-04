package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.advancement.AllAdvancements;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

public class DivingHelmetItem extends BaseArmorItem {
    public static final EquipmentSlot SLOT = EquipmentSlot.HEAD;
    public static final ArmorItem.Type TYPE = ArmorItem.Type.HELMET;

    public DivingHelmetItem(
            Holder<ArmorMaterial> material, Properties properties, ResourceLocation textureLoc) {
        super(material, TYPE, properties, textureLoc);
    }

    // fabric: NeoForge grants innate Aqua Affinity via getAllEnchantments/getEnchantmentLevel,
    // which have no
    // Fabric equivalent. Aqua Affinity is only an attribute modifier in 1.21, so grant that
    // directly instead.
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return super.getDefaultAttributeModifiers()
                .withModifierAdded(
                        Attributes.SUBMERGED_MINING_SPEED,
                        new AttributeModifier(
                                Create.asResource("diving_helmet_aqua_affinity"),
                                4,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        EquipmentSlotGroup.HEAD);
    }

    public static boolean isWornBy(Entity entity) {
        return !getWornItem(entity).isEmpty();
    }

    public static ItemStack getWornItem(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = livingEntity.getItemBySlot(SLOT);
        if (!(stack.getItem() instanceof DivingHelmetItem)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public static void breatheUnderwater(LivingEntity entity) {
        Level level = entity.level();
        boolean second = level.getGameTime() % 20 == 0;
        boolean drowning = entity.getAirSupply() == 0;

        if (level.isClientSide) entity.getCustomData().remove("VisualBacktankAir");

        ItemStack helmet = getWornItem(entity);
        if (helmet.isEmpty()) return;

        boolean lavaDiving = entity.isInLava();
        if (!helmet.has(DataComponents.FIRE_RESISTANT) && lavaDiving) return;
        if (!entity.isEyeInFluid(AllFluidTags.DIVING_FLUIDS.tag) && !lavaDiving) return;
        if (entity instanceof Player player && (player.isSpectator() || player.isCreative()))
            return;

        List<ItemStack> backtanks = BacktankUtil.getAllWithAir(entity);
        if (backtanks.isEmpty()) return;

        if (lavaDiving) {
            if (entity instanceof ServerPlayer sp) AllAdvancements.DIVING_SUIT_LAVA.awardTo(sp);
            if (backtanks.stream()
                    .noneMatch(backtank -> backtank.has(DataComponents.FIRE_RESISTANT))) return;
        }

        float visualBacktankAir = 0f;
        for (ItemStack stack : backtanks) visualBacktankAir += BacktankUtil.getAir(stack);

        if (drowning) entity.setAirSupply(10);

        if (level.isClientSide)
            entity.getCustomData().putInt("VisualBacktankAir", Math.round(visualBacktankAir));

        if (!second) return;

        BacktankUtil.consumeAir(entity, backtanks.get(0), 1);

        if (lavaDiving) return;

        if (entity instanceof ServerPlayer sp) AllAdvancements.DIVING_SUIT.awardTo(sp);

        // fabric: there is no LivingBreatheEvent, so keep refilling air and granting water
        // breathing from the entity tick instead of setCanBreathe/setRefillAirAmount
        entity.setAirSupply(Math.min(entity.getMaxAirSupply(), entity.getAirSupply() + 10));
        entity.addEffect(
                new MobEffectInstance(MobEffects.WATER_BREATHING, 30, 0, true, false, true));
    }
}
