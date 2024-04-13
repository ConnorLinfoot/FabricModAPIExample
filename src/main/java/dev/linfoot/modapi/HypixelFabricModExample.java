package dev.linfoot.modapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HypixelFabricModExample implements ClientModInitializer {
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
        registerNetworkHandlers();
        registerCommand();
    }

    private void registerNetworkHandlers() {
        for (String identifier : HypixelModAPI.getInstance().getRegistry().getIdentifiers()) {
            ClientPlayNetworking.registerGlobalReceiver(new Identifier(identifier), (client, handler, buf, responseSender) -> {
                client.execute(() -> {
                    HypixelModAPI.getInstance().handle(identifier, new PacketSerializer(buf));
                });
            });
        }
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("modapi")
                .then(argument("type", StringArgumentType.greedyString())
                        .executes(context -> {
                            String identifier = context.getArgument("type", String.class);
                            if (identifier.equals("direct")) {
                                sendPacket(new ServerboundLocationPacket());
                            } else {
                                sendPacket(identifier);
                                FabricClientCommandSource source = context.getSource();
                                source.sendFeedback(Text.literal("Sent packet: " + identifier));
                            }
                            return 1;
                        }))));
    }

    public void sendPacket(String identifier) {
        PacketByteBuf buf = PacketByteBufs.create();
        HypixelPacket packet = registry.createPacket(identifier);
        packet.write(new PacketSerializer(buf));
        sendPacket(identifier, buf);
    }

    public void sendPacket(HypixelPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(new PacketSerializer(buf));
        ClientPlayNetworking.send(new Identifier(packet.getIdentifier()), buf);
    }

    private void sendPacket(String identifier, PacketByteBuf buf) {
        ClientPlayNetworking.send(new Identifier(identifier), buf);
    }
}
