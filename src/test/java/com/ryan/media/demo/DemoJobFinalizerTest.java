package com.ryan.media.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoJobFinalizerTest {
    @TempDir Path temp;

    @Test
    void exitZeroWithoutManifestIsRejected() throws Exception {
        Path stdout=stdout("missing");
        DemoJobException error=assertThrows(DemoJobException.class,
                ()->finalizer().finalizeResult(running(stdout)));
        assertEquals("RUN_MANIFEST_MISSING",error.code());
    }

    @Test
    void mismatchedOutputDigestIsRejected() throws Exception {
        Path output=temp.resolve("outputs/value.bin");
        Files.createDirectories(output.getParent());
        Files.writeString(output,"actual");
        Path manifest=temp.resolve("artifacts/runs/bad/run_manifest.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest,"""
                {"status":"SUCCEEDED","stages":[{"stage_id":"publish","status":"SUCCEEDED",
                "execution_mode":"EXECUTE","reused":false,"input_digests":{},
                "output_digests":{"outputs/value.bin":"sha256:0000"},"duration_ms":1,"failure":null}]}
                """);
        DemoJobException error=assertThrows(DemoJobException.class,
                ()->finalizer().finalizeResult(running(stdout("bad"))));
        assertEquals("RESULT_DIGEST_MISMATCH",error.code());
    }

    private DemoJobFinalizer finalizer(){
        return new DemoJobFinalizer(new ObjectMapper(),
                new DemoRunnerProperties("python3",temp.toString(),temp.toString(),
                        "runner.py","config.json",1,100));
    }
    private Path stdout(String runId)throws Exception{
        Path path=temp.resolve("stdout-"+runId+".log");
        Files.writeString(path,"{\"run_id\":\""+runId+"\"}");
        return path;
    }
    private LocalRunnerProcess.Running running(Path stdout)throws Exception{
        Process process=new ProcessBuilder("true").start();
        process.waitFor();
        Path stderr=temp.resolve("stderr.log");
        Files.writeString(stderr,"");
        return new LocalRunnerProcess.Running(process,stdout,stderr,Instant.now());
    }
}
