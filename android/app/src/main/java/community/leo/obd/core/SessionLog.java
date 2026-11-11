package community.leo.obd.core;
import java.util.ArrayDeque;
import java.util.Arrays;

public final class SessionLog {
    private final String source;private final int capacity;private final ArrayDeque<String> rows=new ArrayDeque<>();
    public SessionLog(String source,int capacity){
        if(!Arrays.asList("DEMO","OBD").contains(source)||capacity<1||capacity>10000)throw new IllegalArgumentException("Invalid log");
        this.source=source;this.capacity=capacity;
    }
    public synchronized void append(long time,String field,String value,String unit,String status){
        if(time<0||field==null||!field.matches("[a-z_]{1,40}")||value==null||!value.matches("[-0-9.A-Z;]{0,200}")
            ||unit==null||!unit.matches("[A-Za-z/%]{0,16}")||status==null||!status.matches("[A-Z_]{1,40}"))throw new IllegalArgumentException("Invalid log row");
        if(rows.size()==capacity)rows.removeFirst();rows.add(time+","+source+","+field+","+value+","+unit+","+status+"\n");
    }
    public synchronized int size(){return rows.size();}
    public synchronized String csv(){return "timestamp_ms,source,field,value,unit,status\n"+String.join("",rows);}
}
