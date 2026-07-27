package com.vcwdfca.novaengfix.hypernet;

import com.vcwdfca.novaengfix.mixin.hypernet.AccessorComputationCenterCache;
import com.vcwdfca.novaengfix.mixin.hypernet.AccessorNetNodeCache;
import com.vcwdfca.novaengfix.mixin.hypernet.InvokerComputationCenterCheckNodeConnection;
import github.kasuminova.novaeng.common.hypernet.old.ComputationCenter;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeCache;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps HyperNet lifecycle work in one place so registry reloads cannot race node IO.
 */
public final class HyperNetFixLifecycle {
    private static final Set<NetNode> NODES = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<TileMultiblockMachineController> NODE_TICK_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<TileHyperNetTerminal> TERMINAL_TICK_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<TileEntity> DIRTY_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<NetNode> NODE_WRITE_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<ComputationCenter> CENTER_WRITE_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<ComputationCenter> CENTER_CHECK_PENDING = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private static final Set<World> UNLOADING_WORLDS = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );

    private static volatile boolean reloading;
    private static volatile boolean serverStopping;

    public HyperNetFixLifecycle() {
    }

    public static boolean isReloading() {
        return reloading;
    }

    public static void serverStarting() {
        serverStopping = false;
        synchronized (UNLOADING_WORLDS) {
            UNLOADING_WORLDS.clear();
        }
    }

    public static void track(NetNode node) {
        if (node == null) {
            return;
        }
        synchronized (NODES) {
            NODES.add(node);
        }
    }

    public static void untrack(NetNode node) {
        if (node == null) {
            return;
        }
        synchronized (NODES) {
            NODES.remove(node);
        }
    }

    public static boolean canCreateCache(TileMultiblockMachineController controller) {
        World world = controller == null ? null : controller.getWorld();
        return controller != null && !controller.isInvalid() && !reloading && !serverStopping && !isWorldUnloading(world);
    }

    public static boolean canTick(NetNode node) {
        if (node == null || reloading || serverStopping) {
            return false;
        }

        TileMultiblockMachineController owner = node.getOwner();
        World world = owner == null ? null : owner.getWorld();
        return owner != null && !world.isRemote && !owner.isInvalid() && !isWorldUnloading(world);
    }

    public static boolean isServerThread(TileEntity tile) {
        if (tile == null) {
            return false;
        }
        World world = tile.getWorld();
        if (world.isRemote) {
            return true;
        }
        MinecraftServer server = world.getMinecraftServer();
        return server == null || server.isCallingFromMinecraftThread();
    }

    public static boolean isWorldActive(World world) {
        return world != null && !serverStopping && !isWorldUnloading(world);
    }

    public static void scheduleNodeWrite(final NetNode node) {
        if (node == null || serverStopping || isServerThread(node.getOwner())) {
            return;
        }

        synchronized (NODE_WRITE_PENDING) {
            if (!NODE_WRITE_PENDING.add(node)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (NODE_WRITE_PENDING) {
                if (!NODE_WRITE_PENDING.remove(node)) {
                    return;
                }
            }

            if (reloading || serverStopping) {
                return;
            }
            TileMultiblockMachineController owner = node.getOwner();
            World world = owner == null ? null : owner.getWorld();
            if (owner == null || owner.isInvalid() || world.isRemote || isWorldUnloading(world)) {
                return;
            }
            node.writeNBT();
        });
    }

    public static void scheduleCenterWrite(final ComputationCenter center) {
        if (center == null || serverStopping || isServerThread(center.getOwner())) {
            return;
        }

        synchronized (CENTER_WRITE_PENDING) {
            if (!CENTER_WRITE_PENDING.add(center)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (CENTER_WRITE_PENDING) {
                if (!CENTER_WRITE_PENDING.remove(center)) {
                    return;
                }
            }

            if (reloading || serverStopping) {
                return;
            }
            TileMultiblockMachineController owner = center.getOwner();
            World world = owner == null ? null : owner.getWorld();
            if (owner == null || owner.isInvalid() || world.isRemote || isWorldUnloading(world)) {
                return;
            }
            center.writeNBT();
        });
    }

    public static void scheduleCenterNodeCheck(final ComputationCenter center) {
        if (center == null || reloading || serverStopping) {
            return;
        }

        TileMultiblockMachineController owner = center.getOwner();
        if (owner == null || isServerThread(owner)) {
            return;
        }

        synchronized (CENTER_CHECK_PENDING) {
            if (!CENTER_CHECK_PENDING.add(center)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (CENTER_CHECK_PENDING) {
                if (!CENTER_CHECK_PENDING.remove(center)) {
                    return;
                }
            }

            if (reloading || serverStopping) {
                return;
            }
            TileMultiblockMachineController currentOwner = center.getOwner();
            World world = currentOwner == null ? null : currentOwner.getWorld();
            if (currentOwner == null || currentOwner.isInvalid() || world.isRemote || isWorldUnloading(world)) {
                return;
            }
            ((InvokerComputationCenterCheckNodeConnection) center)
                .novaengfix$invokeCheckNodeConnection();
        });
    }

    public static synchronized void beginReload() {
        if (reloading || serverStopping) {
            return;
        }
        flushPendingWrites(null);
        reloading = true;
        clearTickMarks();

        List<NetNode> nodes = snapshotNodes();
        for (NetNode node : nodes) {
            disconnect(node);
        }
        for (NetNode node : nodes) {
            TileMultiblockMachineController owner = node.getOwner();
            if (!(owner instanceof TileHyperNetTerminal)) {
                untrack(node);
            }
        }
        clearNodeCaches(null);
        clearComputationCenters(null);
    }

    public static void finishReload() {
        reloading = false;
        clearTickMarks();
    }

    public static void disconnectController(TileMultiblockMachineController controller) {
        if (controller == null) {
            return;
        }

        List<NetNode> owned = new ArrayList<>();
        synchronized (NODES) {
            for (NetNode node : NODES) {
                if (node.getOwner() == controller) {
                    owned.add(node);
                }
            }
        }

        for (NetNode node : owned) {
            disconnect(node);
        }
        novaengfix$removeCachedNode(controller, owned);
        for (NetNode node : owned) {
            untrack(node);
        }
        synchronized (NODE_TICK_PENDING) {
            NODE_TICK_PENDING.remove(controller);
        }
        synchronized (TERMINAL_TICK_PENDING) {
            if (controller instanceof TileHyperNetTerminal) {
                TERMINAL_TICK_PENDING.remove((TileHyperNetTerminal) controller);
            }
        }
    }

    public static void clearAll() {
        serverStopping = true;
        flushPendingWrites(null);
        List<NetNode> nodes = snapshotNodes();
        for (NetNode node : nodes) {
            disconnect(node);
        }

        clearNodeCaches(null);

        synchronized (NODES) {
            NODES.clear();
        }
        clearTickMarks();
        clearComputationCenters(null);
        synchronized (UNLOADING_WORLDS) {
            UNLOADING_WORLDS.clear();
        }
        reloading = false;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }

        synchronized (UNLOADING_WORLDS) {
            UNLOADING_WORLDS.add(world);
        }

        Set<TileMultiblockMachineController> owners = Collections.newSetFromMap(
            new IdentityHashMap<>()
        );
        List<NetNode> nodes = snapshotNodes();
        for (NetNode node : nodes) {
            TileMultiblockMachineController owner = node.getOwner();
            if (owner != null && owner.getWorld() == world) {
                owners.add(owner);
            }
        }
        for (TileMultiblockMachineController owner : owners) {
            disconnectController(owner);
        }
        flushPendingWrites(world);
        clearPendingForWorld(world);
        clearNodeCaches(world);
        clearComputationCenters(world);
    }

    public static void scheduleNodeTick(final TileMultiblockMachineController controller) {
        if (controller == null || reloading || serverStopping) {
            return;
        }

        synchronized (NODE_TICK_PENDING) {
            if (!NODE_TICK_PENDING.add(controller)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (NODE_TICK_PENDING) {
                NODE_TICK_PENDING.remove(controller);
            }

            if (reloading || serverStopping || controller.isInvalid()) {
                return;
            }
            World world = controller.getWorld();
            if (world.isRemote || isWorldUnloading(world)) {
                return;
            }
            DynamicMachine machine = controller.getFoundMachine();
            if (machine == null) {
                return;
            }
            Class<? extends NetNode> type = RegistryHyperNet.getNodeType(machine);
            if (type == null) {
                return;
            }
            NetNode node = NetNodeCache.getCache(controller, type);
            if (canTick(node)) {
                node.onMachineTick();
            }
        });
    }

    public static void scheduleTerminalTick(final TileHyperNetTerminal terminal) {
        if (terminal == null || reloading || serverStopping) {
            return;
        }
        synchronized (TERMINAL_TICK_PENDING) {
            if (!TERMINAL_TICK_PENDING.add(terminal)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (TERMINAL_TICK_PENDING) {
                TERMINAL_TICK_PENDING.remove(terminal);
            }
            World world = terminal.getWorld();
            if (isWorldActive(world) && !world.isRemote && !terminal.isInvalid()) {
                track(terminal.getNodeProxy());
            }
            if (!reloading && !serverStopping && isWorldActive(world) && !world.isRemote && !terminal.isInvalid()
                && terminal instanceof ITerminalTickFix) {
                ((ITerminalTickFix) terminal).novaengfix$runTerminalTick();
            }
        });
    }

    public static void markDirty(final TileEntity tile) {
        if (tile == null || serverStopping) {
            return;
        }

        if (isServerThread(tile)) {
            if (!tile.isInvalid()) {
                tile.markDirty();
            }
            return;
        }

        synchronized (DIRTY_PENDING) {
            if (!DIRTY_PENDING.add(tile)) {
                return;
            }
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (DIRTY_PENDING) {
                DIRTY_PENDING.remove(tile);
            }
            World world = tile.getWorld();
            if (!tile.isInvalid() && !isWorldUnloading(world)) {
                tile.markDirty();
            }
        });
    }

    public static boolean isTerminalMachine(ResourceLocation name) {
        return name != null && "hypernet_terminal".equals(name.getPath());
    }

    private static List<NetNode> snapshotNodes() {
        synchronized (NODES) {
            return new ArrayList<>(NODES);
        }
    }

    private static boolean isWorldUnloading(World world) {
        synchronized (UNLOADING_WORLDS) {
            return UNLOADING_WORLDS.contains(world);
        }
    }

    private static void clearComputationCenters(World world) {
        Map<TileMultiblockMachineController, ComputationCenter> cache =
            AccessorComputationCenterCache.novaengfix$getCachedComputationCenters();
        if(cache == null) {
            return;
        }

        synchronized (cache) {
            if (world == null) {
                cache.clear();
                return;
            }

            Iterator<Map.Entry<TileMultiblockMachineController, ComputationCenter>> iterator =
                cache.entrySet().iterator();
            while (iterator.hasNext()) {
                TileMultiblockMachineController owner = iterator.next().getKey();
                if (owner != null && owner.getWorld() == world) {
                    iterator.remove();
                }
            }
        }
    }

    private static void clearNodeCaches(World world) {
        Map<TileMultiblockMachineController, NetNode> cache =
            AccessorNetNodeCache.novaengfix$getCachedNodes();
        List<TileMultiblockMachineController> owners = new ArrayList<>();
        List<NetNode> nodes = new ArrayList<>();
        if(cache == null) {
            return;
        }

        synchronized (cache) {
            for (Map.Entry<TileMultiblockMachineController, NetNode> entry : cache.entrySet()) {
                TileMultiblockMachineController owner = entry.getKey();
                World ownerWorld = owner == null ? null : owner.getWorld();
                if (world == null || ownerWorld == world) {
                    owners.add(owner);
                    nodes.add(entry.getValue());
                }
            }
        }

        for (int i = 0; i < owners.size(); i++) {
            TileMultiblockMachineController owner = owners.get(i);
            NetNode node = nodes.get(i);
            if (cache.remove(owner, node)) {
                disconnect(node);
                untrack(node);
            }
        }
    }

    private static void novaengfix$removeCachedNode(TileMultiblockMachineController controller,
                                                     List<NetNode> owned) {
        Map<TileMultiblockMachineController, NetNode> cache =
            AccessorNetNodeCache.novaengfix$getCachedNodes();
        if(cache == null) {
            return;
        }

        NetNode cached = cache.get(controller);
        if (cached == null || (!owned.isEmpty() && !owned.contains(cached))) {
            return;
        }
        if (cache.remove(controller, cached)) {
            disconnect(cached);
            untrack(cached);
        }
    }

    private static void clearPendingForWorld(World world) {
        synchronized (NODE_TICK_PENDING) {
            NODE_TICK_PENDING.removeIf(controller -> controller.getWorld() == world);
        }
        synchronized (TERMINAL_TICK_PENDING) {
            TERMINAL_TICK_PENDING.removeIf(terminal -> terminal.getWorld() == world);
        }
        synchronized (DIRTY_PENDING) {
            DIRTY_PENDING.removeIf(tile -> tile.getWorld() == world);
        }
        synchronized (NODE_WRITE_PENDING) {
            NODE_WRITE_PENDING.removeIf(node -> node.getOwner() != null && node.getOwner().getWorld() == world);
        }
        synchronized (CENTER_WRITE_PENDING) {
            CENTER_WRITE_PENDING.removeIf(center -> center.getOwner() != null && center.getOwner().getWorld() == world);
        }
        synchronized (CENTER_CHECK_PENDING) {
            CENTER_CHECK_PENDING.removeIf(center -> center.getOwner() != null && center.getOwner().getWorld() == world);
        }
    }

    private static void disconnect(NetNode node) {
        if (node == null) {
            return;
        }
        try {
            node.disconnect();
        } catch (RuntimeException ignored) {
            // A stale world can invalidate the owner while a node is being removed.
        }
    }

    private static void clearTickMarks() {
        synchronized (NODE_TICK_PENDING) {
            NODE_TICK_PENDING.clear();
        }
        synchronized (TERMINAL_TICK_PENDING) {
            TERMINAL_TICK_PENDING.clear();
        }
        synchronized (DIRTY_PENDING) {
            DIRTY_PENDING.clear();
        }
        synchronized (NODE_WRITE_PENDING) {
            NODE_WRITE_PENDING.clear();
        }
        synchronized (CENTER_WRITE_PENDING) {
            CENTER_WRITE_PENDING.clear();
        }
        synchronized (CENTER_CHECK_PENDING) {
            CENTER_CHECK_PENDING.clear();
        }
    }

    private static void flushPendingWrites(World world) {
        if (reloading && !serverStopping) {
            return;
        }

        List<NetNode> nodes = new ArrayList<>();
        synchronized (NODE_WRITE_PENDING) {
            Iterator<NetNode> iterator = NODE_WRITE_PENDING.iterator();
            while (iterator.hasNext()) {
                NetNode node = iterator.next();
                World nodeWorld = node.getOwner() == null ? null : node.getOwner().getWorld();
                if (world == null || nodeWorld == world) {
                    nodes.add(node);
                    iterator.remove();
                }
            }
        }
        for (NetNode node : nodes) {
            node.writeNBT();
        }

        List<ComputationCenter> centers = new ArrayList<>();
        synchronized (CENTER_WRITE_PENDING) {
            Iterator<ComputationCenter> iterator = CENTER_WRITE_PENDING.iterator();
            while (iterator.hasNext()) {
                ComputationCenter center = iterator.next();
                World centerWorld = center.getOwner() == null ? null : center.getOwner().getWorld();
                if (world == null || centerWorld == world) {
                    centers.add(center);
                    iterator.remove();
                }
            }
        }
        for (ComputationCenter center : centers) {
            center.writeNBT();
        }
    }
}
