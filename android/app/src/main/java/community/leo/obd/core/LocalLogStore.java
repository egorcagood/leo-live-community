package community.leo.obd.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One bounded private session. Export requires a separate explicit user action. */
public final class LocalLogStore {
    private static final String HEADER="timestamp_ms,source,field,value,unit,status\n";
    private static final int LIMIT=4000000;
    private final File directory;
    public LocalLogStore(File directory){this.directory=directory;}
    public synchronized void save(String csv) throws IOException {
        if(csv==null||!csv.startsWith(HEADER)||csv.length()>LIMIT)throw new IllegalArgumentException("Invalid session CSV");
        byte[] body=csv.getBytes(StandardCharsets.UTF_8);if(body.length>LIMIT)throw new IllegalArgumentException("Session CSV too large");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create log directory");
        File temp=new File(directory,"last-session.csv.tmp"),target=new File(directory,"last-session.csv");
        try(FileOutputStream stream=new FileOutputStream(temp)){stream.write(body);stream.getFD().sync();}
        Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public synchronized String read() throws IOException {
        File file=new File(directory,"last-session.csv");if(!file.exists())return null;
        if(file.length()>LIMIT)throw new IOException("Session too large");
        byte[] body=Files.readAllBytes(file.toPath());if(body.length>LIMIT)throw new IOException("Session too large");
        String result=new String(body,StandardCharsets.UTF_8);if(!result.startsWith(HEADER))throw new IOException("Invalid stored session");return result;
    }
}
