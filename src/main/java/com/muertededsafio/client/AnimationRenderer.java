package com.muertededsafio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.muertededsafio.AnimPayload;
import com.muertededsafio.ModSounds;
import com.muertededsafio.MuerteDedsafio;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Dibuja los 263 cuadros de la animación del texture pack, el texto rojo, el filtro rojo y los rayos. */
public final class AnimationRenderer {
    private static final int FRAMES = 263;
    private static final long DEATH_MS = 6575L; // 40 cuadros por segundo (igual que el pack original)
    private static final long RED_MS = 9900L;   // dura lo mismo que la música de la ruleta
    // 263 cuadros agrupados en 5 hojas de 2048x2048 (8x8 cuadros de 256x256 cada una)
    private static final Identifier[] SHEETS = new Identifier[5];

    static {
        for (int i = 0; i < SHEETS.length; i++) {
            SHEETS[i] = Identifier.of(MuerteDedsafio.MOD_ID, "textures/muerte/sheet_" + i + ".png");
        }
    }

    private static final Random RNG = new Random();
    private static final List<float[]> SEGS = new ArrayList<>(); // x1, y1, x2, y2, grosor

    private static boolean active;
    private static int type;
    private static long startMs;
    private static long totalMs;
    private static long nextBoltMs;
    private static long flashMs;
    private static String playerName = "";
    private static String cause = "";

    private AnimationRenderer() {}

