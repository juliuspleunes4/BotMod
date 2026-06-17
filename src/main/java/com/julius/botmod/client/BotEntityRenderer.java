package com.julius.botmod.client;

import com.julius.botmod.entity.BotEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class BotEntityRenderer extends LivingEntityRenderer<BotEntity, PlayerModel<BotEntity>> {

    // ConcurrentHashMap because skin loads complete on a different thread
    private final ConcurrentHashMap<UUID, ResourceLocation> skinCache = new ConcurrentHashMap<>();

    public BotEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(BotEntity entity) {
        UUID uuid = entity.getUUID();

        // Return cached result if we already started loading
        if (skinCache.containsKey(uuid)) {
            return skinCache.get(uuid);
        }

        // Entity data not synced from server yet — show default, try again next frame
        if (!entity.hasSkinData()) {
            return DefaultPlayerSkin.get(uuid).texture();
        }

        // Cache default immediately so we don't re-trigger the load every frame
        ResourceLocation defaultTexture = DefaultPlayerSkin.get(uuid).texture();
        skinCache.put(uuid, defaultTexture);

        // Kick off async skin download — updates the cache when done
        GameProfile profile = entity.buildClientProfile();
        Minecraft.getInstance().getSkinManager()
                .getOrLoad(profile)
                .thenAccept(skin -> skinCache.put(uuid, skin.texture()));

        return defaultTexture;
    }
}
