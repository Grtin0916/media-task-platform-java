package com.ryan.media.demo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class DemoRunnerCommandFactoryTest {
    @Test void buildsFixedArgumentArray(){
        DemoRunnerProperties p=new DemoRunnerProperties("python3","../audio_engineering_repo_skeleton_v1",".","scripts/run_demo.py","configs/demo/runner.yaml",2,100);
        DemoRunnerCommandFactory f=new DemoRunnerCommandFactory(p);
        DemoResultSeed seed=new DemoResultSeed("r","forest_bird_branch_001","MANUAL_REVIEW",DemoPublishDecision.PROVISIONAL_SELECTED,null,null,"x",true,false,false,false);
        var command=f.command(seed);assertEquals("python3",command.get(0));assertTrue(command.contains("forest_bird_branch_001"));assertFalse(command.contains("shell=True"));
    }
    @Test void rejectsUnsafeCaseId(){
        DemoRunnerProperties p=new DemoRunnerProperties("python3","../audio_engineering_repo_skeleton_v1",".","s","c",2,100);
        DemoResultSeed seed=new DemoResultSeed("r","../bad","MANUAL_REVIEW",DemoPublishDecision.PROVISIONAL_SELECTED,null,null,"x",true,false,false,false);
        assertThrows(DemoJobException.class,()->new DemoRunnerCommandFactory(p).command(seed));
    }
}
