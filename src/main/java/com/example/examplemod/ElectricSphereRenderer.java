
package com.example.examplemod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElectricSphereRenderer {

    private static final List<Sphere> SPHERES = new ArrayList<>();

    private static final int DURATION = 8;
    private static final float MAX_RADIUS = 14.0F;

    private static final int SEGMENTS = 64;
    private static final int RINGS = 32;

    public static void spawn(double x, double y, double z) {
        SPHERES.add(new Sphere(new Vec3(x, y, z)));
    }

    public static void tick() {
        Iterator<Sphere> iterator = SPHERES.iterator();

        while (iterator.hasNext()) {
            Sphere sphere = iterator.next();
            sphere.age++;

            if (sphere.age >= DURATION) {
                iterator.remove();
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (SPHERES.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) return;

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();

        for (Sphere sphere : SPHERES) {
            float progress = sphere.getProgress();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float animationTime = sphere.age + partial;
            float radius = sphere.getRadius();

            poseStack.pushPose();

            poseStack.translate(
                    sphere.position.x - camera.x,
                    sphere.position.y - camera.y,
                    sphere.position.z - camera.z
            );

            PoseStack.Pose pose = poseStack.last();

            // Внешний слой — насыщенный синий


            VertexConsumer outer = buffers.getBuffer(RenderType.lightning());
            drawShell(outer, pose, radius, 1.00F,
                    5, 15, 130, 65, progress, animationTime);

            VertexConsumer deepBlue = buffers.getBuffer(RenderType.lightning());
            drawShell(deepBlue, pose, radius, 0.92F,
                    10, 35, 220, 75, progress, animationTime);

            VertexConsumer blue = buffers.getBuffer(RenderType.lightning());
            drawShell(blue, pose, radius, 0.84F,
                    15, 65, 255, 85, progress, animationTime);

            VertexConsumer royalBlue = buffers.getBuffer(RenderType.lightning());
            drawShell(royalBlue, pose, radius, 0.76F,
                    25, 100, 255, 90, progress, animationTime);

            VertexConsumer cyan = buffers.getBuffer(RenderType.lightning());
            drawShell(cyan, pose, radius, 0.68F,
                    35, 155, 255, 95, progress, animationTime);

            VertexConsumer lightBlue = buffers.getBuffer(RenderType.lightning());
            drawShell(lightBlue, pose, radius, 0.60F,
                    90, 205, 255, 105, progress, animationTime);

            VertexConsumer whiteBlue = buffers.getBuffer(RenderType.lightning());
            drawShell(whiteBlue, pose, radius, 0.52F,
                    190, 240, 255, 115, progress, animationTime);
            buffers.endBatch(RenderType.lightning());


            buffers.endBatch(RenderType.lightning());

            // Электрические разряды по поверхности
            VertexConsumer arcs = buffers.getBuffer(RenderType.lines());
            drawElectricArcs(arcs, pose, radius, sphere.age);
            buffers.endBatch(RenderType.lines());

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawShell(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float radius,
            float scale,
            int red,
            int green,
            int blue,
            int baseAlpha,
            float progress,
            float time
    ) {
        float r = radius * scale;
        int alpha = (int) (baseAlpha * (1.0F - progress * 0.25F));

        for (int ring = 0; ring < RINGS; ring++) {
            double lat1 = -Math.PI / 2 + Math.PI * ring / RINGS;
            double lat2 = -Math.PI / 2 + Math.PI * (ring + 1) / RINGS;

            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a1 = Math.PI * 2 * segment / SEGMENTS;
                double a2 = Math.PI * 2 * (segment + 1) / SEGMENTS;
                double wave1 = Math.sin(a1 * 5 + time * 0.35) * 0.025;
                double wave2 = Math.cos(lat1 * 7 - time * 0.45) * 0.018;

                float animatedRadius = r * (1.0F + (float) (wave1 + wave2));
                float x1 = (float) (Math.cos(lat1) * Math.cos(a1) * r);
                float y1 = (float) (Math.sin(lat1) * r);
                float z1 = (float) (Math.cos(lat1) * Math.sin(a1) * r);

                float x2 = (float) (Math.cos(lat1) * Math.cos(a2) * r);
                float y2 = y1;
                float z2 = (float) (Math.cos(lat1) * Math.sin(a2) * r);

                float x3 = (float) (Math.cos(lat2) * Math.cos(a2) * r);
                float y3 = (float) (Math.sin(lat2) * r);
                float z3 = (float) (Math.cos(lat2) * Math.sin(a2) * r);

                float x4 = (float) (Math.cos(lat2) * Math.cos(a1) * r);
                float y4 = y3;
                float z4 = (float) (Math.cos(lat2) * Math.sin(a1) * r);

                consumer.addVertex(pose, x1, y1, z1)
                        .setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z2)
                        .setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x3, y3, z3)
                        .setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x4, y4, z4)
                        .setColor(red, green, blue, alpha);
            }
        }
    }

    private static void drawElectricArcs(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float radius,
            int age
    ) {
        if (radius < 1.0F) return;

        int arcCount = 8;
        int segments = 10;

        for (int arc = 0; arc < arcCount; arc++) {
            double longitude = Math.PI * 2 * arc / arcCount;

            for (int i = 0; i < segments; i++) {
                double t1 = -1.25 + 2.5 * i / segments;
                double t2 = -1.25 + 2.5 * (i + 1) / segments;

                double offset1 = Math.sin(i * 2.7 + age * 0.9 + arc) * 0.12;
                double offset2 = Math.sin((i + 1) * 2.7 + age * 0.9 + arc) * 0.12;

                double a1 = longitude + offset1;
                double a2 = longitude + offset2;

                float x1 = (float) (Math.cos(t1) * Math.cos(a1) * radius * 1.005);
                float y1 = (float) (Math.sin(t1) * radius * 1.005);
                float z1 = (float) (Math.cos(t1) * Math.sin(a1) * radius * 1.005);

                float x2 = (float) (Math.cos(t2) * Math.cos(a2) * radius * 1.005);
                float y2 = (float) (Math.sin(t2) * radius * 1.005);
                float z2 = (float) (Math.cos(t2) * Math.sin(a2) * radius * 1.005);

                consumer.addVertex(pose, x1, y1, z1)
                        .setColor(190, 235, 255, 255)
                        .setNormal(pose, 0.0F, 1.0F, 0.0F);

                consumer.addVertex(pose, x2, y2, z2)
                        .setColor(20, 65, 255, 240)
                        .setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    public static class Sphere {
        public final Vec3 position;
        public int age = 0;

        public Sphere(Vec3 position) {
            this.position = position;
        }

        public float getProgress() {
            return Math.min(1.0F, age / (float) (DURATION - 1));
        }

        public float getRadius() {
            return MAX_RADIUS * getProgress();
        }
    }
}
