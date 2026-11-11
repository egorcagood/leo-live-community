package community.leo.obd.core;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** This is the only wire-command encoder. Never accept commands from a server or UI. */
public final class ReadPolicy {
    private static final Set<String> COMMANDS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0",
        "0100","0120","0140","0160","0104","0105","010C","010D","0111","012F","0142","03","07")));
    private ReadPolicy(){}
    public static byte[] frame(String command){
        if(command==null||!COMMANDS.contains(command))throw new IllegalArgumentException("Command is not an approved read");
        return (command+"\r").getBytes(StandardCharsets.US_ASCII);
    }
}
