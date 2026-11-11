
package com.example.examplemod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class NetheriteSwordItem extends SwordItem {

    private static final double RADIUS = 6.0;
    private static final float DAMAGE = 6.0F;
    private static final double KNOCKBACK = 1.8;

    private static final int CHARGE_TIME = 40;
    private static final int ACTIVE_TIME = 80;
    private static final int COOLDOWN = 60;

    private static final Map<UUID, AbilityState> ACTIVE_PLAYERS =
            new HashMap<>();

    private static final Set<UUID> CHARGING_PLAYERS =
            new HashSet<>();

    public NetheriteSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TIME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            UUID id = player.getUUID();

            if (ACTIVE_PLAYERS.containsKey(id)
                    || CHARGING_PLAYERS.contains(id)) {
                return InteractionResultHolder.fail(stack);
            }

            CHARGING_PLAYERS.add(id);
        }

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity livingEntity,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }

        if (remainingUseDuration <= 1) {
            UUID id = player.getUUID();

            if (CHARGING_PLAYERS.remove(id)) {
                player.stopUsingItem();

                ACTIVE_PLAYERS.put(id, new AbilityState(this));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        AbilityState state = ACTIVE_PLAYERS.get(player.getUUID());

        if (state == null) return;

        if (!player.isAlive()) {
            ACTIVE_PLAYERS.remove(player.getUUID());
            return;
        }

        state.age++;

        if (state.age <= ACTIVE_TIME) {
            applyPulse(player, state);
            return;
        }

        ACTIVE_PLAYERS.remove(player.getUUID());

        player.getCooldowns().addCooldown(state.item, COOLDOWN);
    }

    private static void applyPulse(
            Player player,
            AbilityState state
    ) {
        Vec3 center = player.position().add(0, 1, 0);

        AABB area = new AABB(
                center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
                center.x + RADIUS, center.y + RADIUS, center.z + RADIUS
        );

        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class, area
        )) {
            if (target == player || !target.isAlive()) continue;
            if (state.hitTargets.contains(target.getUUID())) continue;

            Vec3 targetCenter = target.position().add(
                    0, target.getBbHeight() / 2, 0
            );

            if (center.distanceTo(targetCenter) > RADIUS) continue;

            boolean damaged = target.hurt(
                    player.level().damageSources().playerAttack(player),
                    DAMAGE
            );

            if (!damaged) continue;

            state.hitTargets.add(target.getUUID());

            Vec3 direction = target.position().subtract(player.position());
            direction = new Vec3(direction.x, 0, direction.z);

            if (direction.lengthSqr() > 0.0001) {
                direction = direction.normalize();
            } else {
                direction = new Vec3(0, 0, 1);
            }

            target.setDeltaMovement(
                    target.getDeltaMovement().add(
                            direction.x * KNOCKBACK,
                            0.65,
                            direction.z * KNOCKBACK
                    )
            );

            target.hurtMarked = true;
        }
    }

    private static class AbilityState {
        private final NetheriteSwordItem item;
        private final Set<UUID> hitTargets = new HashSet<>();
        private int age = 0;

        private AbilityState(NetheriteSwordItem item) {
            this.item = item;
        }
    }
}
