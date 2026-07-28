package com.ryan.media.demo;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
@Component
public class DemoProcessTreeTerminator {
    public TerminationResult terminate(Process process, Duration grace) {
        List<ProcessHandle> descendants=process.descendants().toList();int terminated=0;
        for (ProcessHandle child:descendants) if(child.destroy())terminated++;
        try { process.waitFor(grace.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        for (ProcessHandle child:descendants) if(child.isAlive() && child.destroyForcibly())terminated++;
        if(process.isAlive())process.destroy();
        try { process.waitFor(grace.toMillis(),TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if(process.isAlive())process.destroyForcibly();
        int alive=(int)descendants.stream().filter(ProcessHandle::isAlive).count()+(process.isAlive()?1:0);
        return new TerminationResult(descendants.size(),terminated,alive);
    }
    public record TerminationResult(int discovered,int terminated,int stillAlive){}
}
