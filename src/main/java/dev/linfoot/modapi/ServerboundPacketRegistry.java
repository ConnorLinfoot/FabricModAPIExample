package dev.linfoot.modapi;

import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.HypixelPacketType;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class ServerboundPacketRegistry {
    private final Map<HypixelPacketType, Supplier<HypixelPacket>> packetFactory = new HashMap<>();

    ServerboundPacketRegistry() {
        register(HypixelPacketType.PING, ServerboundPingPacket::new);
        register(HypixelPacketType.LOCATION, ServerboundLocationPacket::new);
        register(HypixelPacketType.PARTY_INFO, ServerboundPartyInfoPacket::new);
        register(HypixelPacketType.PLAYER_INFO, ServerboundPlayerInfoPacket::new);
    }

    private void register(HypixelPacketType type, Supplier<HypixelPacket> packetClass) {
        packetFactory.put(type, packetClass);
    }

    @Nullable
    public HypixelPacket createPacket(HypixelPacketType type) {
        Supplier<HypixelPacket> packet = packetFactory.get(type);
        if (packet == null) {
            return null;
        }
        return packet.get();
    }
}
