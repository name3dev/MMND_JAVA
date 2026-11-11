package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class SpellbookItem extends Item {
    private static final int SHARED_COOLDOWN_TICKS = 40;
    private static final double BOLT_RANGE = 24.0;
    private static final Predicate<Entity> SPELL_TARGET =
            entity -> entity instanceof LivingEntity && entity.isPickable();

    public SpellbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ExampleModClient.openSpellbook();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void castSpell(Player player, int spellId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Spell spell = Spell.fromId(spellId);
        if (spell == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("spell.examplemod.invalid"), true);
            return;
        }

        if (!isHoldingSpellbook(player)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("spell.examplemod.no_book"), true);
            return;
        }

        if (!player.isAlive() || player.isSpectator()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("spell.examplemod.unavailable"), true);
            return;
        }

        if (player.getCooldowns().isOnCooldown(ExampleMod.ARCANE_SPELLBOOK.get())) {
            serverPlayer.displayClientMessage(
                    Component.translatable("spell.examplemod.cooldown"), true);
            return;
        }

        boolean cast = switch (spell) {
            case ARCANE_BOLT -> castArcaneBolt(serverPlayer);
            case FROST_NOVA -> castFrostNova(serverPlayer);
            case BLINK -> castBlink(serverPlayer);
            case HEALING_LIGHT -> castHealingLight(serverPlayer);
        };

        if (cast) {
            player.getCooldowns().addCooldown(ExampleMod.ARCANE_SPELLBOOK.get(), SHARED_COOLDOWN_TICKS);
            MutableComponent result = Component.translatable("spell.examplemod.cast");
            result.append(Component.translatable(spell.translationKey()));
            serverPlayer.displayClientMessage(result, true);
        } else {
            serverPlayer.displayClientMessage(
                    Component.translatable("spell.examplemod.failed." + spell.key),
                    true);
        }
    }

    private static boolean isHoldingSpellbook(Player player) {
        return player.getMainHandItem().is(ExampleMod.ARCANE_SPELLBOOK.get())
                || player.getOffhandItem().is(ExampleMod.ARCANE_SPELLBOOK.get());
    }

    private static boolean castArcaneBolt(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 rangeEnd = start.add(player.getLookAngle().scale(BOLT_RANGE));
        BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
                start,
                rangeEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();
        AABB searchBounds = player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBounds,
                SPELL_TARGET.and(entity -> entity != player),
                start.distanceToSqr(end)
        );

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return false;
        }

        if (!target.hurt(player.damageSources().magic(), 8.0F)) {
            return false;
        }

        player.serverLevel().sendParticles(ParticleTypes.END_ROD, hit.getLocation().x, hit.getLocation().y,
                hit.getLocation().z, 16, 0.15, 0.15, 0.15, 0.03);
        player.playNotifySound(SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }

    private static boolean castFrostNova(ServerPlayer player) {
        double radius = 5.5;
        int affected = 0;
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive()
        )) {
            if (!target.hurt(player.damageSources().magic(), 4.0F)) {
                continue;
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), player);
            Vec3 push = target.position().subtract(player.position()).normalize().scale(0.7);
            target.push(push.x, 0.25, push.z);
            target.hurtMarked = true;
            affected++;
        }

        if (affected == 0) {
            return false;
        }

        player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                70, radius / 2, 1.0, radius / 2, 0.08);
        player.playNotifySound(SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static boolean castBlink(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().normalize();
        for (double distance = 8.0; distance >= 1.0; distance -= 0.5) {
            Vec3 destination = player.position().add(direction.scale(distance));
            AABB destinationBox = player.getBoundingBox().move(destination.subtract(player.position()));
            if (!player.serverLevel().getWorldBorder().isWithinBounds(destinationBox)
                    || !player.serverLevel().noCollision(player, destinationBox)) {
                continue;
            }

            player.teleportTo(destination.x, destination.y, destination.z);
            player.fallDistance = 0.0F;
            player.serverLevel().sendParticles(ParticleTypes.PORTAL,
                    destination.x, destination.y + 0.8, destination.z,
                    36, 0.35, 0.6, 0.35, 0.15);
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.1F);
            return true;
        }
        return false;
    }

    private static boolean castHealingLight(ServerPlayer player) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }

        player.heal(8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1), player);
        player.serverLevel().sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.45, 0.6, 0.45, 0.0);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }

    public enum Spell {
        ARCANE_BOLT(0, "arcane_bolt"),
        FROST_NOVA(1, "frost_nova"),
        BLINK(2, "blink"),
        HEALING_LIGHT(3, "healing_light");

        private final int id;
        private final String key;

        Spell(int id, String key) {
            this.id = id;
            this.key = key;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return "spell." + ExampleMod.MODID + "." + key;
        }

        public String descriptionKey() {
            return translationKey() + ".description";
        }

        public static Spell fromId(int id) {
            for (Spell spell : values()) {
                if (spell.id == id) {
                    return spell;
                }
            }
            return null;
        }
    }
}
