package community.leo.obd.core;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** At most one worker and one pending snapshot, even when storage is slow. */
public final class LatestSnapshotWriter {
    public interface Sink { void save(String snapshot) throws IOException; }
    private final Executor executor;
    private final Sink sink;
    private final Consumer<IOException> failure;
    private String pending;
    private boolean running;
    public LatestSnapshotWriter(Executor executor,Sink sink,Consumer<IOException> failure){
        this.executor=executor;this.sink=sink;this.failure=failure;
    }
    public synchronized void offer(String snapshot){
        if(snapshot==null)throw new IllegalArgumentException("Missing snapshot");
        pending=snapshot;
        if(!running){running=true;executor.execute(this::drain);}
    }
    private void drain(){
        while(true){
            String snapshot;
            synchronized(this){snapshot=pending;pending=null;if(snapshot==null){running=false;return;}}
            try{sink.save(snapshot);}catch(IOException error){failure.accept(error);}
        }
    }
}
