package com.ryan.media.demo;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
@Repository
public class DemoJobRepository {
    private final Map<String,DemoJob> jobs=new ConcurrentHashMap<>();
    private final Map<String,String> keys=new ConcurrentHashMap<>();
    public void save(DemoJob job){jobs.put(job.getJobId(),job);keys.putIfAbsent(job.getIdempotencyKey(),job.getJobId());}
    public DemoJob require(String id){DemoJob j=jobs.get(id);if(j==null)throw new DemoJobException("JOB_NOT_FOUND",id);return j;}
    public DemoJob byKey(String key){String id=keys.get(key);return id==null?null:jobs.get(id);}
    public Collection<DemoJob> all(){return jobs.values();}
}
