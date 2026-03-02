package ru.obabok.areascanner.server;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
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
import ru.obabok.areascanner.common.network.payloads.s2c.ScanChunkSummaryPayload;
import ru.obabok.areascanner.common.network.payloads.s2c.ScanDeltaPayload;
import ru.obabok.areascanner.common.network.payloads.s2c.ScanFullCompletedPayload;
import ru.obabok.areascanner.server.network.SendQueue;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FastScanJob {
    private final long jobId;
    private final ServerPlayerEntity owner;
    private final ArrayList<ServerPlayerEntity> subscribers;
    private final BlockBox range;
    private final Whitelist whitelist;
    private final ServerWorld world;
    private boolean scanCompleted;
    private final Long2ObjectOpenHashMap<CompletableFuture<Optional<NbtCompound>>> pendingNbt;
    private final LongArrayFIFOQueue readyChunks;
    private long processedChunks;
    private final LongOpenHashSet scannedChunks;
    private boolean fullComplete;
    private final LongArrayFIFOQueue pendingDeltaPos = new LongArrayFIFOQueue();
    private final IntArrayFIFOQueue pendingDeltaTypes = new IntArrayFIFOQueue();
    public LongOpenHashSet selectedBlocks = new LongOpenHashSet();
    private final String sharedName;
    private final String whitelistName;
    private final Object2IntOpenHashMap<Block> materialList = new Object2IntOpenHashMap<>();
    private final LongArrayList deltaBuffer = new LongArrayList();
    private final LongArrayList activeNbtReads = new LongArrayList();
    private NbtScannable scannableIOWorker;
    private final ChunkCursor chunkCursor;
    private int sendCooldown = 1;

    public FastScanJob(ServerPlayerEntity player, long jobId, BlockBox range, Whitelist whitelist, String sharedName, String whitelistName){
        this.jobId = jobId;
        this.owner = player;
        this.range = range;
        this.sharedName = sharedName;
        this.whitelist = whitelist;
        this.world = player.getServerWorld();
        chunkCursor = new ChunkCursor(range);
        this.pendingNbt = new Long2ObjectOpenHashMap<>();
        this.readyChunks = new LongArrayFIFOQueue();
        this.scannedChunks = new LongOpenHashSet();
        this.whitelistName = whitelistName;
        this.subscribers = new ArrayList<>();
        subscribers.add(owner);
    }

    public ServerWorld getWorld(){
        return world;
    }
    public String getSharedName(){
        return sharedName;
    }

    public long getJobId(){
        return jobId;
    }

    public ServerPlayerEntity getOwner(){
        return owner;
    }

    public JobInfo getInfo(){
        return new JobInfo(jobId, sharedName, owner.getName().getString(), owner.getUuid(), world.getRegistryKey().getValue().getPath(), range, whitelistName, chunkCursor.size(), processedChunks, selectedBlocks.size(), scanCompleted);
    }

    public void tick(){
        long startNs = System.nanoTime();
        long budgetNs = ServerScanConfig.getBudgetMs() * 1_000_000L;

        sendCooldown--;
        if(sendCooldown <= 0){
            updateChunkProcess();
            sendCooldown = 20;
        }
        sendDeltaDataPackets();

        if (!scanCompleted) {
            processCompletedNbtChecks();
            if (timeExceeded(startNs, budgetNs)) {
                return;
            }
            scanReadyChunks(startNs, budgetNs);
            if (timeExceeded(startNs, budgetNs)) {
                return;
            }

            scheduleNbtChecks();

            if (processedChunks >= chunkCursor.size() && pendingNbt.isEmpty() && readyChunks.isEmpty()) {
                scanCompleted = true;
                cleanup();
                subscribe(owner);
            }
        }

        if (scanCompleted) {
            if(selectedBlocks.isEmpty()){
                stop("no blocks found!", false);
            }
        }
    }

    private void processCompletedNbtChecks() {
        if (activeNbtReads.isEmpty()) return;

        int completed = 0;
        for (int i = activeNbtReads.size() - 1; i >= 0; i--) {
            if (completed >= ServerScanConfig.getMaxNbtReadsPerTick()) break;

            long pos = activeNbtReads.getLong(i);
            CompletableFuture<Optional<NbtCompound>> future = pendingNbt.get(pos);

            if (future == null || !future.isDone()) continue;

            try {
                Optional<NbtCompound> nbt = future.getNow(Optional.empty());
                pendingNbt.remove(pos);
                activeNbtReads.removeLong(i);
                completed++;

                if (nbt.isPresent()) {
                    readyChunks.enqueue(pos);
                } else {
                    stop("Chunk " + new ChunkPos(pos) + " missing", true);
                    return;
                }
            } catch (Exception e) {
                stop("NBT Read Error", true);
                return;
            }
        }
    }

    public Object2IntOpenHashMap<Block> getMaterialList(){
        return materialList;
    }

    private void scheduleNbtChecks() {
        if (pendingNbt.size() >= ServerScanConfig.getMaxPendingNbt()) return;

        StorageIoWorker storage = getStorageIoWorker();
        if (storage == null) return;

        int scheduled = 0;
        while (chunkCursor.hasNext()
                && scheduled < ServerScanConfig.getMaxNbtReadsPerTick()
                && pendingNbt.size() < ServerScanConfig.getMaxPendingNbt()) {
                long pos = chunkCursor.nextLong();
                CompletableFuture<Optional<NbtCompound>> future = storage.readChunkData(new ChunkPos(pos));
                pendingNbt.put(pos, future);
                activeNbtReads.add(pos); // Запоминаем, что этот чанк в работе
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

            scanChunk(worldChunk, chunkPos, deltaBuffer);
            selectedBlocks.addAll(deltaBuffer);

            if(selectedBlocks.size() > ServerScanConfig.getMaxSelectedBlocks()){
                stop("too many selected blocks", true);
            }

            processedChunks++;
            scannedChunks.add(ChunkPos.toLong(chunkPos.x, chunkPos.z));
            loads++;
        }
    }

    private void sendDeltaDataPackets() {
        while (!pendingDeltaTypes.isEmpty()) {
            // Берем первый тип и позицию
            int currentType = pendingDeltaTypes.dequeueInt();
            boolean isAdd = (currentType == 1);

            deltaBuffer.clear();
            deltaBuffer.add(pendingDeltaPos.dequeueLong());

            // Собираем пачку, пока типы совпадают
            while (!pendingDeltaTypes.isEmpty() && deltaBuffer.size() < ServerScanConfig.getMaxDeltaPositionsPerPacket()) {
                // "Подсматриваем" тип следующего элемента
                int nextType = pendingDeltaTypes.firstInt();
                if (nextType != currentType) break;

                // Если тип такой же, забираем его из обеих очередей
                pendingDeltaTypes.dequeueInt();
                deltaBuffer.add(pendingDeltaPos.dequeueLong());
            }

            ScanDeltaPayload payload = new ScanDeltaPayload(jobId, isAdd, deltaBuffer.toLongArray());

            for (int i = 0; i < subscribers.size(); i++) {
                SendQueue.addPacket(subscribers.get(i), payload);
            }
        }
    }

    private void updateChunkProcess(){
        for (int i = 0; i < subscribers.size(); i++) {
            if(subscribers.get(i).getServerWorld() != world){
                ServerPlayNetworking.send(subscribers.get(i), new ScanFullCompletedPayload(jobId, "Wrong world", false));
                unsubscribe(subscribers.get(i));
                return;
            }
            SendQueue.addPacket(subscribers.get(i), new ScanChunkSummaryPayload(jobId, processedChunks));
        }
    }

    public void unsubscribe(ServerPlayerEntity playerEntity){
        subscribers.remove(playerEntity);
    }

    public void subscribe(ServerPlayerEntity player){
        subscribers.remove(player);
        subscribers.add(player);
        LongArrayList blockList = new LongArrayList(selectedBlocks);
        int max = ServerScanConfig.getMaxDeltaPositionsPerPacket();
        for (int i = 0; i < blockList.size(); i += max) {
            int end = Math.min(i + max, blockList.size());
            LongList batch = blockList.subList(i, end);
            SendQueue.addPacket(player, new ScanDeltaPayload(jobId, true, batch.toLongArray()));
        }
    }

    private void scanChunk(WorldChunk chunk, ChunkPos chunkPos, LongArrayList output) {
        output.clear();
        int minX = Math.max(chunkPos.getStartX(), range.getMinX());
        int maxX = Math.min(chunkPos.getEndX(), range.getMaxX());
        int minZ = Math.max(chunkPos.getStartZ(), range.getMinZ());
        int maxZ = Math.min(chunkPos.getEndZ(), range.getMaxZ());
        int minY = Math.max(range.getMinY(), world.getBottomY());
        int maxY = Math.min(range.getMaxY(), world.getTopYInclusive());
        if (minY > maxY) {
            return;
        }

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (BlockMatcher.matches(whitelist, chunk.getBlockState(pos), world, pos)) {
                        long p = pos.asLong();
                        output.add(p);
                        materialList.addTo(chunk.getBlockState(pos).getBlock(), 1);
                    }
                }
            }
        }
    }

    public void stop(String cause, boolean restart){
        ScanFullCompletedPayload payload = new ScanFullCompletedPayload(jobId, cause, restart);
        for (int i = 0; i < subscribers.size(); i++) {
            SendQueue.addPacket(subscribers.get(i), payload);
        }
        fullComplete = true;
    }

    private void cleanup(){
        readyChunks.clear();
        readyChunks.trim();
        pendingNbt.clear();
        pendingNbt.trim();
        activeNbtReads.clear();
        activeNbtReads.trim();
        deltaBuffer.clear();
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

    public boolean isFullComplete() {
        return fullComplete;
    }

    private static boolean timeExceeded(long startNs, long budgetNs) {
        return System.nanoTime() - startNs > budgetNs;
    }

    public void onBlockStateChange(ServerWorld eventWorld, BlockPos pos, BlockState oldState, BlockState newState) {
        if (fullComplete) return;
        if (eventWorld != world) return;
        if (!isInRange(pos)) return;
        if (!scanCompleted && !scannedChunks.contains(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) return;

        boolean oldMatch = BlockMatcher.matches(whitelist, oldState, world, pos);
        boolean newMatch = BlockMatcher.matches(whitelist, newState, world, pos);
        if (oldMatch == newMatch) return;
        queueDelta(pos, newMatch, oldState, newState);
    }

    private void queueDelta(BlockPos pos, boolean add, BlockState oldState, BlockState newState) {
        long posLong = pos.asLong();
        if (add) {
            selectedBlocks.add(posLong);
            materialList.addTo(newState.getBlock(), 1);
        } else {
            selectedBlocks.remove(posLong);
            materialList.addTo(oldState.getBlock(), -1);
        }

        // Записываем тип (1 или 0) и координаты
        pendingDeltaTypes.enqueue(add ? 1 : 0);
        pendingDeltaPos.enqueue(posLong);
    }

    private boolean isInRange(BlockPos pos) {
        return pos.getX() >= range.getMinX()
                && pos.getX() <= range.getMaxX()
                && pos.getY() >= range.getMinY()
                && pos.getY() <= range.getMaxY()
                && pos.getZ() >= range.getMinZ()
                && pos.getZ() <= range.getMaxZ();
    }


    private static class ChunkCursor {
        private final int startX;
        private final int startZ;
        private final int width;
        private final int total;
        private int current = 0;

        public ChunkCursor(BlockBox range) {
            this.startX = range.getMinX() >> 4;
            this.startZ = range.getMinZ() >> 4;
            int endX = range.getMaxX() >> 4;
            int endZ = range.getMaxZ() >> 4;
            this.width = (endX - startX + 1);
            this.total = width * (endZ - startZ + 1);
        }

        public int size() {
            return total;
        }

        public boolean hasNext() {
            return current < total;
        }

        public long nextLong() {
            int cz = current / width;
            int cx = current % width;
            current++;
            return ChunkPos.toLong(startX + cx, startZ + cz);
        }
    }

}
