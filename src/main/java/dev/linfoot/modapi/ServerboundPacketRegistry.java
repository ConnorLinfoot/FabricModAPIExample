package dev.linfoot.modapi;

import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

class ServerboundPacketRegistry {
    private final Map<String, Supplier<HypixelPacket>> packetFactory = new HashMap<>();

    ServerboundPacketRegistry() {
        register("hypixel:ping", ServerboundPingPacket::new);
        register("hypixel:location", ServerboundLocationPacket::new);
        register("hypixel:party_info", ServerboundPartyInfoPacket::new);
        register("hypixel:player_info", ServerboundPlayerInfoPacket::new);
    }

    private void register(String type, Supplier<HypixelPacket> packetClass) {
        packetFactory.put(type, packetClass);
    }

    @Nullable
    public HypixelPacket createPacket(String identifier) {
        Supplier<HypixelPacket> packet = packetFactory.get(identifier);
        if (packet == null) {
            return null;
        }
        return packet.get();
    }

    public Set<String> getIdentifiers() {
        return Collections.unmodifiableSet(packetFactory.keySet());
    }
}
