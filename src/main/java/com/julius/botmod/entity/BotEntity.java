package com.julius.botmod.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BotEntity extends Mob {

    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE =
            SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_SIGNATURE =
            SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);

    public BotEntity(EntityType<? extends BotEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_NAME, "");
        builder.define(SKIN_VALUE, "");
        builder.define(SKIN_SIGNATURE, "");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** Called server-side after spawning to attach the owner's skin data. */
    public void setOwnerProfile(GameProfile profile) {
        entityData.set(OWNER_NAME, profile.getName() != null ? profile.getName() : "");
        var textures = profile.getProperties().get("textures");
        if (textures != null && !textures.isEmpty()) {
            Property prop = textures.iterator().next();
            entityData.set(SKIN_VALUE, prop.value());
            entityData.set(SKIN_SIGNATURE, prop.signature() != null ? prop.signature() : "");
        }
    }

    /** Called client-side by the renderer to reconstruct a GameProfile for skin loading. */
    public GameProfile buildClientProfile() {
        String name = entityData.get(OWNER_NAME);
        String value = entityData.get(SKIN_VALUE);
        String sig = entityData.get(SKIN_SIGNATURE);
        GameProfile profile = new GameProfile(this.getUUID(), name.isEmpty() ? "Bot" : name);
        if (!value.isEmpty()) {
            profile.getProperties().put("textures",
                    new Property("textures", value, sig.isEmpty() ? null : sig));
        }
        return profile;
    }

    public boolean hasSkinData() {
        return !entityData.get(SKIN_VALUE).isEmpty();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("OwnerName", entityData.get(OWNER_NAME));
        tag.putString("SkinValue", entityData.get(SKIN_VALUE));
        tag.putString("SkinSignature", entityData.get(SKIN_SIGNATURE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(SKIN_VALUE, tag.getString("SkinValue"));
        entityData.set(SKIN_SIGNATURE, tag.getString("SkinSignature"));
    }
}
