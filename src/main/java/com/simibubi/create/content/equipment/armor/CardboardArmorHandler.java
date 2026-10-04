package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.advancement.AllAdvancements;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CardboardArmorHandler {

    public static void playerHitboxChangesWhenHidingAsBox(EntityEvents.Size event) {
        Entity entity = event.getEntity();
        if (!entity.isAlive()) return;
        if (!testForStealth(entity)) return;

        float scale;
        if (entity instanceof LivingEntity le) {
            scale = le.getScale();
        } else {
            scale = 1.0F;
        }

        event.setNewSize(
                EntityDimensions.fixed(0.6F * scale, 0.8F * scale).withEyeHeight(0.6F * scale));
        if (!entity.level().isClientSide() && entity instanceof Player p)
            AllAdvancements.CARDBOARD_ARMOR.awardTo(p);
    }

    // fabric: registered to ServerEntityEvents.EQUIPMENT_CHANGE (server side only)
    public static void playerChangesEquipment(
            LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to) {
        if (entity instanceof Player player
                && player.getPose() == Pose.CROUCHING
                && (isCardboardArmor(player.getItemBySlot(EquipmentSlot.HEAD))
                        || isCardboardArmor(player.getItemBySlot(EquipmentSlot.CHEST))
                        || isCardboardArmor(player.getItemBySlot(EquipmentSlot.LEGS))
                        || isCardboardArmor(player.getItemBySlot(EquipmentSlot.FEET)))) {
            // assuming player is putting on last piece or took off first piece of cardboard armor
            if (!player.level().isClientSide()) {
                Pose pose = player.getPose();
                player.setPose(pose == Pose.CROUCHING ? Pose.STANDING : Pose.CROUCHING);
                player.setPose(pose);
            }
        }
    }

    public static void playersStealthWhenWearingCardboard(
            LivingEvents.LivingVisibilityEvent event) {
        LivingEntity entity = event.getEntity();
        if (!testForStealth(entity)) return;
        event.modifyVisibility(0);
    }

    public static void mobsMayLoseTargetWhenItIsWearingCardboard(LivingEntity entity) {
        if (entity.tickCount % 16 != 0) return;
        if (!(entity instanceof Mob mob)) return;

        if (testForStealth(mob.getTarget())) {
            mob.setTarget(null);
            if (mob.targetSelector != null)
                for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
                    if (goal.isRunning() && goal.getGoal() instanceof TargetGoal tg) tg.stop();
                }
        }

        if (entity instanceof NeutralMob nMob && entity.level() instanceof ServerLevel sl) {
            UUID uuid = nMob.getPersistentAngerTarget();
            if (uuid != null && testForStealth(sl.getEntity(uuid))) nMob.stopBeingAngry();
        }

        if (testForStealth(mob.getLastHurtByMob())) {
            mob.setLastHurtByMob(null);
            mob.setLastHurtByPlayer(null);
        }
    }

    public static boolean testForStealth(Entity entityIn) {
        if (!(entityIn instanceof LivingEntity entity)) return false;
        if (entity.getPose() != Pose.CROUCHING) return false;
        if (entity instanceof Player player && player.getAbilities().flying) return false;
        if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.HEAD))) return false;
        if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.CHEST))) return false;
        if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.LEGS))) return false;
        if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.FEET))) return false;
        return true;
    }

    public static boolean isCardboardArmor(ItemStack stack) {
        return stack.getItem() instanceof CardboardArmorItem;
    }
}
