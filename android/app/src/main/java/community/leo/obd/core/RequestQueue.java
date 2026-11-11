package community.leo.obd.core;
import java.util.ArrayDeque;

/** Single connection epoch. A failed/cancelled instance can never transmit again. */
public final class RequestQueue {
    public record Reply(String command,String body){}
    private final ArrayDeque<String> pending=new ArrayDeque<>();
    private final StringBuilder buffer=new StringBuilder();
    private String current;private long started;private boolean failed;
    public void enqueue(String command){
        ReadPolicy.frame(command);
        if(failed||pending.size()>=16)throw new IllegalArgumentException("Queue unavailable");pending.add(command);
    }
    public String next(long now){
        if(failed||current!=null||pending.isEmpty())return null;
        current=pending.remove();started=now;return current;
    }
    public Reply receive(String chunk,long now){
        if(failed)return null;
        if(current==null||chunk==null||expire(now)){cancel();return null;}
        for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c>126||c<32&&c!='\r'&&c!='\n'&&c!='\t'){cancel();return null;}}
        buffer.append(chunk);if(buffer.length()>4096){cancel();return null;}
        int prompt=buffer.indexOf(">");if(prompt<0)return null;
        if(!buffer.substring(prompt+1).trim().isEmpty()){cancel();return null;}
        Reply reply=new Reply(current,buffer.substring(0,prompt));current=null;buffer.setLength(0);return reply;
    }
    public boolean expire(long now){
        if(!failed&&current!=null&&(now<started||now-started>=10000)){cancel();return true;}return false;
    }
    public boolean idle(){return !failed&&current==null&&pending.isEmpty();}
    public boolean failed(){return failed;}
    public void cancel(){failed=true;pending.clear();current=null;buffer.setLength(0);}
}
