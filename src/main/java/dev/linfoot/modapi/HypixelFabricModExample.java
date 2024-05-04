package dev.linfoot.modapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundLocationPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HypixelFabricModExample implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServerboundPacketRegistry registry = new ServerboundPacketRegistry();

    @Override
    public void onInitializeClient() {
        HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
            private void handle(ClientboundHypixelPacket packet) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Received packet " + packet));
            }

            @Override
            public void onPingPacket(ClientboundPingPacket packet) {
                handle(packet);
            }

            @Override
            public void onLocationPacket(ClientboundLocationPacket packet) {
                handle(packet);
            }

            @Override
            public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                handle(packet);
            }

            @Override
            public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
                handle(packet);
            }
        });
        registerCommand();
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("modapi")
                .then(argument("type", StringArgumentType.greedyString())
                        .executes(context -> {
                            String identifier = context.getArgument("type", String.class);
                            if (identifier.equals("direct")) {
                                sendPacket(new ServerboundLocationPacket());
                            } else {
                                if (sendPacket(identifier)) {
                                    FabricClientCommandSource source = context.getSource();
                                    source.sendFeedback(Text.literal("Sent packet: " + identifier).withColor(Formatting.GREEN.getColorValue()));
                                } else {
                                    FabricClientCommandSource source = context.getSource();
                                    source.sendError(Text.literal("Failed to send packet: " + identifier));
                                }
                            }
                            return 1;
                        }))));
    }

    public boolean sendPacket(String identifier) {
        HypixelPacket packet = registry.createPacket(identifier);
        if (packet == null) {
            LOGGER.warn("Packet {} is not registered", identifier);
            return false;
        }

        sendPacket(packet);
        return true;
    }

    public void sendPacket(HypixelPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(new PacketSerializer(buf));
        HypixelModAPI.getInstance().sendPacket(packet);
    }
}
