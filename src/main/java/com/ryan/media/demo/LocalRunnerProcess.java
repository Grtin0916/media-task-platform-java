package com.ryan.media.demo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
@Component
public class LocalRunnerProcess {
    private final DemoProcessTreeTerminator terminator;
    public LocalRunnerProcess(DemoProcessTreeTerminator terminator){this.terminator=terminator;}
    public Running start(List<String> command,Path cwd,Path attemptDir) {
        try {
            Files.createDirectories(attemptDir);
            Path stdout=attemptDir.resolve("stdout.log"),stderr=attemptDir.resolve("stderr.log");
            Process p=new ProcessBuilder(command).directory(cwd.toFile())
                    .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
            return new Running(p,stdout,stderr,Instant.now());
        } catch(IOException e){throw new DemoJobException("PROCESS_START_FAILED",e.getMessage());}
    }
    public Outcome await(Running running,Duration timeout) {
        try {
            if(!running.process().waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS)){
                var t=terminator.terminate(running.process(),Duration.ofMillis(300));
                return new Outcome(null,true,t);
            }
            return new Outcome(running.process().exitValue(),false,new DemoProcessTreeTerminator.TerminationResult(0,0,0));
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new DemoJobException("PROCESS_INTERRUPTED",e.getMessage());}
    }
    public DemoProcessTreeTerminator.TerminationResult cancel(Running running,Duration grace){return terminator.terminate(running.process(),grace);}
    public record Running(Process process,Path stdout,Path stderr,Instant startedAt){}
    public record Outcome(Integer exitCode,boolean timedOut,DemoProcessTreeTerminator.TerminationResult termination){}
}
