package ru.obabok.areascanner.server;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import ru.obabok.areascanner.common.model.JobInfo;
import ru.obabok.areascanner.common.model.Whitelist;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ServerScanManager {
    private static final ServerScanManager INSTANCE = new ServerScanManager();
    private final Map<UUID, ScanJob> jobs = new ConcurrentHashMap<>();

    public static ServerScanManager getInstance() {
        return INSTANCE;
    }

    public void startJob(ServerPlayerEntity player, long jobId, BlockBox range, String whitelistName, Whitelist whitelist, String sharedName) {
        jobs.remove(player.getUuid());
        ScanJob job = new ScanJob(player, jobId, range, whitelist, sharedName, whitelistName);
        jobs.put(player.getUuid(), job);
    }

    public void stopJob(ServerPlayerEntity player, long jobId, String cause){
        ScanJob job = jobs.get(player.getUuid());
        if(job != null && job.getJobId() == jobId){
            job.stop(cause, true);
            jobs.remove(player.getUuid());
        }
    }

    public void stopOPJob(long jobId){
        jobs.forEach((uuid, scanJob) -> {
            if(scanJob.getJobId() == jobId){
                scanJob.stop("The Admin said so", true);
                jobs.remove(scanJob.getOwner().getUuid());
            }
        });
    }

    public void onBlockStateChange(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState){
        jobs.forEach((uuid, scanJob) -> {
            scanJob.onBlockStateChange(world, pos, oldState, newState);
        });
    }

    public ArrayList<JobInfo> getJobs(){
        ArrayList<JobInfo> jobInfos = new ArrayList<>();
        jobs.forEach((uuid, scanJob) -> {
            jobInfos.add(scanJob.getInfo());
        });
        return jobInfos;
    }

    public JobInfo getJobInfo(long jobId){
        AtomicReference<JobInfo> info = new AtomicReference<>();
        jobs.forEach((uuid, scanJob) -> {
            if(scanJob.getJobId() == jobId){
                info.set(scanJob.getInfo());
            }
        });
        return info.get();
    }

    public String subscribe(ServerPlayerEntity player, long jobId){
        AtomicBoolean subscribed = new AtomicBoolean(false);
        jobs.forEach((uuid, scanJob) -> {
            if(scanJob.getJobId() == jobId){
                if(scanJob.canSubscribe()){
                    scanJob.subscribe(player);
                    subscribed.set(true);
                }
            }
        });
        return subscribed.get() ? null : "wrong job id";
    }

    public void unsubscribe(ServerPlayerEntity player, long jobId){
        jobs.forEach((uuid, scanJob) -> {
            if(scanJob.getJobId() == jobId){
                scanJob.unsubscribe(player);
            }
        });
    }

    public void tick() {
        if (jobs.isEmpty()) return;
        for (ScanJob job : jobs.values()) {
            job.tick();
        }
        jobs.values().removeIf(ScanJob::isComplete);
    }
}