    public static void start(AnimPayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        type = p.type();
        playerName = p.name();
        cause = p.cause().getString();
        startMs = Util.getMeasuringTimeMs();
        totalMs = type == AnimPayload.RED ? RED_MS : DEATH_MS;
        nextBoltMs = 0L;
        flashMs = 0L;
        SEGS.clear();
        mc.getTextureManager().getTexture(SHEETS[0]);

        SoundEvent sound = type == AnimPayload.RED ? ModSounds.RULETA : ModSounds.DEDSAFIO;
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0F, 1.0F));
        active = true;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!active) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = Util.getMeasuringTimeMs();
        long elapsed = now - startMs;
        if (elapsed >= totalMs) {
            active = false;
            return;
        }

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        boolean red = type == AnimPayload.RED;

        float fadeIn = Math.min(1.0F, elapsed / 500.0F);
        float fadeOut = Math.min(1.0F, (totalMs - elapsed) / 800.0F);
        float fade = Math.max(0.0F, Math.min(fadeIn, fadeOut));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---------- Filtro rojo + rayos rojos ----------
        if (red) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(elapsed / 180.0);
            int tint = (int) ((70 + 40 * pulse) * fade);
            ctx.fill(0, 0, w, h, (tint << 24) | 0xB00000);
            int vig = (int) (150 * fade);
            ctx.fillGradient(0, 0, w, (int) (h * 0.35F), (vig << 24) | 0x500000, 0x00500000);
            ctx.fillGradient(0, (int) (h * 0.65F), w, h, 0x00500000, (vig << 24) | 0x500000);
            ctx.draw();

            if (now >= nextBoltMs) {
                flashMs = now;
                nextBoltMs = now + 110 + RNG.nextInt(260);
                if (RNG.nextInt(4) == 0) SEGS.clear(); else genBolts(w, h);
            }
            long sinceFlash = now - flashMs;
            if (sinceFlash < 140) {
                int fa = (int) (110 * (1.0F - sinceFlash / 140.0F) * fade);
                ctx.fill(0, 0, w, h, (fa << 24) | 0xFF2020);
                ctx.draw();
            }
            if (!SEGS.isEmpty()) {
                float span = Math.max(1L, nextBoltMs - flashMs);
                float alphaMul = Math.max(0.25F, 1.0F - sinceFlash / span) * fade;
                drawBolts(ctx.getMatrices().peek().getPositionMatrix(), alphaMul);
            }
        }

        // ---------- Cuadro de la animación ----------
        int frame = (int) Math.min(FRAMES - 1, elapsed * FRAMES / totalMs);
        int sheet = frame / 64;
        int idx = frame % 64;
        mc.getTextureManager().getTexture(SHEETS[Math.min(SHEETS.length - 1, sheet + 1)]);
        int size = Math.min(256, (int) (h * 0.6F));
        int top = h / 2 - size / 2 - 30;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ctx.drawTexture(SHEETS[sheet], (w - size) / 2, top, size, size,
                (float) ((idx % 8) * 256), (float) ((idx / 8) * 256), 256, 256, 2048, 2048);

        // ---------- Nombre y causa de muerte en rojo ----------
        if (!red) {
            float tIn = Math.min(1.0F, elapsed / 400.0F);
            float tOut = Math.min(1.0F, (totalMs - elapsed) / 600.0F);
            int alpha = Math.max(8, (int) (Math.max(0.0F, Math.min(tIn, tOut)) * 255));
            int color = (alpha << 24) | 0xFF1A1A;

            TextRenderer tr = mc.textRenderer;
            int y = top + size + 2;
            Text nameText = Text.literal(playerName).formatted(Formatting.BOLD);
            y += drawFitted(ctx, tr, nameText, w, y, 3.0F, color) + 4;
            drawFitted(ctx, tr, Text.literal(cause), w, y, 1.7F, color);
        }
        ctx.draw();
    }

    /** Dibuja texto centrado y lo achica si no cabe en pantalla. Devuelve el alto usado. */
    private static int drawFitted(DrawContext ctx, TextRenderer tr, Text text, int screenW, int y, float baseScale, int color) {
        int tw = Math.max(1, tr.getWidth(text));
        float scale = Math.min(baseScale, (screenW * 0.92F) / tw);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(screenW / 2.0F, y, 0.0F);
        ctx.getMatrices().scale(scale, scale, 1.0F);
        ctx.drawCenteredTextWithShadow(tr, text, 0, 0, color);
        ctx.getMatrices().pop();
        return (int) (tr.fontHeight * scale);
    }

    // ---------------- Rayos ----------------

    private static void genBolts(int w, int h) {
        SEGS.clear();
        int count = 1 + RNG.nextInt(3);
        for (int i = 0; i < count; i++) {
            float x = RNG.nextFloat() * w;
            float y = 0.0F;
            int steps = 10 + RNG.nextInt(6);
            float stepY = h * (0.75F + RNG.nextFloat() * 0.35F) / steps;
            for (int s = 0; s < steps; s++) {
                float nx = x + (RNG.nextFloat() - 0.5F) * w * 0.10F;
                float ny = y + stepY * (0.6F + RNG.nextFloat() * 0.8F);
                SEGS.add(new float[]{x, y, nx, ny, 1.0F});
                if (RNG.nextInt(5) == 0) {
                    float bx = nx;
                    float by = ny;
                    float dir = RNG.nextBoolean() ? 1.0F : -1.0F;
                    for (int b = 0; b < 3; b++) {
                        float bnx = bx + dir * (RNG.nextFloat() * w * 0.05F);
                        float bny = by + stepY * (0.4F + RNG.nextFloat() * 0.5F);
                        SEGS.add(new float[]{bx, by, bnx, bny, 0.5F});
                        bx = bnx;
                        by = bny;
                    }
                }
                x = nx;
                y = ny;
            }
        }
    }

    private static void drawBolts(Matrix4f m, float alphaMul) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        drawPass(m, 7.0F, 255, 0, 0, (int) (60 * alphaMul));
        drawPass(m, 3.5F, 255, 40, 40, (int) (130 * alphaMul));
        drawPass(m, 1.4F, 255, 170, 170, (int) (255 * alphaMul));
        RenderSystem.enableCull();
    }

    private static void drawPass(Matrix4f m, float halfWidth, int r, int g, int b, int a) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        boolean any = false;
        for (float[] s : SEGS) {
            float dx = s[2] - s[0];
            float dy = s[3] - s[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001F) continue;
            float px = -dy / len * halfWidth * s[4];
            float py = dx / len * halfWidth * s[4];
            buf.vertex(m, s[0] - px, s[1] - py, 0.0F).color(r, g, b, a);
            buf.vertex(m, s[0] + px, s[1] + py, 0.0F).color(r, g, b, a);
            buf.vertex(m, s[2] + px, s[3] + py, 0.0F).color(r, g, b, a);
            buf.vertex(m, s[2] - px, s[3] - py, 0.0F).color(r, g, b, a);
            any = true;
        }
        if (any) BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
