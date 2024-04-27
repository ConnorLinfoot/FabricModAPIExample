package dev.linfoot.modapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.linfoot.modapi.payload.ClientboundHypixelPayload;
import dev.linfoot.modapi.payload.ServerboundHypixelPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundLocationPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HypixelFabricModExample implements ClientModInitializer {
    private final ServerboundPacketRegistry registry = new ServerboundPacketRegistry();
    private ClientboundPacketHandler handler;

    @Override
    public void onInitializeClient() {
        handler = new ClientboundPacketHandler() {
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
        };
        registerPayloads();
        registerCommand();
    }

    private void registerPayloads() {
        for (String identifier : HypixelModAPI.getInstance().getRegistry().getIdentifiers()) {
            CustomPayload.Id<ServerboundHypixelPayload> serverboundId = CustomPayload.id(identifier);
            PayloadTypeRegistry.playC2S().register(serverboundId, ServerboundHypixelPayload.buildCodec(serverboundId));

            CustomPayload.Id<ClientboundHypixelPayload> clientboundId = CustomPayload.id(identifier);
            PayloadTypeRegistry.playS2C().register(clientboundId, ClientboundHypixelPayload.buildCodec(clientboundId));

            ClientPlayNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
                if (payload.isSuccess()) {
                    payload.getPacket().handle(handler);
                } else {
                    System.err.println("Got error packet: " + payload.getErrorReason());
                }
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
        HypixelPacket packet = registry.createPacket(identifier);
        sendPacket(packet);
    }

    public void sendPacket(HypixelPacket packet) {
        ClientPlayNetworking.send(new ServerboundHypixelPayload(packet));
    }
}
