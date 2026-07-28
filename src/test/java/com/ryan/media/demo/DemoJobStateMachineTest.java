package com.ryan.media.demo;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
class DemoJobStateMachineTest {
    private DemoJob job(){
        MainbaseDemoHandoff h=new MainbaseDemoHandoff("v","c","d",List.of(),0,0,0);
        return new DemoJob("j","k","f",new DemoJobRequest("c","REPLAY",10,true),h,
                DemoPublishDecision.PROVISIONAL_SELECTED,"cfg","impl");
    }
    @Test void successCannotReturnToRunning(){
        DemoJob j=job();DemoJobStateMachine s=new DemoJobStateMachine();
        s.transition(j,DemoJobStatus.QUEUED);s.transition(j,DemoJobStatus.STARTING);s.transition(j,DemoJobStatus.RUNNING);
        s.transition(j,DemoJobStatus.FINALIZING);s.transition(j,DemoJobStatus.SUCCEEDED);
        assertThrows(DemoJobException.class,()->s.transition(j,DemoJobStatus.RUNNING));
    }
    @Test void failedCanRetryToQueue(){
        DemoJob j=job();DemoJobStateMachine s=new DemoJobStateMachine();
        s.transition(j,DemoJobStatus.QUEUED);s.transition(j,DemoJobStatus.STARTING);s.transition(j,DemoJobStatus.FAILED);
        s.transition(j,DemoJobStatus.QUEUED);assertEquals(DemoJobStatus.QUEUED,j.getExecutionStatus());
    }
    @Test void acceptedMayBlock(){DemoJob j=job();new DemoJobStateMachine().transition(j,DemoJobStatus.BLOCKED);assertEquals(DemoJobStatus.BLOCKED,j.getExecutionStatus());}
}
