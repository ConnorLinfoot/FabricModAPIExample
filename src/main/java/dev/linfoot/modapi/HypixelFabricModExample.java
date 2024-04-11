package dev.linfoot.modapi;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.HypixelPacketType;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HypixelFabricModExample implements ClientModInitializer {
    private final ServerboundPacketRegistry packetRegistry = new ServerboundPacketRegistry();

    @Override
    public void onInitializeClient() {
        HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
            @Override
            public void handle(HypixelPacket packet) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Received " + packet.getType().getIdentifier() + " packet " + packet));
            }
        });
        registerNetworkHandlers();
        registerCommand();
    }

    private void registerNetworkHandlers() {
        for (HypixelPacketType packetType : HypixelPacketType.values()) {
            ClientPlayNetworking.registerGlobalReceiver(new Identifier(packetType.getIdentifier()), (client, handler, buf, responseSender) -> {
                client.execute(() -> {
                    HypixelModAPI.getInstance().handle(packetType.getIdentifier(), new PacketSerializer(buf));
                });
            });
        }
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("modapi")
                .then(argument("type", StringArgumentType.word())
                        .executes(context -> {
                            String type = context.getArgument("type", String.class);
                            HypixelPacketType packetType = HypixelPacketType.valueOf(type.toUpperCase());

                            sendPacket(packetType);
                            FabricClientCommandSource source = context.getSource();
                            source.sendFeedback(Text.literal("Sent packet: " + packetType.name()));
                            return 1;
                        }))));
    }

    public void sendPacket(HypixelPacketType packetType) {
        HypixelPacket packet = packetRegistry.createPacket(packetType);
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(new PacketSerializer(buf));
        ClientPlayNetworking.send(new Identifier(packetType.getIdentifier()), buf);
    }
}
