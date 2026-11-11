
package com.example.examplemod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ElectricSpherePayload(
        double x,
        double y,
        double z
) implements CustomPacketPayload {

    public static final Type<ElectricSpherePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "electric_sphere"
                    )
            );

    public static final StreamCodec<ByteBuf, ElectricSpherePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    ElectricSpherePayload::x,
                    ByteBufCodecs.DOUBLE,
                    ElectricSpherePayload::y,
                    ByteBufCodecs.DOUBLE,
                    ElectricSpherePayload::z,
                    ElectricSpherePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
