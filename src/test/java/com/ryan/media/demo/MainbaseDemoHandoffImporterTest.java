package com.ryan.media.demo;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
class MainbaseDemoHandoffImporterTest {
    @Test void importsAndPreservesTwelveTenTwoZero(){
        var importer=new MainbaseDemoHandoffImporter(new ObjectMapper(),"../audio_engineering_repo_skeleton_v1",".",
                "artifacts/manifests/dss_rerank_repair_handoff_20260722.json");
        var h=importer.importHandoff();assertEquals(12,h.records().size());assertEquals(10,h.provisionalCount());
        assertEquals(2,h.blockedCount());assertEquals(0,h.finalSelectedCount());
        assertTrue(h.records().stream().filter(x->x.repairArtifact()!=null).allMatch(x->"SHA256_VERIFIED".equals(x.repairArtifact().integrityStatus())));
    }
}
