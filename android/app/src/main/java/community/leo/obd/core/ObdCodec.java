package community.leo.obd.core;

import java.util.*;

/** Headerless ELM responses only. Unknown framing is rejected, never guessed. */
public final class ObdCodec {
    public static final List<Integer> PIDS=Collections.unmodifiableList(Arrays.asList(0x0c,0x0d,5,4,0x11,0x2f,0x42));
    private ObdCodec(){}
    public static String command(int pid){return String.format(Locale.ROOT,"01%02X",pid);}
    public static List<String> lines(String body,String echo){
        if(body==null||body.length()>4096)throw new IllegalArgumentException("Invalid response size");
        List<String> lines=new ArrayList<>();
        for(String line:body.replace('>','\n').split("[\r\n]+")){
            String s=line.trim().replace(" ","").replace("\t","").toUpperCase(Locale.ROOT);
            if(s.isEmpty()||s.equals(echo)||s.equals("SEARCHING..."))continue;
            lines.add(s);
        }
        if(lines.isEmpty())throw new IllegalArgumentException("Empty response");
        return lines;
    }
    private static byte[] hex(String s){
        if(s.length()%2!=0||!s.matches("[0-9A-F]+"))throw new IllegalArgumentException("Unsupported response");
        byte[] bytes=new byte[s.length()/2];
        for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);
        return bytes;
    }
    private static byte[] scalar(String body,int pid,int count){
        byte[] accepted=null;
        for(String line:lines(body,command(pid))){
            byte[] b=hex(line);
            if(b.length!=count+2||(b[0]&255)!=0x41||(b[1]&255)!=pid)throw new IllegalArgumentException("Unexpected PID or length");
            if(accepted!=null&&!Arrays.equals(accepted,b))throw new IllegalArgumentException("Conflicting ECU replies");
            accepted=b;
        }
        return accepted;
    }
    public static Set<Integer> supported(String body,int base){
        if(!Arrays.asList(0,32,64,96).contains(base))throw new IllegalArgumentException("Invalid map");
        byte[] b=scalar(body,base,4);Set<Integer> result=new LinkedHashSet<>();
        for(int i=0;i<32;i++)if(((b[2+i/8]&255)&(0x80>>>(i%8)))!=0)result.add(base+i+1);
        return result;
    }
    public static double value(String body,int pid){
        if(!PIDS.contains(pid))throw new IllegalArgumentException("Unsupported PID");
        byte[] b=scalar(body,pid,pid==12||pid==66?2:1);int a=b[2]&255;
        return switch(pid){case 12->(a*256+(b[3]&255))/4.0;case 66->(a*256+(b[3]&255))/1000.0;
            case 5->a-40.0;case 4,17,47->a*100.0/255.0;default->(double)a;};
    }
    public static List<String> dtc(String body,String request){
        if(!Arrays.asList("03","07").contains(request))throw new IllegalArgumentException("Unsupported DTC read");
        int expected=Integer.parseInt(request,16)+0x40;Set<String> codes=new LinkedHashSet<>();
        for(String line:lines(body,request)){
            byte[] b=hex(line);
            if(b.length<3||(b[0]&255)!=expected||(b.length-1)%2!=0)throw new IllegalArgumentException("Unsupported DTC framing");
            for(int i=1;i<b.length;i+=2){int a=b[i]&255,z=b[i+1]&255;if(a==0&&z==0)continue;
                codes.add("PCBU".charAt(a>>>6)+String.format(Locale.ROOT,"%01X%01X%02X",(a>>>4)&3,a&15,z));}
        }
        return new ArrayList<>(codes);
    }
}
