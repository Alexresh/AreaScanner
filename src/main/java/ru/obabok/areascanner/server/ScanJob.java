package ru.obabok.areascanner.server;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.storage.NbtScannable;
import net.minecraft.world.storage.StorageIoWorker;
import ru.obabok.areascanner.common.BlockMatcher;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.common.model.JobInfo;
import ru.obabok.areascanner.common.model.Whitelist;
import ru.obabok.areascanner.common.network.payloads.s2c.*;
import ru.obabok.areascanner.server.network.SendQueue;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ScanJob{
    private final long jobId;
    private final ServerPlayerEntity owner;
    private final ArrayList<ServerPlayerEntity> subscribers;
    private final BlockBox range;
    private final Whitelist whitelist;
    private final ServerWorld world;
    private final ArrayDeque<PendingPacket> pendingPackets;
    private boolean scanCompleted;
    private int chunkIndex;
    private final LongArrayList chunks;
    private final Long2ObjectOpenHashMap<CompletableFuture<Optional<NbtCompound>>> pendingNbt;
    private final LongArrayFIFOQueue readyChunks;
    private long processedChunks;
    private final long totalChunks;
    private final LongOpenHashSet scannedChunks;
    private boolean fullComplete;
    private final ArrayDeque<DeltaUpdate> pendingDeltas;
    public HashSet<BlockPos> selectedBlocks = new HashSet<>();
    private final String sharedName;
    private final String whitelistName;

    private NbtScannable scannableIOWorker;

    public ScanJob(ServerPlayerEntity player, long jobId, BlockBox range, Whitelist whitelist, String sharedName, String whitelistName){
        this.jobId = jobId;
        this.owner = player;
        this.range = range;
        this.sharedName = sharedName;
        this.whitelist = whitelist;
        this.world = player.getServerWorld();
        this.pendingPackets = new ArrayDeque<>();
        this.chunks = buildChunkList(range);
        this.pendingNbt = new Long2ObjectOpenHashMap<>();
        this.readyChunks = new LongArrayFIFOQueue();
        this.scannedChunks = new LongOpenHashSet();
        this.pendingDeltas = new ArrayDeque<>();
        this.totalChunks = this.chunks.size();
        this.whitelistName = whitelistName;
        this.subscribers = new ArrayList<>();
        subscribers.add(owner);
    }

    public void tick(){
        if (!scanCompleted && !owner.networkHandler.isConnectionOpen()) {
            stop("changed world", false);
            return;
        }
        if (!scanCompleted && owner.getServerWorld() != world) {
            stop("changed world", false);
            return;
        }

        long startNs = System.nanoTime();
        long budgetNs = ServerScanConfig.getBudgetMs() * 1_000_000L;

        sendPendingPackets();
        if (timeExceeded(startNs, budgetNs)) {
            return;
        }

        if (!scanCompleted) {
            processCompletedNbtChecks();
            if (timeExceeded(startNs, budgetNs)) {
                return;
            }

            scheduleNbtChecks();
            if (timeExceeded(startNs, budgetNs)) {
                return;
            }

            scanReadyChunks(startNs, budgetNs);

            if (processedChunks >= totalChunks && pendingNbt.isEmpty() && readyChunks.isEmpty() && pendingPackets.isEmpty()) {
                SendQueue.addPacket(owner, new ScanCompletePayload(jobId));
                scanCompleted = true;
            }
        }

        if (scanCompleted) {
            sendCompletedDeltaPackets();
            if(selectedBlocks.isEmpty()){
                stop("no blocks found!", false);
            }
        }
    }

    public long getJobId(){
        return jobId;
    }

    public JobInfo getInfo(){
        return new JobInfo(jobId, sharedName, owner.getName().getString(), world.getRegistryKey().getValue().getPath(), range, whitelistName, totalChunks, processedChunks, selectedBlocks.size(), scanCompleted);
    }

    private void sendPendingPackets() {
        while (!pendingPackets.isEmpty()) {
            PendingPacket packet = pendingPackets.poll();
            if (packet != null) {

                SendQueue.addPacket(owner, new ScanChunkDataPayload(jobId, packet.positions));
            }
        }
    }

    public void subscribe(ServerPlayerEntity player){
        subscribers.remove(player);
        subscribers.add(player);
        scannedChunks.forEach(scannedChunk -> SendQueue.addPacket(player, new ScanChunkSummaryPayload(jobId, new ChunkPos(scannedChunk))));
        SendQueue.addPacket(player, new ScanCompletePayload(jobId));
        List<BlockPos> blockList = new ArrayList<>(selectedBlocks);
        int max = ServerScanConfig.getMaxDeltaPositionsPerPacket();
        for (int i = 0; i < blockList.size(); i += max) {
            int end = Math.min(i + max, blockList.size());
            List<BlockPos> batch = blockList.subList(i, end);
            SendQueue.addPacket(player, new ScanDeltaPayload(jobId, true, new ArrayList<>(batch)));
        }
    }

    public boolean canSubscribe(){
        return scanCompleted;
    }

    private void sendCompletedDeltaPackets() {
        while (!pendingDeltas.isEmpty()) {
            DeltaUpdate first = pendingDeltas.poll();
            if (first == null) {
                break;
            }
            boolean add = first.add();
            List<BlockPos> positions = new ArrayList<>();
            positions.add(first.pos());
            while (!pendingDeltas.isEmpty() && positions.size() < ServerScanConfig.getMaxDeltaPositionsPerPacket()) {
                DeltaUpdate next = pendingDeltas.peek();
                if (next == null || next.add() != add) {
                    break;
                }
                positions.add(next.pos());
                pendingDeltas.poll();
            }
            subscribers.forEach(subscriber -> SendQueue.addPacket(subscriber, new ScanDeltaPayload(jobId, add, positions)));
        }
    }

    private void processCompletedNbtChecks() {
        if (pendingNbt.isEmpty()) return;
        int completed = 0;
        LongIterator it = pendingNbt.keySet().iterator();
        while (it.hasNext() && completed < ServerScanConfig.getMaxNbtReadsPerTick()) {
            long pos = it.nextLong();
            CompletableFuture<Optional<NbtCompound>> future = pendingNbt.get(pos);
            if (future == null || !future.isDone()) continue;
            Optional<NbtCompound> nbt;
            try {
                nbt = future.getNow(Optional.empty());
            } catch (Exception e) {
                stop("Error Chunk NBT read failed for " + new ChunkPos(pos) + e, true);
                return;
            }
            it.remove();
            completed++;
            if (nbt.isPresent()) {
                readyChunks.enqueue(pos);
            } else {
                stop("Chunk " + new ChunkPos(pos) + " is not created! Cancelling", true);
                return;
            }
        }
    }

    private void scheduleNbtChecks() {
        if (chunkIndex >= chunks.size()) return;
        if (pendingNbt.size() >= ServerScanConfig.getMaxPendingNbt()) return;
        StorageIoWorker storage = getStorageIoWorker();
        if (storage == null) {
            owner.sendMessage(Text.literal((References.MOD_ID + " ScanJob->scheduleNbtChecks StorageIoWorker was null")));
            stop("ScanJob->scheduleNbtChecks StorageIoWorker was null", true);
            return;
        }
        int scheduled = 0;
        while (chunkIndex < chunks.size()
                && scheduled < ServerScanConfig.getMaxNbtReadsPerTick()
                && pendingNbt.size() < ServerScanConfig.getMaxPendingNbt()) {
            long pos = chunks.getLong(chunkIndex++);
            CompletableFuture<Optional<NbtCompound>> future = storage.readChunkData(new ChunkPos(pos));
            pendingNbt.put(pos, future);
            scheduled++;
        }
    }

    private void scanReadyChunks(long startNs, long budgetNs) {
        int loads = 0;
        while (!readyChunks.isEmpty() && loads < ServerScanConfig.getMaxChunkLoadsPerTick()) {
            if (timeExceeded(startNs, budgetNs)) {
                break;
            }
            long pos = readyChunks.dequeueLong();
            ChunkPos chunkPos = new ChunkPos(pos);
            Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
            if (!(chunk instanceof WorldChunk worldChunk)) {
                owner.sendMessage(Text.literal(References.MOD_ID + " chunk " + new ChunkPos(pos) + " is not world chunk! Cancelling"));
                stop("Chunk " + new ChunkPos(pos) + " is not world chunk! Cancelling", true);
                return;
            }
            List<BlockPos> matches = scanChunk(worldChunk, chunkPos);
            selectedBlocks.addAll(matches);
            if(selectedBlocks.size() > ServerScanConfig.getMaxSelectedBlocks()){
                stop("too many selected blocks", true);
            }

            markChunkProcessed(chunkPos);
            queueDataPackets(matches);
            scannedChunks.add(ChunkPos.toLong(chunkPos.x, chunkPos.z));
            loads++;
        }
    }

    private void markChunkProcessed(ChunkPos chunkPos) {
        processedChunks++;
        SendQueue.addPacket(owner, new ScanChunkSummaryPayload(jobId, chunkPos));
    }

    private StorageIoWorker getStorageIoWorker() {
        if(scannableIOWorker == null){
            scannableIOWorker = world.getChunkManager().getChunkIoWorker();
        }
        if (scannableIOWorker instanceof StorageIoWorker storage) {
            return storage;
        }
        return null;
    }

    public void onBlockStateChange(ServerWorld eventWorld, BlockPos pos, BlockState oldState, BlockState newState) {
        if (fullComplete) return;
        if (eventWorld != world) return;
        if (!isInRange(pos)) return;
        if (!scannedChunks.contains(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) return;
        boolean oldMatch = BlockMatcher.matches(whitelist, oldState, world, pos);
        boolean newMatch = BlockMatcher.matches(whitelist, newState, world, pos);
        if (oldMatch == newMatch) return;
        queueDelta(pos, newMatch);
    }

    private static LongArrayList buildChunkList(BlockBox range) {
        int startChunkX = range.getMinX() >> 4;
        int startChunkZ = range.getMinZ() >> 4;
        int endChunkX = range.getMaxX() >> 4;
        int endChunkZ = range.getMaxZ() >> 4;
        int total = (endChunkX - startChunkX + 1) * (endChunkZ - startChunkZ + 1);
        LongArrayList list = new LongArrayList(total);
        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                list.add(ChunkPos.toLong(chunkX, chunkZ));
            }
        }
        return list;
    }

    private void queueDelta(BlockPos pos, boolean add) {
        if(add){
            selectedBlocks.add(pos);
        }else {
            selectedBlocks.remove(pos);
        }
        pendingDeltas.add(new DeltaUpdate(add, pos.toImmutable()));
    }

    private boolean isInRange(BlockPos pos) {
        return pos.getX() >= range.getMinX()
                && pos.getX() <= range.getMaxX()
                && pos.getY() >= range.getMinY()
                && pos.getY() <= range.getMaxY()
                && pos.getZ() >= range.getMinZ()
                && pos.getZ() <= range.getMaxZ();
    }

    private void queueDataPackets(List<BlockPos> matches) {
        if (matches.isEmpty()) return;
        int index = 0;
        while (index < matches.size()) {
            int end = Math.min(index + ServerScanConfig.getMaxPositionsPerPacket(), matches.size());
            List<BlockPos> slice = new ArrayList<>(matches.subList(index, end));
            pendingPackets.add(new PendingPacket(slice));
            index = end;
        }
    }

    private List<BlockPos> scanChunk(WorldChunk chunk, ChunkPos chunkPos) {
        int minX = Math.max(chunkPos.getStartX(), range.getMinX());
        int maxX = Math.min(chunkPos.getEndX(), range.getMaxX());
        int minZ = Math.max(chunkPos.getStartZ(), range.getMinZ());
        int maxZ = Math.min(chunkPos.getEndZ(), range.getMaxZ());
        int minY = Math.max(range.getMinY(), world.getBottomY());
        int maxY = Math.min(range.getMaxY(), world.getTopYInclusive());
        if (minY > maxY) {
            return List.of();
        }

        List<BlockPos> matches = new ArrayList<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (BlockMatcher.matches(whitelist, chunk.getBlockState(pos), world, pos)) {
                        matches.add(pos.toImmutable());
                    }
                }
            }
        }
        return matches;
    }

    public boolean isComplete() {
        return fullComplete;
    }

    private static boolean timeExceeded(long startNs, long budgetNs) {
        return System.nanoTime() - startNs > budgetNs;
    }

    public void stop(String cause, boolean restart){
        subscribers.forEach(subscriber -> SendQueue.addPacket(subscriber, new ScanFullCompletedPayload(jobId, cause, restart)));
        fullComplete = true;
    }

    private record PendingPacket(List<BlockPos> positions) {}
    private record DeltaUpdate(boolean add, BlockPos pos) {}

}
