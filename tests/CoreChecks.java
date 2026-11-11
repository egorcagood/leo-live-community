import community.leo.obd.core.*;
import java.util.*;

public final class CoreChecks {
    static int checks;
    static void yes(boolean condition) { checks++; if(!condition)throw new AssertionError("Check "+checks); }
    static void equal(Object a,Object b){yes(Objects.equals(a,b));}
    static void reject(Runnable action){checks++;try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Expected rejection "+checks);}
    public static void main(String[] args) throws Exception {
        for(String command:List.of("04","ATSH7E0","2701","2E0000","1101","3101","1A86","2101","010C\r04","010C\n","010c"," 010C","ATZ\u0000","010C;04"))reject(()->ReadPolicy.frame(command));
        equal(new String(ReadPolicy.frame("010C"),java.nio.charset.StandardCharsets.US_ASCII),"010C\r");
        equal(ObdCodec.value("410C0BB8",0x0c),750.0);
        equal(ObdCodec.value("0105\r41057E\r>",5),86.0);
        equal(ObdCodec.value("410D00",13),0.0);
        equal(ObdCodec.value("410CFFFF",12),16383.75);
        equal(ObdCodec.value("410500",5),-40.0);
        equal(ObdCodec.value("4104FF",4),100.0);
        equal(ObdCodec.value("411100",17),0.0);
        equal(ObdCodec.value("412FFF",47),100.0);
        equal(ObdCodec.value("414236B0",66),14.0);
        equal(ObdCodec.value("410DFF",13),255.0);
        reject(()->ObdCodec.value("7E804410C0BB8",12));
        equal(ObdCodec.value("410C0BB8\r410C0BB8",12),750.0);
        reject(()->ObdCodec.value("410C0BB8\r410C0BC0",12));
        reject(()->ObdCodec.value("NO DATA",12));
        reject(()->ObdCodec.value("410C0B",12));
        reject(()->ObdCodec.value("410C0BB800",12));
        reject(()->ObdCodec.value("410D80",12));
        reject(()->ObdCodec.value("410C0BB8\rgarbage",12));
        equal(ObdCodec.supported("410000100001",0),Set.of(12,32));
        reject(()->ObdCodec.supported("410000100001\r410000000000",0));
        reject(()->ObdCodec.supported("412000100001",0));
        equal(ObdCodec.dtc("4301710000","03"),List.of("P0171"));
        equal(ObdCodec.dtc("470000","07"),List.of());
        equal(ObdCodec.dtc("430171\r430300","03"),List.of("P0171","P0300"));
        reject(()->ObdCodec.dtc("NO DATA","03"));
        reject(()->ObdCodec.dtc("4301","03"));
        reject(()->ObdCodec.dtc("440000","03"));
        reject(()->ObdCodec.dtc("430000","04"));
        SessionLog demo=new SessionLog("DEMO",2),real=new SessionLog("OBD",2);
        demo.append(1,"rpm","750","rpm","OK");demo.append(2,"speed","0","km/h","OK");demo.append(3,"coolant","86","C","OK");
        yes(!demo.csv().contains("1,DEMO,rpm"));yes(demo.csv().contains("DEMO"));equal(real.size(),0);
        reject(()->real.append(-1,"rpm","1","rpm","OK"));
        reject(()->real.append(1,"=FORMULA","1","rpm","OK"));
        reject(()->real.append(1,"rpm","1\nleak","rpm","OK"));
        for(int p:ObdCodec.PIDS)yes(Double.isFinite(ObdCodec.value(DemoVehicle.response(p,12),p)));
        reject(()->DemoVehicle.response(0x99,0));
        RequestQueue q=new RequestQueue();q.enqueue("010C");q.enqueue("0105");
        equal(q.next(0),"010C");equal(q.next(1),null);
        equal(q.receive("410C",2),null);
        RequestQueue.Reply reply=q.receive("0BB8\r>",3);equal(reply.command(),"010C");equal(reply.body(),"410C0BB8\r");
        equal(q.next(4),"0105");yes(!q.expire(10003));yes(q.expire(10004));yes(q.failed());equal(q.next(10005),null);
        RequestQueue cancelled=new RequestQueue();cancelled.enqueue("010C");cancelled.next(1);cancelled.cancel();equal(cancelled.receive("410C0BB8>",2),null);equal(cancelled.next(2),null);
        RequestQueue overflow=new RequestQueue();overflow.enqueue("010C");overflow.next(0);equal(overflow.receive("A".repeat(4097),1),null);yes(overflow.failed());
        RequestQueue late=new RequestQueue();late.enqueue("010C");late.next(0);equal(late.receive("410C0BB8>",10000),null);yes(late.failed());
        RequestQueue unsolicited=new RequestQueue();equal(unsolicited.receive("410C0BB8>",0),null);yes(unsolicited.failed());
        RequestQueue doubleReply=new RequestQueue();doubleReply.enqueue("010C");doubleReply.next(0);equal(doubleReply.receive("410C0BB8>410C0BB8>",1),null);yes(doubleReply.failed());
        RequestQueue unsafe=new RequestQueue();reject(()->unsafe.enqueue("04"));
        RequestQueue full=new RequestQueue();for(int i=0;i<16;i++)full.enqueue("010C");reject(()->full.enqueue("0105"));
        java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("community-log-test");
        try{
            LocalLogStore store=new LocalLogStore(dir.toFile());equal(store.read(),null);
            store.save(demo.csv());equal(store.read(),demo.csv());
            reject(()->{try{store.save("not a CSV");}catch(java.io.IOException e){throw new AssertionError(e);}});
            equal(store.read(),demo.csv());
        }finally{try(var paths=java.nio.file.Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{java.nio.file.Files.delete(p);}catch(Exception e){throw new AssertionError(e);}});}}
        List<Runnable> tasks=new ArrayList<>();List<String> saved=new ArrayList<>();
        LatestSnapshotWriter latest=new LatestSnapshotWriter(tasks::add,saved::add,e->{throw new AssertionError(e);});
        latest.offer("first");latest.offer("second");latest.offer("last");equal(tasks.size(),1);
        tasks.remove(0).run();equal(saved,List.of("last"));
        latest.offer("after");equal(tasks.size(),1);tasks.remove(0).run();equal(saved,List.of("last","after"));
        List<String> failures=new ArrayList<>();
        LatestSnapshotWriter broken=new LatestSnapshotWriter(tasks::add,s->{throw new java.io.IOException("disk");},e->failures.add(e.getMessage()));
        broken.offer("x");tasks.remove(0).run();equal(failures,List.of("disk"));
        broken.offer("y");equal(tasks.size(),1);tasks.remove(0).run();equal(failures.size(),2);
        System.out.println("PASS "+checks+" core checks");
    }
}
